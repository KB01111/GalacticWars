"""Fetch the curated character-source inventory from its pinned upstream revisions.

All responses are retained in memory until every upstream and vendored SHA-256 hash has been
verified. Blockbench host paths are removed deterministically before vendoring while geometry,
texture identifiers, and embedded texture data remain unchanged.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Callable
from urllib.parse import quote
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "tools/source_art/authorized_upstream"
INVENTORY = SOURCE_ROOT / "source_inventory.json"


def raw_url(repository: str, revision: str, source_path: str) -> str:
    encoded_path = "/".join(quote(segment, safe="") for segment in source_path.split("/"))
    return f"https://raw.githubusercontent.com/{repository}/{revision}/{encoded_path}"


def sha256(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def sanitize_blockbench_payload(payload: bytes) -> bytes:
    """Remove host-local texture paths without changing usable Blockbench texture records."""
    model = json.loads(payload.decode("utf-8"))
    for texture in model.get("textures", []):
        if not isinstance(texture, dict):
            continue
        texture.pop("path", None)
        texture.pop("relative_path", None)
    return json.dumps(model, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def vendored_payload(entry: dict, payload: bytes) -> bytes:
    sanitizer = entry.get("sanitizer")
    if sanitizer is None:
        return payload
    if sanitizer == "blockbench_texture_paths":
        return sanitize_blockbench_payload(payload)
    raise ValueError(f"Unknown source sanitizer {sanitizer!r} for {entry['source_path']}")


def download_payload(url: str) -> bytes:
    with urlopen(url, timeout=60) as response:
        return response.read()


def verified_writes(
        inventory: dict,
        source_root: Path,
        download: Callable[[str], bytes] = download_payload,
) -> list[tuple[Path, bytes]]:
    verified: list[tuple[Path, bytes]] = []
    for source in inventory["sources"]:
        for entry in source["files"]:
            url = raw_url(source["repository"], source["revision"], entry["source_path"])
            try:
                upstream = download(url)
            except Exception as error:
                raise RuntimeError(f"Failed to fetch pinned source {entry['source_path']} from {url}") from error
            actual_upstream = sha256(upstream)
            if actual_upstream != entry["upstream_sha256"]:
                raise ValueError(
                    f"Pinned source hash changed for {entry['source_path']}: "
                    f"expected {entry['upstream_sha256']}, found {actual_upstream}"
                )
            sanitized = vendored_payload(entry, upstream)
            actual_vendored = sha256(sanitized)
            if actual_vendored != entry["vendored_sha256"]:
                raise ValueError(
                    f"Vendored source hash changed for {entry['source_path']}: "
                    f"expected {entry['vendored_sha256']}, found {actual_vendored}"
                )
            verified.append((source_root / entry["destination"], sanitized))
    return verified


def fetch_sources(
        inventory_path: Path = INVENTORY,
        source_root: Path = SOURCE_ROOT,
        download: Callable[[str], bytes] = download_payload,
) -> int:
    inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
    verified = verified_writes(inventory, source_root, download)

    for destination, payload in verified:
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(payload)
    return len(verified)


def main() -> None:
    count = fetch_sources()
    print(f"Fetched and verified {count} authorized character source files")


if __name__ == "__main__":
    main()
