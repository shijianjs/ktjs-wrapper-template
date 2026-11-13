#!/usr/bin/env python3
"""Type hierarchy (supertypes + subtypes) from the IDE index, rendered as a tree.

Since Index MCP plugin 5.20.4 `ide_type_hierarchy` resolves Kotlin/JS (klib) as well as Java and
Kotlin/JVM (github.com/hechtcarmel/jetbrains-index-mcp-plugin/issues/456), so this script no longer
reconstructs anything: it calls that tool and only adds what the tool does not do.

  * per-direction depth caps (`--up` / `--down`); the tool returns the whole BFS closure
  * tree rendering with parent/child edges instead of the flat node list
  * compact `artifact:version/file` paths instead of full gradle cache paths
  * page merging (`maxNodes` is capped at 500 per call)

Usage:
  python hierarchy.py <file> <line> <column> [--project PATH] [--up N] [--down N]
  python hierarchy.py --class io.github.shijianjs.ktjs.dom.CustomElementAdapter --project PATH
  python hierarchy.py --symbol sym_xxx --project PATH
"""

import argparse
import os
import re
import sys
from collections import defaultdict

from mcp_client import Client

MAX_NODES = 500
SCOPES = ("project_and_libraries", "project_files", "project_production_files", "project_test_files")

# .../files-2.1/<group>/<artifact>/<version>/<sha1>/<file>  ->  artifact:version/file
LIB_PATH = re.compile(r".*files-2\.1/[^/]+/([^/]+)/([^/]+)/[0-9a-f]+/(.+)$")


def shorten(path):
    if not path:
        return "<unknown>"
    archive, sep, inner = path.partition("!/")
    match = LIB_PATH.match(archive)
    if match:
        archive = f"{match.group(1)}:{match.group(2)}/{match.group(3)}"
    return f"{archive}{sep}{inner}"


def fetch(client, selector, scope):
    """Call ide_type_hierarchy once and follow cursors until the traversal is complete.

    The selector goes at the top level (`className` / `symbolId` / `file`+`line`+`column`); nesting it
    under `target` with `className` is rejected as "target must select exactly one variant".
    """
    pages = []
    cursor = None
    while True:
        args = {"maxNodes": MAX_NODES}
        args.update({"cursor": cursor} if cursor else {**selector, "scope": scope})
        page = client.call("ide_type_hierarchy", args)
        if "element" not in page:
            raise RuntimeError(f"no hierarchy in the response: {page}")
        pages.append(page)
        if not page.get("hasMore"):
            return pages
        cursor = page.get("cursor")
        if not cursor:
            raise RuntimeError(f"hierarchy truncated without a cursor: {page.get('truncationReason')}")


def node_key(node):
    return (node.get("name"), node.get("file") or "")


def merge(pages):
    """Join the pages into one tree: {parent key: [child keys]} plus the node records."""
    root = pages[0]["element"]
    root_key = ("root", node_key(root))
    children = {"up": defaultdict(list), "down": defaultdict(list)}
    nodes = {"up": {}, "down": {}}

    for page in pages:
        ids = {n["nodeId"]: n for n in (page["element"], *(page.get("supertypes") or []), *(page.get("subtypes") or []))}
        buckets = (("up", page.get("supertypes") or []), ("down", page.get("subtypes") or []))
        for direction, bucket in buckets:
            for node in bucket:
                key = (direction, node_key(node))
                nodes[direction][key] = node
                parent = ids.get(node.get("parentId"))
                if parent is None or parent["nodeId"] == page["element"]["nodeId"]:
                    parent_key = root_key
                else:
                    parent_key = (direction, node_key(parent))
                if key not in children[direction][parent_key]:
                    children[direction][parent_key].append(key)

    return root, root_key, children, nodes


def walk(key, direction, children, nodes, cap, depth=0, visited=None, lines=None):
    lines = [] if lines is None else lines
    visited = set() if visited is None else visited
    if depth > cap or key in visited:
        return lines
    visited.add(key)
    node = nodes[direction].get(key)
    if node:
        lines.append((depth, f"{node['name']}  @ {shorten(node['file'])}"))
    for child in children[direction].get(key, ()):
        walk(child, direction, children, nodes, cap, depth + 1, visited, lines)
    return lines


def print_section(title, lines):
    print(f"\n{title}:")
    if not lines:
        print("  (none)")
        return
    for depth, text in lines:
        print(f"{'  ' * depth}{text}")


def main():
    parser = argparse.ArgumentParser(description="Type hierarchy via the IDE index (Index MCP plugin)")
    parser.add_argument("file", nargs="?", help="project-relative or absolute file of the type")
    parser.add_argument("line", nargs="?", type=int, help="1-based line of the type declaration")
    parser.add_argument("column", nargs="?", type=int, help="1-based column on the type name")
    parser.add_argument("--class", dest="class_name", help="fully qualified type name")
    parser.add_argument("--symbol", help="symbolId returned by a previous call")
    parser.add_argument("--project", default=os.environ.get("IDE_MCP_PROJECT"))
    parser.add_argument("--url", default=None)
    parser.add_argument("--scope", default="project_and_libraries", choices=SCOPES)
    parser.add_argument("--up", type=int, default=8, help="supertype depth cap (0 hides the section)")
    parser.add_argument("--down", type=int, default=8, help="subtype depth cap (0 hides the section)")
    args = parser.parse_args()

    if args.class_name:
        selector = {"className": args.class_name}
    elif args.symbol:
        selector = {"symbolId": args.symbol}
    elif args.file and args.line and args.column:
        selector = {"file": args.file, "line": args.line, "column": args.column}
    else:
        parser.error("give `file line column`, or --class, or --symbol")

    client = Client(args.url, project=args.project)
    pages = fetch(client, selector, args.scope)
    root, root_key, children, nodes = merge(pages)

    print(f"{root['name']}  ({shorten(root['file'])})  [{root['language']} {root['kind']}]")
    if args.up:
        print_section("UP (supertypes)", walk(root_key, "up", children, nodes, args.up))
    if args.down:
        print_section("DOWN (subtypes)", walk(root_key, "down", children, nodes, args.down))
    if len(pages) > 1:
        print(f"\nnote: merged {len(pages)} pages of {MAX_NODES} nodes", file=sys.stderr)


if __name__ == "__main__":
    main()
