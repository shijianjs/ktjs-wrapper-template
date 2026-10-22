#!/usr/bin/env python3
"""Turn this template into a wrapper project.

Replaces the placeholders that ship with the template, copies `scaffold/module` into the first
wrapper module and writes the project READMEs. Everything is standard library only, so it runs on
any Python 3 without extra packages.

The script refuses to run twice: it deletes itself and `scaffold/project` when it is done.
`scaffold/module` is kept (still containing placeholders) so new modules can be copied from it.
"""

from __future__ import annotations

import argparse
import datetime
import os
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = Path(__file__).resolve()

# Directories that never contain project files, plus `scaffold`: its placeholders are replaced per
# module when a module is created, not during the global pass.
SKIP_DIRS = {".git", "build", ".gradle", ".kotlin", "node_modules", "kotlin-js-store", "scaffold"}
# Binary files copied from elsewhere; reading them as UTF-8 would fail anyway, this keeps it quiet.
SKIP_SUFFIXES = {".jar", ".png", ".jpg", ".ico", ".zip", ".gz", ".xz"}

PREFIX = re.compile(r"^[a-z][a-z0-9-]*$")
OWNER = re.compile(r"^[A-Za-z0-9][A-Za-z0-9-]*$")
PACKAGE = re.compile(r"^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--prefix", required=True, help="Gradle module prefix, e.g. ktjs-vaadin")
    parser.add_argument("--owner", required=True, help="GitHub user or organization")
    parser.add_argument("--repo", help="GitHub repository name; defaults to the prefix")
    parser.add_argument("--package", required=True, help="Kotlin base package, e.g. vaadin")
    parser.add_argument("--module", default="core", help="suffix of the first module; default: core")
    parser.add_argument("--npm-package", required=True, help="wrapped npm package, e.g. @vaadin/grid")
    parser.add_argument("--npm-version", required=True, help="upstream npm version")
    parser.add_argument("--project", help="display name; defaults to --repo")
    args = parser.parse_args(argv)

    for label, value, pattern in (
        ("--prefix", args.prefix, PREFIX),
        ("--owner", args.owner, OWNER),
        ("--module", args.module, PREFIX),
        ("--package", args.package, PACKAGE),
    ):
        if not pattern.match(value):
            parser.error(f"{label} {value!r} is not valid; expected {pattern.pattern}")

    args.repo = args.repo or args.prefix
    if not OWNER.match(args.repo):
        parser.error(f"--repo {args.repo!r} is not a valid GitHub repository name")
    args.project = args.project or args.repo
    if not args.npm_package.strip() or any(character.isspace() for character in args.npm_package):
        parser.error("--npm-package must be a single npm package name")
    if not args.npm_version.strip():
        parser.error("--npm-version must not be empty")
    return args


def replacements(args: argparse.Namespace, year: str) -> dict[str, str]:
    return {
        "__PROJECT__": args.project,
        "__PREFIX__": args.prefix,
        "__OWNER__": args.owner,
        "__REPO__": args.repo,
        "__PACKAGE__": args.package,
        "__MODULE__": args.module,
        "__NPM_PACKAGE__": args.npm_package,
        "__NPM_VERSION__": args.npm_version,
        "__YEAR__": year,
    }


def replace_in_text(path: Path, values: dict[str, str]) -> bool:
    try:
        original = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return False
    updated = original
    for placeholder, value in values.items():
        updated = updated.replace(placeholder, value)
    if updated == original:
        return False
    path.write_text(updated, encoding="utf-8", newline="\n")
    return True


def replace_tree(root: Path, values: dict[str, str], *, skip_scaffold: bool) -> list[Path]:
    changed = []
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root)
        if relative.parts and relative.parts[0] in SKIP_DIRS and not (
            skip_scaffold and relative.parts[0] == "scaffold"
        ):
            continue
        if skip_scaffold and relative.parts and relative.parts[0] == "scaffold":
            continue
        if path == SCRIPT or path.suffix in SKIP_SUFFIXES:
            continue
        if replace_in_text(path, values):
            changed.append(relative)
    return changed


def relocate_package(module_root: Path, package: str) -> None:
    for source_set in ("jsMain", "jsTest"):
        kotlin = module_root / "src" / source_set / "kotlin"
        old = kotlin / "__PACKAGE__"
        if not old.is_dir():
            continue
        target = kotlin.joinpath(*package.split("."))
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            raise SystemExit(f"error: {target} already exists; cannot move {old}")
        os.replace(old, target)


def wire(path: Path, marker: str, comment: str, line: str) -> None:
    """Replace the template marker with a plain comment and the generated wiring line."""
    text = path.read_text(encoding="utf-8")
    if marker not in text:
        raise SystemExit(f"error: {path}: marker not found: {marker}")
    path.write_text(text.replace(marker, f"{comment}\n{line}", 1), encoding="utf-8", newline="\n")


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    year = str(datetime.date.today().year)
    project_readme = ROOT / "scaffold" / "project" / "README.md"
    if not project_readme.is_file() or not SCRIPT.is_file():
        raise SystemExit("error: this project was already initialized from the template")

    module_project = f"{args.prefix}-{args.module}"
    module_root = ROOT / module_project
    if module_root.exists():
        raise SystemExit(f"error: {module_project} already exists")

    values = replacements(args, year)

    # 1. Replace placeholders in every project file. `scaffold/` keeps its placeholders because it
    #    is the reusable module skeleton; the generated READMEs are handled separately below.
    changed = replace_tree(ROOT, values, skip_scaffold=True)

    # 2. Create the first module from the scaffold and replace its placeholders.
    source = ROOT / "scaffold" / "module"
    if not source.is_dir():
        raise SystemExit(f"error: missing module scaffold at {source}")
    shutil.copytree(source, module_root)
    replace_tree(module_root, values, skip_scaffold=False)
    relocate_package(module_root, args.package)

    # 3. Wire the module into the build, the version table, the demo and the npm manifest.
    wire(
        ROOT / "settings.gradle.kts",
        "// scripts/init-wrapper-project.py appends one include per wrapper module below. The template ships\n"
        "// none on purpose: replace the placeholders and run the script before the first build.",
        "// One Gradle project per published wrapper module.",
        f'include(":{module_project}")',
    )
    wire(
        ROOT / "demo" / "build.gradle.kts",
        "            // scripts/init-wrapper-project.py appends the wrapper modules this demo uses below.",
        "            // Wrapper modules used by the demo.",
        f'            implementation(project(":{module_project}"))',
    )

    # 4. Swap the template README for the generated project README, then drop template-only files.
    scaffold_project = ROOT / "scaffold" / "project"
    replace_tree(scaffold_project, values, skip_scaffold=True)
    for name in ("README.md", "README.zh-CN.md"):
        os.replace(scaffold_project / name, ROOT / name)
    scaffold_project.rmdir()
    SCRIPT.unlink()

    print(f"initialized {args.project} ({args.owner}/{args.repo})")
    print(f"  module: {module_project}  package: {args.package}")
    print(f"  upstream: {args.npm_package}@{args.npm_version} -> {args.npm_version}-kt.1")
    print(f"  files changed: {len(changed)}")
    print()
    print("next steps:")
    print("  ./gradlew kotlinNpmInstall")
    print("  ./gradlew kotlinUpgradePackageLock")
    print("  ./gradlew build")
    print("  ./gradlew pagesSite")
    print("  ./gradlew :demo:e2eTest")
    print()
    print("then rewrite the demo, the module READMEs and the KDoc, and commit.")
    print("`scaffold/module` keeps its placeholders; copy it when adding another module (see AGENTS.md).")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except KeyboardInterrupt:
        sys.exit(130)
