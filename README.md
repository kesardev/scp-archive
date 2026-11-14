# SCP 档案库

一个面向 SCP 基金会中文分部的 Android 阅读器。阅读器保留 Wikidot 原始
HTML/CSS 与页面脚本行为所需的结构，并提供首页、目录、书架、收藏、历史和
深色模式。

- Package: cc.kesardev.scparchive
- Version: 2.3-interactive (versionCode 5)
- Android: minSdk 24, targetSdk 35
- Repository: https://github.com/kesardev/scp-archive

Status: Phase 1 home page, Phase 2 bottom tabs/theme switch and Phase 3
interactive-entry handling implemented, compiled with Gradle and unit-tested.

The application id changed from cc.zsakvo.scpreader to
cc.kesardev.scparchive. Android treats this as a new app, so data from an older
package is not migrated automatically.

## Implemented

- Android reader shell: MaterialToolbar menu for page selection, TOC, reading
  settings, in-page find and reload.
- ReaderShellBuilder: keeps #page-content and each page <style> block, strips
  scripts/chrome, upgrades images to https and emits the exact WebView document.
- Reading settings: font size 14-24 px, line height 1.3-2.3, margin 8-32 dp and
  image on/off; persisted in SharedPreferences and applied through CSS variables.
- TOC: reader JS extracts h1-h3; the native dialog jumps to the chosen heading.
- Reading position: scroll ratio saved per page and restored after load.
- Cache: raw HTML cache with ETag / Last-Modified conditional requests, a
  six-hour freshness window, stale-cache and bundled-fixture fallback.
- Image gallery: previous/next navigation and a counter across article images.
- Series catalog: 10 SCP + 5 SCP-CN series, parsed from entry links inside the
  series panel, cached for 24 hours; tapping an entry opens it in the reader.
- Offline catalog index: entries are stored in SQLite; search matches id or
  title, and a "同步全部" action fetches all 15 series and indexes them.
- Favorites and reading history: star toggle in the reader, a two-tab library
  screen, per-entry removal, and clear-history action. History is recorded when
  a page opens and updated with reading progress.
- Request rate limiter: article and catalog requests are serialized with at
  least 1.5 s between them.
- Edge-to-edge safe areas: the root view applies status bar, navigation bar and
  display-cutout insets; the footnote popover stays above both 3-button and
  gesture navigation bars.
- Footnote popover, tabs, collapsibles, table wrapping, internal/external link
  split, progress bar and JS bridge.
- Bundled fixtures: scp-173, scp-5000, scp-cn-1000 and scp-3029.

## Home page (Phase 1)

The launcher is now HomeActivity; MainActivity remains the reader. Phase 1
started without bottom navigation; Phase 2 adds the shared tab bar while
catalog, search and library remain separate screens.

- Daily recommendation: a 12-entry curated rotation keyed by the local day, so it
  is stable for the day and works before any network request. "换一篇" cycles
  within the session.
- Continue reading: unfinished history entries (progress below 98%) from the
  library database, newest first, with a progress bar.
- Favorites: the most recent six favorites, shown when present.
- Quick actions: random catalog entry, open by number, series catalog and
  library. Number input accepts 173, CN-1000, full-width CN－1000 and full wiki
  URLs.
- Top rated: the wiki SCP-only ranking endpoint, parsed from the content-scp
  table and cached for 12 hours.
- Recent updates: the site-changes RSS feed, filtered to numbered SCP pages,
  deduplicated and cached for 30 minutes.
- Series entry: all 15 series as chips; a chip opens the catalog already
  positioned on that series.
- Footer stats: local index size, favorites and history counts.
- Startup is local-first: the curated card, history, favorites, quick actions,
  series chips and stats render from SQLite and preferences before any network
  call. A failed feed only hides or labels its own section; it never blocks the
  page.
- First run seeds the catalog index from the bundled SCP I and SCP-CN I series
  pages, so random, open-by-number and daily cards work before a full sync.

## Bottom tabs and appearance (Phase 2)

- Bottom navigation: 首页 / 目录 / 书架 / 设置 on Home, Catalog, Library and
  Settings. Tab switches use REORDER_TO_FRONT | SINGLE_TOP; Home uses CLEAR_TOP
  so returning home clears the tab history.
- The selected tab is restored in onResume, so a screen never keeps the tab the
  user tapped on the way out.
- The screen root owns system-bar insets and the BottomNavigationView inset
  padding is cleared, so 3-button and gesture navigation each show a single
  navigation bar without a duplicate blank strip.
- Theme setting: 跟随系统 / 浅色 / 深色. The choice is persisted and applied
  through AppCompatDelegate when the application starts.
- The same setting resolves ReaderTheme.LIGHT or DARK. MainActivity bakes the
  theme into the reader shell, sets the WebView background, and reloads when the
  resolved theme changes while the reader is in the back stack.
- Reader dark CSS covers the body palette, links, blockquotes, tables, code,
  footnote popovers, rating widgets, tabs, collapsibles, modals and common
  buttons. Pages with custom inline colours may keep them.
- BaseActivity keeps status/navigation bar icon appearance in sync with the
  effective night mode.
- The reader overflow menu adds 外观设置, which opens the Settings tab.

## Interactive entries (Phase 3)

The 互动性 tag was sampled across 22 articles. The structures are not uniform:
most put the experience in an HTML block iframe, while others use page-level
onclick/forms or external iframes. Wikidot chrome iframes (interwiki/style/
credit) are noise and should not become reading content.

- ReaderShellBuilder classifies iframes before replacing them:
  - interwiki/style/credit/resize iframes are removed;
  - HTML block iframes (html-block-iframe or a wiki /html/ path) become an
    "互动式内容" card with 进入互动版 and 在浏览器打开;
  - external content iframes become an "外部互动内容" card with 在浏览器打开;
  - page-level onclick/forms get a static notice and 在浏览器打开原页.
- The interactive card uses scparchive://interactive?url=... . reader.js posts
  openInteractive, and MainActivity opens InteractiveActivity.
- InteractiveActivity loads the block in an isolated WebView with JavaScript
  and DOM storage enabled, no Reader bridge, no file/content access, cookies
  off, and external hosts sent to the browser.
- HtmlBlockPreparer removes the parent resize helper and injects a mobile
  viewport; HtmlBlockRepository caches raw blocks for six hours.
- scp-cn-963-j now shows the interactive card instead of a generic embed link.
  Its HTML block starts with the "欢迎使用..." screen and advances to
  "检测到版本更新，请立即升级" after three seconds.

## Validation result

Chrome headless, 504 px viewport, real page CSS loaded:

| page | styles | images | tables | quotes | footnotes | horizontal overflow |
|---|---:|---:|---:|---:|---:|---:|
| scp-173 | 3 | 1 | 0 | 0 | 0 | 0 |
| scp-5000 | 4 | 3 | 6 | 25 | 5 | 0 |
| scp-cn-1000 | 5 | 3 | 0 | 1 | 0 | 0 |
| scp-3029 | 3 | 3 | 1 | 5 | 3 | 0 |

Measured details:

- scp-173 image: natural 1200x981, rendered 300 px wide, https URL, complete.
- scp-5000 images: all three loaded; the large image renders at 300 / 463 px.
- scp-5000 tables: all six are inside the horizontal-scroll wrapper.
- footnote popover opens and shows footnote 1 text.
- image lightbox opens and contains the full image.
- no onclick attributes and no javascript: links remain in the rendered content.
- page content uses 17 px / 1.75 line-height from the reader layer; page title
  keeps the wiki theme colour.

Preview files:

    preview/scp-173.html
    preview/scp-5000.html
    preview/scp-173.png
    preview/scp-5000.png
    preview/stats.json

Regenerate the preview:

    python tools/preview/build_preview.py

## Verify the rendered DOM

The Chrome validation dumps are checked by:

    python tools/preview/verify.py

It asserts image loading, no horizontal overflow, 25 quotes / 6 tables / 5
footnotes on scp-5000, the footnote popover, and the image lightbox. It also
checks scp-cn-1000 for the rating box, the permission prose, and the second tab
becoming visible.

Each page <style> block is emitted as its own style element, so @import rules
inside later blocks stay valid instead of being ignored.

## Build result

Verified in this workspace with a temporary JDK 17 + Android SDK 35:

    assembleDebug      BUILD SUCCESSFUL
    testDebugUnitTest  21 tests, 0 failures

APK:

    app/build/outputs/apk/debug/app-debug.apk

Build note: on Windows, keep GRADLE_USER_HOME on a short writable path. In this
workspace C:\Windows\Temp could make javac fail with AccessDeniedException on
a Gradle transform jar; using the user TEMP path worked. The wrapper
distribution may need to be present or downloaded once.

## Device check

Installed and launched on HQ658W0041 (XQ_FE72, 1080x2340):

- scp-173 loads from the bundled fixture; status shows 1 image, 0 tables, 0
  footnotes; the image and caption are visible.
- scp-5000 loads; status shows 3 images, 6 tables, 5 footnotes.
- WebView DevTools probe: 15,203 characters, 25 blockquotes, 6 tables, 5
  footnotes, 5 footnote refs, no horizontal overflow, 17 px / 1.75 line-height.
- Tapping the first footnote reference opens the popover with footnote 1 text.
- Tapping the first image opens the lightbox with the full image.
- Scrolling to the bottom reports 97% progress through the JS bridge.
- Reload switches the source label to 在线, so the live wiki fetch path works.
- Two lazy images load after scrolling into view; all three images complete.
- scp-cn-1000 loads; status shows 3 images, 0 tables, 0 footnotes.
- scp-cn-1000 keeps the rating module: "评分: +2801+–X", visible at 31 px high.
- scp-cn-1000 keeps the permission prose: the heading "以下内容需要5/CN-1000级权限"
  and the later "好吧，你看到这了" paragraph are both present.
- scp-cn-1000 has three tabs: SAP-001, SAP-069, SAP-090. JavaScript switching and
  physical taps both move to wiki-tab-0-1 and wiki-tab-0-2 correctly.
- scp-cn-1000 has no horizontal overflow and keeps 17 px / 1.75 line-height.
- scp-3029 loads; status shows 3 images, 1 table, 3 footnotes.
- scp-3029 has one collapsible. It starts with unfolded display none and the
  folded label "▼ SCP-3029修订11/04/2019 [展开] ▼".
- A physical tap opens it: unfolded display becomes block, folded becomes none,
  and visible text grows from 792 to 3,949 characters.
- A second physical tap on the close link returns it to display none and the
  folded label; no horizontal overflow appears while open.
- ReaderTheme.LIGHT is the default; ReaderTheme.DARK sets body.reader-dark and
  data-reader-theme="dark". The Settings tab now selects the theme.

- Reading settings were applied on the device: font 17 -> 24 px, line height
  1.8, image toggle switched body class reader-hide-images on and off, and the
  values survived a restart.
- TOC on scp-5000 listed "档案 5000-1：" and jumped to it.
- Reading position: scp-5000 was scrolled to y=20,000; after switching to
  scp-173 and back, it restored to exactly y=20,000.
- Cache: reload changed the source label to 在线; revisiting the page showed
  缓存 (fresh cached HTML).
- Gallery: the first image showed "1 / 3"; next showed "2 / 3".

- Series catalog: SCP 系列 I showed 998 entries from the bundled page; SCP-CN
  系列 I showed 999 entries and kept the selection after returning from an
  article.
- Series II (not bundled) fetched from the network and showed 999 entries with
  source 在线; the shared rate limiter covered this path.
- Back from an opened entry returned to the catalog with the same series.

- 3-button navigation: WebView bottom 2214 px, footnote popover bottom 2182 px,
  navigation bar starts at 2214 px.
- Gesture navigation: WebView bottom 2277 px, footnote popover bottom 2246 px,
  navigation bar starts at 2277 px. The device was restored to 3-button mode
  after the check.

- Catalog sync indexed 14,990 entries across all 15 series.
- Search for "173" returned 35 matches, with SCP-173 first; tapping it opened
  the reader.

- Favorited SCP-173 from the reader; the library showed it under 收藏 and the
  item could be removed.
- Reading history recorded SCP-173, opened it from the 历史 tab, and removed the
  entry; the empty state changed to 暂无阅读历史.
- Removing the favorite changed the library to 暂无收藏.
- The device is left on the library/reader stack with no crash.

- HomeActivity is the launcher; package resolution returns
  cc.kesardev.scparchive/.home.HomeActivity. MainActivity is the reader.
- Home cold launch renders the daily card, continue reading, favorites, quick
  actions, top rated, recent updates, series chips and index stats.
- The daily rotation was stable for 2026-11-14; 换一篇 moved SCP-CN-1000 to
  SCP-173 and updated the tagline.
- 编号 accepted CN－1000 typed with a full-width hyphen and opened
  SCP-CN-1000.
- 随机 opened SCP-2305扩展文档记录 from the local catalog index.
- Tapping the SCP III chip opened 系列目录 with SCP 系列 III selected and 999
  cached entries.
- Opening an article updated the continue-reading row on return.
- 刷新首页 changed both feed labels from 缓存 to 在线.
- With Wi-Fi and mobile data disabled, home still launched from local data; a
  forced refresh changed both feed labels to 离线缓存 without hiding local
  sections.
- Gesture navigation: home content bottom 2277 px; 3-button: 2214 px, matching
  the reader safe-area behaviour. The device was restored to 3-button mode.

- Bottom navigation renders on Home, Catalog, Library and Settings; the
  selected item matches the current screen and resets after returning from
  another tab.
- Tapping 目录, 书架 and 设置 switches activities; back from a tab returns to the
  previous screen with the correct selected tab. Home uses CLEAR_TOP, so back
  from a home tab exits to the launcher.
- 3-button: bottom nav 2004-2214 px; gesture: 2067-2277 px. No duplicate inset
  gap remains.
- 跟随系统 followed the system night mode; 浅色 produced a 254,247,255
  background and 深色 produced 20,18,24. The choice survived force-stop and
  relaunch.
- Explicit 深色 while the system was light also produced a 20,18,24 UI and
  light status-bar icons, so the override is not tied to the system theme.
- Reader dark mode: SCP-CN-1000, SCP-5000, SCP-173 and SCP-3029 used an
  18,18,18 WebView background; page surfaces stayed dark (white pixels were
  confined to article images). SCP-3029's collapsible opened and stayed
  readable.
- Changing the theme from Settings while the reader was in the back stack
  reloaded the reader into the new light/dark palette.
- The reader menu 外观设置 opened the Settings tab.
- Status-bar icons followed the effective theme: light icons in dark mode and
  dark icons in light mode.

- Interactive-content unit tests cover HTML block detection, chrome iframe
  removal, external embed cards, page-level notices and the HTML block
  preparer.
- Chrome headless executed the scp-cn-963-j HTML block: the initial screen was
  visible, and after 4.2 seconds #pageo contained
  "检测到版本更新，请立即升级。 立即升级" with a working upgrade link.

Device screenshots and one Chrome-rendered interactive preview:

    preview/device-scp-173.png
    preview/device-scp-5000.png
    preview/device-cn-1000.png
    preview/device-cn-tab2.png
    preview/device-3029.png
    preview/device-3029-open.png
    preview/device-catalog.png
    preview/device-footnote-safe.png
    preview/device-footnote-gesture.png
    preview/device-search.png
    preview/device-library.png
    preview/device-home.png
    preview/device-home-sections.png
    preview/device-home-dark.png
    preview/device-settings-dark.png
    preview/device-settings-light.png
    preview/device-reader-dark.png
    preview/device-3029-dark.png
    preview/interactive-963.png

## Relationship to SCP-Flutter

No source code was copied from SCP-Flutter. That project is Dart/Flutter under
GPL-3.0; this reader is Kotlin/Android plus JavaScript/CSS. Directly copied
lines: 0.

Design references reviewed from it:

- the verified Wikidot endpoint list and its non-working endpoints;
- the four-layer content flow (offline, cache, network, fallback);
- reading settings exposed as CSS variables and driven through a JS bridge;
- scroll-position persistence and internal/external link splitting;
- a WebView-first renderer instead of a native block renderer.

Anomicon (Apache-2.0) informed the block/warning model; SCP-Android (MIT)
informed the local Wikidot asset + WebView approach. No code from either
project is included. The Phase 1 home layout is original; Anomicon was a visual
reference only.

## Build the Android app

Requirements: JDK 17 and Android SDK 35.

Open the project folder in Android Studio, or run:

    gradlew.bat assembleDebug

The Gradle wrapper jar and scripts are included.

## Structure

    scp-archive/
      app/src/main/java/cc/kesardev/scparchive/
        MainActivity.kt
        ReaderViewModel.kt
        BaseActivity.kt
        ScpArchiveApp.kt
        catalog/CatalogActivity.kt
        catalog/CatalogAdapter.kt
        catalog/CatalogDatabase.kt
        catalog/CatalogEntry.kt
        catalog/CatalogRepository.kt
        catalog/CatalogViewModel.kt
        catalog/SearchActivity.kt
        catalog/SearchViewModel.kt
        catalog/SeriesCache.kt
        catalog/SeriesCatalog.kt
        catalog/SeriesParser.kt
        data/ArticleCache.kt
        data/ArticleRepository.kt
        data/RequestRateLimiter.kt
        home/DailyPicks.kt
        home/HomeActivity.kt
        home/HomeCache.kt
        home/HomeModels.kt
        home/HomeParser.kt
        home/HomeRepository.kt
        home/HomeViewModel.kt
        home/PageInput.kt
        library/LibraryActivity.kt
        library/LibraryAdapter.kt
        library/LibraryDatabase.kt
        library/LibraryViewModel.kt
        reader/ReaderAssets.kt
        reader/ReaderPreferences.kt
        reader/ReaderSettings.kt
        reader/ReaderShellBuilder.kt
        reader/ReaderTheme.kt
        reader/ReaderWebViewClient.kt
        reader/HtmlBlockPreparer.kt
        reader/HtmlBlockRepository.kt
        reader/InteractiveActivity.kt
        settings/AppearancePreferences.kt
        settings/SettingsActivity.kt
        settings/ThemeMode.kt
        ui/BottomTabs.kt
      app/src/main/assets/
        catalog/scp-series.html
        catalog/scp-series-cn.html
        fixtures/scp-173.html
        fixtures/scp-5000.html
        fixtures/scp-cn-1000.html
        fixtures/scp-3029.html
        reader/reader.css
        reader/reader.js
      app/src/test/java/cc/kesardev/scparchive/
        ReaderSettingsTest.kt
        ReaderShellBuilderTest.kt
        SeriesParserTest.kt
        HomeParserTest.kt
        HomeSelectionTest.kt
        ThemeModeTest.kt
        InteractiveContentTest.kt
      app/src/test/resources/pages/
      app/src/main/res/layout/
        activity_home.xml
        activity_settings.xml
        activity_interactive.xml
        item_home_article.xml
        item_home_quick.xml
        item_home_recent.xml
      app/src/main/res/menu/
        menu_bottom.xml
        menu_interactive.xml
      tools/preview/
        build_preview.py
        reader.css
        reader.js
        validate.js
      preview/

## Known limitations

- Raw HTML + catalog JSON caches and a SQLite catalog index; there is no
  cross-device sync or search ranking/recent searches yet.
- Home remote feeds use a small file cache and the toolbar refresh action; there
  is no pull-to-refresh gesture yet. The daily card rotates through a curated
  12-item set rather than a personalized recommendation.
- Bottom tabs are activity-based rather than a single-activity fragment shell.
  Each tab preserves its own screen state; Home deliberately clears the tab
  history when selected.
- Dark mode uses targeted CSS overrides rather than a full re-theme, so pages
  with custom inline colours may remain light in places.
- HTML blocks have an interactive viewer but no static "正文回退" extraction
  yet. Page-level JavaScript interactions in ordinary articles still show the
  browser notice instead of running in the reader.
- Comments and AI are app-level features and are outside this reader scope.
- In-page find uses WebView's deprecated findAllAsync/findNext; it works, but
  there is no match counter yet.
- The WebView strips page scripts; only the reader's own JS runs.
- Position restore uses a scroll ratio, so changing font size or image visibility
  can shift the exact pixel position.

## Next steps

1. Add search ranking and recent searches.
2. Consider a single-activity tab shell if deeper cross-tab state sharing is
   needed.
3. Add a static body fallback for HTML block articles, so the text can be read
   even when the interaction is skipped.
4. Consider a native block renderer for TTS and text-only mode.
5. Add reading statistics or app-level comments if needed.
