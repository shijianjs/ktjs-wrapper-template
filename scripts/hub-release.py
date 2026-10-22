#!/usr/bin/env python3
"""Package published coordinates into hub archives and build their registrations.

The Maven Central hub (`__OWNER__/maven-central-publishing`) publishes nothing itself: a release
here hands it one archive per Maven coordinate plus a registration that names it. This script turns
the file repository produced by `publishAllPublicationsToHubStagingRepository` into exactly that,
and it checks the same rules the hub applies so a mistake is caught here rather than in someone
else's workflow run.

A Kotlin Multiplatform module is not one coordinate: the `kotlinMultiplatform` publication is
`<module>` and the JS publication is `<module>-js`, so each module here yields two archives and two
registrations.

Only the Python standard library is used, so this runs on any `ubuntu-latest` CI image and locally.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import lzma
import sys
import tarfile
import urllib.parse
from dataclasses import dataclass, field
from pathlib import Path

GROUP_PATH = "io/github/__OWNER__"
# Gradle writes these next to every artifact. Maven Central does not need them and the hub drops
# them because they only consume the monthly File Count quota.
CHECKSUM_SUFFIXES = (".md5", ".sha1", ".sha256", ".sha512")
# Entries are stored with a fixed timestamp so the same input always produces the same archive and
# therefore the same sha256 in the registration.
FIXED_MTIME = 315532800  # 1980-01-01, the lowest tar timestamp that stays portable


class ReleaseError(RuntimeError):
    pass


@dataclass
class Coordinate:
    module: str
    version: str
    artifact: str
    directory: Path
    # File names inside the coordinate directory, which Maven publishes flat.
    files: list[str] = field(default_factory=list)

    @property
    def coordinate(self) -> str:
        return f"io.github.__OWNER__:{self.artifact}:{self.version}"

    @property
    def archive_name(self) -> str:
        return f"{self.artifact}-{self.version}.tar.xz"

    @property
    def repository_path(self) -> str:
        return f"{GROUP_PATH}/{self.artifact}/{self.version}"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def central_files(directory: Path) -> list[str]:
    """The files Maven Central needs: artifacts and their `.asc` signatures, nothing else.

    Gradle also writes `.md5`/`.sha1`/`.sha256`/`.sha512` next to everything, including next to the
    signatures; Central ignores those and the hub counts them against the monthly File Quota.
    """
    names = []
    for path in sorted(directory.iterdir()):
        if not path.is_file():
            continue
        name = path.name
        if name.startswith(".") or name == "maven-metadata.xml":
            continue
        if any(name.endswith(suffix) for suffix in CHECKSUM_SUFFIXES):
            continue
        names.append(name)
    return names


def discover(staging: Path, module: str, version: str) -> list[Coordinate]:
    root = staging / module / "build" / "hub-staging" / GROUP_PATH
    if not root.is_dir():
        raise ReleaseError(
            f"{root}: missing; run ./gradlew :{module}:publishAllPublicationsToHubStagingRepository first"
        )

    coordinates = []
    for artifact_dir in sorted(p for p in root.iterdir() if p.is_dir()):
        version_dir = artifact_dir / version
        if not version_dir.is_dir():
            continue
        # The module's own coordinate is `<module>`; KMP target coordinates are `<module>-<target>`.
        if not artifact_dir.name.startswith(module):
            raise ReleaseError(f"{version_dir}: unexpected artifact id {artifact_dir.name!r}")
        coordinate = Coordinate(
            module=module,
            version=version,
            artifact=artifact_dir.name,
            directory=version_dir,
        )
        coordinate.files = central_files(version_dir)
        if not coordinate.files:
            raise ReleaseError(f"{version_dir}: no artifacts after filtering checksum files")
        coordinates.append(coordinate)

    if not coordinates:
        raise ReleaseError(f"{root}: no {module} coordinate found for version {version}")
    return coordinates


def validate(coordinate: Coordinate) -> None:
    """Assert what scripts/stage.py asserts, so the hub cannot reject the archive later."""
    names = set(coordinate.files)
    pom = f"{coordinate.artifact}-{coordinate.version}.pom"
    if pom not in names:
        raise ReleaseError(f"{coordinate.directory}: missing {pom}")

    for name in coordinate.files:
        size = (coordinate.directory / name).stat().st_size
        if name.endswith(".asc"):
            if size == 0:
                raise ReleaseError(f"{coordinate.directory}: empty signature {name}")
            continue
        if any(name.endswith(suffix) for suffix in CHECKSUM_SUFFIXES):
            raise ReleaseError(f"{coordinate.directory}: {name} is a checksum file")
        if f"{name}.asc" not in names:
            raise ReleaseError(f"{coordinate.directory}: {name} has no .asc signature")


def write_archive(coordinate: Coordinate, archives: Path) -> tuple[Path, str, int]:
    path = archives / coordinate.archive_name
    plain = io.BytesIO()
    with tarfile.open(fileobj=plain, mode="w", format=tarfile.PAX_FORMAT) as tar:
        for name in coordinate.files:
            source = coordinate.directory / name
            info = tarfile.TarInfo(name=f"{coordinate.repository_path}/{name}")
            info.size = source.stat().st_size
            info.mtime = FIXED_MTIME
            info.mode = 0o644
            info.uid = info.gid = 0
            info.uname = info.gname = ""
            with source.open("rb") as stream:
                tar.addfile(info, stream)

    compressed = lzma.compress(plain.getvalue(), preset=6)
    path.write_bytes(compressed)
    return path, hashlib.sha256(compressed).hexdigest(), len(compressed)


def registration(coordinate: Coordinate, project: str, release: str, url: str, sha256: str) -> dict:
    return {
        "schemaVersion": 1,
        "project": project,
        "module": coordinate.artifact,
        "version": coordinate.version,
        "release": release,
        "coordinate": coordinate.coordinate,
        "archive": {"url": url, "sha256": sha256},
    }


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--project", required=True, help="GitHub repository, owner/repo")
    parser.add_argument("--release", required=True, help="release tag the archives are uploaded to")
    parser.add_argument(
        "--module",
        action="append",
        default=[],
        metavar="NAME=VERSION",
        help="publishable module and its version; repeat for every module in this release",
    )
    parser.add_argument(
        "--modules-file",
        type=Path,
        help="file with one NAME=VERSION per line; blank lines and #-comments are ignored",
    )
    parser.add_argument(
        "--repository",
        default=".",
        help="directory holding <module>/build/hub-staging (the repository root)",
    )
    parser.add_argument("--archives", required=True, help="where to write the .tar.xz files")
    parser.add_argument("--registrations", required=True, help="where to write registry/<owner>/... JSON")
    parser.add_argument(
        "--base-url",
        help="override the asset URL prefix; defaults to this project's GitHub release download URL",
    )
    args = parser.parse_args(argv)

    if "/" not in args.project or args.project.count("/") != 1:
        raise ReleaseError(f"--project must look like owner/repo, got {args.project!r}")
    repository = Path(args.repository).resolve()
    archives = Path(args.archives)
    registrations = Path(args.registrations)
    archives.mkdir(parents=True, exist_ok=True)
    base = args.base_url or (
        f"https://github.com/{args.project}/releases/download/{urllib.parse.quote(args.release)}"
    )

    specs = list(args.module)
    if args.modules_file is not None:
        try:
            # utf-8-sig tolerates the BOM PowerShell writes by default.
            lines = args.modules_file.read_text(encoding="utf-8-sig").splitlines()
        except OSError as error:
            raise ReleaseError(f"{args.modules_file}: cannot read module list: {error}") from error
        specs.extend(
            line.strip()
            for line in lines
            if line.strip() and not line.lstrip().startswith("#")
        )
    if not specs:
        raise ReleaseError("no modules selected; pass --module or --modules-file")

    rows = []
    for spec in specs:
        module, _, version = spec.partition("=")
        if not version:
            raise ReleaseError(f"--module {spec!r} must be NAME=VERSION")
        for coordinate in discover(repository, module, version):
            validate(coordinate)
            path, sha256, size = write_archive(coordinate, archives)
            payload = registration(
                coordinate,
                args.project,
                args.release,
                f"{base}/{coordinate.archive_name}",
                sha256,
            )
            target = registrations / "registry" / args.project / f"{coordinate.artifact}.json"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            rows.append((coordinate, path, sha256, size))

    if not rows:
        raise ReleaseError("nothing was packaged; check --module values")

    manifest = {
        "project": args.project,
        "release": args.release,
        "archives": [
            {
                "module": coordinate.module,
                "artifact": coordinate.artifact,
                "coordinate": coordinate.coordinate,
                "version": coordinate.version,
                "archive": path.name,
                "sha256": sha256,
                "bytes": size,
                "files": len(coordinate.files),
                "registration": f"registry/{args.project}/{coordinate.artifact}.json",
            }
            for coordinate, path, sha256, size in rows
        ],
    }
    (registrations / "hub-manifest.json").write_text(
        json.dumps(manifest, indent=2) + "\n", encoding="utf-8"
    )

    for coordinate, path, sha256, size in rows:
        print(
            f"{coordinate.coordinate}: {path.name} ({len(coordinate.files)} files, {size} bytes)"
            f" sha256={sha256[:12]}…"
        )
    print(f"archives: {archives}")
    print(f"registrations: {registrations / 'registry' / args.project}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except ReleaseError as error:
        print(f"error: {error}", file=sys.stderr)
        sys.exit(1)
