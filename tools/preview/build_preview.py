#!/usr/bin/env python3
"""Build the exact HTML shell the Android WebView will load.

This mirrors ReaderShellBuilder.kt so the two renderers can be compared
against the same input pages.  It intentionally keeps the page's own
<style> blocks and the Wikidot ancestor wrappers.
"""
from __future__ import annotations

import argparse
import html as html_module
import json
import re
import sys
from pathlib import Path
from urllib.parse import urljoin

from lxml import etree, html

BASE_URL = "https://scp-wiki-cn.wikidot.com/"
NOISE_SELECTORS = [
    ".footer-wikiwalk-nav",
    "#page-tags",
    ".info-container",
    ".page-options",
    ".page-options-container",
    "script",
    "noscript",
    "link[rel=stylesheet]",
]


def text_of(node) -> str:
    return re.sub(r"\s+", " ", node.text_content() or "").strip()


def is_iframe_noise(frame) -> bool:
    src = (frame.get("src") or "").lower()
    return "interwikiframe" in src or "styleframe" in src


def clean_content(doc):
    for selector in NOISE_SELECTORS:
        for node in doc.cssselect(selector):
            parent = node.getparent()
            if parent is not None:
                parent.remove(node)

    for frame in list(doc.cssselect("#page-content iframe")):
        parent = frame.getparent()
        if parent is None:
            continue
        if is_iframe_noise(frame):
            parent.remove(frame)
            continue
        replacement = html.Element("p")
        replacement.set("class", "reader-embed")
        replacement.text = "嵌入内容："
        link = etree.SubElement(replacement, "a")
        link.set("href", frame.get("src") or "about:blank")
        link.text = "打开"
        parent.replace(frame, replacement)

    for box in list(doc.cssselect("#page-content .list-pages-box")):
        if not text_of(box) and box.cssselect("iframe"):
            parent = box.getparent()
            if parent is not None:
                parent.remove(box)

    for node in doc.cssselect("#page-content *"):
        for attr in list(node.attrib.keys()):
            if attr.lower().startswith("on"):
                del node.attrib[attr]
        if node.tag == "a":
            href = node.get("href") or ""
            if href.lower().startswith("javascript:"):
                node.set("href", "#")
                node.set("data-reader-noop", "1")
            if "target" in node.attrib:
                del node.attrib["target"]
        if node.tag == "img":
            src = node.get("src") or ""
            if src:
                absolute = urljoin(BASE_URL, src)
                if absolute.startswith("http://"):
                    absolute = "https://" + absolute[len("http://"):]
                node.set("src", absolute)
            for attr in ["srcset", "sizes"]:
                if attr in node.attrib:
                    del node.attrib[attr]
            node.set("loading", "lazy")
            node.set("decoding", "async")


def collect_styles(doc):
    styles = []
    seen = set()
    for node in doc.xpath("//head//style | //*[@id='page-content']//style"):
        content = node.text_content() or ""
        content = content.strip()
        if content and content not in seen:
            seen.add(content)
            styles.append(content)
    return styles


def build(raw_html: str, css: str, js: str, slug: str):
    doc = html.document_fromstring(raw_html)
    page = doc.xpath("//*[@id='page-content']")
    if not page:
        raise ValueError("page-content not found in " + slug)
    page_content = page[0]

    title_node = doc.xpath("//*[@id='page-title']")
    title = text_of(title_node[0]) if title_node else text_of(doc.xpath("//title")[0])
    title = title or slug

    styles = collect_styles(doc)
    clean_content(doc)

    inner = page_content.text or ""
    for child in page_content:
        inner += etree.tostring(child, encoding="unicode", method="html")

    page_styles_html = "\n".join("<style>\n" + css + "\n</style>" for css in styles)
    title_html = html_module.escape(title, quote=False)

    shell = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<meta name="referrer" content="no-referrer">
<base href="BASE_HREF">
PAGE_STYLES
<style>
READER_CSS
</style>
</head>
<body id="html-body" class="reader-body">
<div id="skrollr-body">
  <div class="container-wrap-wrap">
    <div class="container-wrap">
      <div class="container">
        <div id="content-wrap">
          <div id="main-content">
            <div id="page-title">TITLE</div>
            <div id="page-content">CONTENT</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<script>
window.Reader = window.Reader || { postMessage: function (message) { try { console.log(message); } catch (e) {} } };
</script>
<script>
READER_JS
</script>
</body>
</html>
"""
    shell = shell.replace("BASE_HREF", BASE_URL)
    shell = shell.replace("PAGE_STYLES", page_styles_html)
    shell = shell.replace("READER_CSS", css)
    shell = shell.replace("TITLE", title_html)
    shell = shell.replace("CONTENT", inner)
    shell = shell.replace("READER_JS", js)

    stats = {
        "slug": slug,
        "title": title,
        "styles": len(styles),
        "images": len(page_content.cssselect("img")),
        "tables": len(page_content.cssselect("table")),
        "blockquotes": len(page_content.cssselect("blockquote")),
        "footnotes": len(page_content.cssselect(".footnote-footer")),
        "headings": len(page_content.cssselect("h1, h2, h3")),
        "tabsets": len(page_content.cssselect(".yui-navset")),
        "collapsibles": len(page_content.cssselect(".collapsible-block")),
        "onclick_left": len(page_content.xpath(".//*[@onclick]")),
        "javascript_links_left": len(page_content.xpath(".//a[starts-with(@href, 'javascript:')]")),
    }
    return shell, stats


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--slug", action="append", default=[])
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    pages_dir = root / "tools" / "preview" / "pages"
    out_dir = root / "preview"
    out_dir.mkdir(parents=True, exist_ok=True)

    css = (root / "tools" / "preview" / "reader.css").read_text(encoding="utf-8")
    js = (root / "tools" / "preview" / "reader.js").read_text(encoding="utf-8") + "\n" + (root / "tools" / "preview" / "validate.js").read_text(encoding="utf-8")

    slugs = args.slug or ["scp-173", "scp-5000", "scp-cn-1000", "scp-3029"]
    all_stats = []
    for slug in slugs:
        raw = (pages_dir / (slug + ".html")).read_text(encoding="utf-8")
        shell, stats = build(raw, css, js, slug)
        (out_dir / (slug + ".html")).write_text(shell, encoding="utf-8")
        all_stats.append(stats)
        print(json.dumps(stats, ensure_ascii=False))

    index = ["<!DOCTYPE html>", "<meta charset='utf-8'>", "<title>ScpReader preview</title>",
             "<style>body{font-family:system-ui,sans-serif;padding:24px;line-height:1.6}td,th{padding:4px 10px;border-bottom:1px solid #ddd;text-align:left}</style>",
             "<h1>ScpReader preview</h1>", "<table><tr><th>page</th><th>styles</th><th>images</th><th>tables</th><th>quotes</th><th>footnotes</th></tr>"]
    for s in all_stats:
        index.append("<tr><td><a href='" + s["slug"] + ".html'>" + s["slug"] + "</a></td><td>" + str(s["styles"]) + "</td><td>" + str(s["images"]) + "</td><td>" + str(s["tables"]) + "</td><td>" + str(s["blockquotes"]) + "</td><td>" + str(s["footnotes"]) + "</td></tr>")
    index.append("</table>")
    (out_dir / "index.html").write_text("\n".join(index), encoding="utf-8")
    (out_dir / "stats.json").write_text(json.dumps(all_stats, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
