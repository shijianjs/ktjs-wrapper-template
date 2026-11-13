#!/usr/bin/env python3
"""Minimal streamable-HTTP MCP client.

Works with both the JetBrains built-in MCP server and the Index MCP plugin: they share the
streamable-HTTP transport, only the URL (and optionally headers) differ.

Usage:
  python mcp_client.py list
  python mcp_client.py schema ide_type_hierarchy
  python mcp_client.py call ide_find_symbol '{"query":"Foo","language":"Kotlin"}'

Connection (all optional):
  --url URL           default: $IDE_MCP_URL, else http://127.0.0.1:29170/index-mcp/streamable-http
  --header KEY=VALUE  repeatable; e.g. IJ_MCP_SERVER_PROJECT_PATH=S:/repo (built-in server only)
  --project PATH      injected as project_path into every tools/call (multi-project workspaces)
"""

import argparse
import json
import os
import sys
import urllib.request

DEFAULT_URL = os.environ.get("IDE_MCP_URL", "http://127.0.0.1:29170/index-mcp/streamable-http")
DEFAULT_TIMEOUT = int(os.environ.get("IDE_MCP_TIMEOUT", "120"))


class Client:
    def __init__(self, url=None, headers=None, project=None, timeout=None):
        self.url = url or DEFAULT_URL
        self.timeout = timeout or DEFAULT_TIMEOUT
        self.project = project
        self.headers = {
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
        }
        self.headers.update(headers or {})
        self.session = None
        self._initialize()

    def _post(self, payload):
        headers = dict(self.headers)
        if self.session:
            headers["Mcp-Session-Id"] = self.session
        request = urllib.request.Request(
            self.url, data=json.dumps(payload).encode(), headers=headers, method="POST"
        )
        with urllib.request.urlopen(request, timeout=self.timeout) as response:
            self.session = response.headers.get("Mcp-Session-Id") or self.session
            return response.read().decode("utf-8", "replace")

    @staticmethod
    def _parse(raw):
        data_lines = [line[5:].strip() for line in raw.splitlines() if line.startswith("data:")]
        text = data_lines[-1] if data_lines else raw
        return json.loads(text) if text.strip() else {}

    def _initialize(self):
        self._post({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "mcp-client", "version": "1.0"},
            },
        })
        try:
            self._post({"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})
        except Exception:
            pass

    def tools(self):
        return self._parse(
            self._post({"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}})
        ).get("result", {}).get("tools", [])

    def call(self, name, arguments=None):
        arguments = dict(arguments or {})
        if self.project:
            arguments.setdefault("project_path", self.project)
        parsed = self._parse(
            self._post({
                "jsonrpc": "2.0",
                "id": 3,
                "method": "tools/call",
                "params": {"name": name, "arguments": arguments},
            })
        )
        if parsed.get("error"):
            # JSON-RPC level failure (e.g. -32603 from a broken IDE index); keep the message visible
            # instead of returning an empty dict that looks like "no results".
            raise RuntimeError(f"{name}: {parsed['error'].get('message')}")
        result = parsed.get("result", {})
        text = "\n".join(item.get("text", "") for item in result.get("content", []))
        if result.get("isError"):
            raise RuntimeError(f"{name}: {text[:500]}")
        return json.loads(text) if text.strip() else {}


def parse_headers(values):
    headers = {}
    for value in values or []:
        key, _, val = value.partition("=")
        if key:
            headers[key] = val
    return headers


def main():
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--url", default=None)
    common.add_argument("--header", action="append", default=[])
    common.add_argument("--project", default=os.environ.get("IDE_MCP_PROJECT"))

    parser = argparse.ArgumentParser(description="streamable-HTTP MCP client", parents=[common])
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list", parents=[common])
    schema = sub.add_parser("schema", parents=[common])
    schema.add_argument("names", nargs="+")
    call = sub.add_parser("call", parents=[common])
    call.add_argument("name")
    call.add_argument("arguments", nargs="?", default="{}")

    args = parser.parse_args()
    client = Client(args.url, parse_headers(args.header), args.project)

    if args.command == "list":
        for tool in client.tools():
            print(tool["name"])
    elif args.command == "schema":
        wanted = set(args.names)
        for tool in client.tools():
            if tool["name"] in wanted:
                print(tool["name"], json.dumps(tool.get("inputSchema", {}), indent=2))
                print("---")
    else:
        print(json.dumps(client.call(args.name, json.loads(args.arguments)), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
