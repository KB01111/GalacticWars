"""Fetch the curated character-source inventory from its pinned upstream revisions.

All responses are retained in memory until every SHA-256 hash has been verified. This keeps a
missing or changed upstream input from partially rewriting the immutable source directory.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from urllib.parse import quote
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "tools/source_art/authorized_upstream"
INVENTORY = SOURCE_ROOT / "source_inventory.json"


def raw_url(repository: str, revision: str, source_path: str) -> str:
    encoded_path = "/".join(quote(segment, safe="") for segment in source_path.split("/"))
    return f"https://raw.githubusercontent.com/{repository}/{revision}/{encoded_path}"


def main() -> None:
    inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
    verified: list[tuple[Path, bytes]] = []
    for source in inventory["sources"]:
        for entry in source["files"]:
            url = raw_url(source["repository"], source["revision"], entry["source_path"])
            with urlopen(url, timeout=60) as response:
                payload = response.read()
            actual = hashlib.sha256(payload).hexdigest()
            if actual != entry["sha256"]:
                raise ValueError(
                    f"Pinned source hash changed for {entry['source_path']}: "
                    f"expected {entry['sha256']}, found {actual}"
                )
            verified.append((SOURCE_ROOT / entry["destination"], payload))

    for destination, payload in verified:
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(payload)
    print(f"Fetched and verified {len(verified)} authorized character source files")


if __name__ == "__main__":
    main()
