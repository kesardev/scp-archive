#!/usr/bin/env python3
"""Assert the measurable facts from the Chrome validation dumps."""
from __future__ import annotations

import json
import sys
from pathlib import Path

from lxml import html

ROOT = Path(__file__).resolve().parents[2]
PREVIEW = ROOT / "preview"


def read_validation(name: str) -> dict:
    path = PREVIEW / ("dom-" + name + ".html")
    if not path.exists():
        raise SystemExit("missing " + str(path) + "; run the Chrome dump step first")
    doc = html.parse(str(path)).getroot()
    nodes = doc.xpath("//*[@id='reader-validation']")
    if not nodes:
        raise SystemExit("validation block missing in " + str(path))
    return json.loads(nodes[0].text_content())


def check(name: str, data: dict, check_images: bool = True, **expected):
    failures = []
    table_count = expected.pop("tables", None)
    if table_count is not None and len(data.get("tables", [])) != table_count:
        failures.append("tables: expected " + repr(table_count) + ", got " + repr(len(data.get("tables", []))))
    for key, value in expected.items():
        actual = data.get(key)
        if actual != value:
            failures.append(str(key) + ": expected " + repr(value) + ", got " + repr(actual))
    images = data.get("images", [])
    if check_images:
        for image in images:
            if not image.get("complete") or image.get("naturalWidth", 0) <= 0:
                failures.append("image not loaded: " + str(image.get("src")))
    if data.get("horizontalOverflow", 0) > 0:
        failures.append("horizontal overflow: " + str(data.get("horizontalOverflow")))
    if failures:
        print("FAIL " + name)
        for failure in failures:
            print("  - " + failure)
        return False
    print("PASS " + name + " (images=" + str(len(images)) + ", text=" + str(data.get("textLength")) + ")")
    return True


def main() -> int:
    ok = True
    scp173 = read_validation("173")
    ok &= check("scp-173", scp173, tables=0, blockquotes=0, footnotes=0, headings=0, tabSets=0)
    if scp173.get("imageBlock", {}).get("float") != "none":
        print("FAIL scp-173 image block should not float")
        ok = False
    if not scp173.get("lightbox", {}).get("visible"):
        print("FAIL scp-173 lightbox did not open")
        ok = False

    scp5000 = read_validation("5000")
    ok &= check("scp-5000", scp5000, tables=6, blockquotes=25, footnotes=5, headings=1, tabSets=0)
    if not scp5000.get("footnotePopover", {}).get("visible"):
        print("FAIL scp-5000 footnote popover did not open")
        ok = False
    if not scp5000.get("lightbox", {}).get("visible"):
        print("FAIL scp-5000 lightbox did not open")
        ok = False

    cn1000 = read_validation("cn1000")
    ok &= check("scp-cn-1000", cn1000, check_images=False, tables=0, blockquotes=1, footnotes=0, headings=3, tabSets=1)
    if "评分" not in cn1000.get("ratingText", ""):
        print("FAIL scp-cn-1000 rating module missing")
        ok = False
    if not cn1000.get("permissionText"):
        print("FAIL scp-cn-1000 permission prose missing")
        ok = False
    if len(cn1000.get("tabTitles", [])) != 3:
        print("FAIL scp-cn-1000 expected 3 tabs")
        ok = False
    if cn1000.get("tabSwitch", {}).get("visibleId") != "wiki-tab-0-1":
        print("FAIL scp-cn-1000 second tab did not become visible")
        ok = False

    scp3029 = read_validation("3029")
    ok &= check("scp-3029", scp3029, check_images=False, tables=1, blockquotes=5, footnotes=3, headings=0, tabSets=0, collapsibles=1)
    if scp3029.get("collapsibleBefore") != "none":
        print("FAIL scp-3029 collapsible should start closed")
        ok = False
    if scp3029.get("collapsibleAfterOpen") != "block":
        print("FAIL scp-3029 collapsible did not open")
        ok = False
    if scp3029.get("collapsibleAfterClose") != "none":
        print("FAIL scp-3029 collapsible did not close")
        ok = False
    if "项目编号" not in scp3029.get("collapsibleContent", ""):
        print("FAIL scp-3029 collapsible content missing")
        ok = False

    print("RESULT " + ("PASS" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
