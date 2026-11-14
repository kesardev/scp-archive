# SCP 档案库

面向 SCP 基金会中文分部的非官方 Android 阅读器，使用 Kotlin 与 WebView 构建。

> [!IMPORTANT]
> 本项目是非官方社区项目，与 SCP Wiki 不存在隶属、合作或认可关系。应用展示的 SCP 内容及相关素材归各自创作者所有。

## 功能

- **发现**：每日推荐、动态精选、随机条目、继续阅读、高分条目和最近更新；动态精选可切换评分、热度或混合排序，并可换一批。
- **系列**：SCP 系列与 SCP-CN 系列的编号条目；“故事与设定”独立分区，故事、故事系列和设定均按 EN/CN 分开；SCP 图书馆保留 GoI 格式、SCP 国际和图书馆入口；故事索引可按名称或发布时间展开到每个条目；故事系列页同样按章节分组逐条展开，点击直接进入阅读器。
- **阅读器**：保留 Wikidot 原始 HTML/CSS，支持标签页、折叠块、脚注、图片、目录、页内查找、阅读进度和锚点跳转。
- **动态精选**：结合 Wiki 评分与本地阅读热度排序；阅读热度由阅读次数、最近阅读和收藏计算。
- **书架**：收藏与历史双页，可左右滑动切换，并记录阅读进度。
- **主题**：跟随系统、浅色和深色，阅读器同步生效。
- **网络**：支持 HTTP/SOCKS 自定义代理，默认关闭；文章、目录和图片资源共用同一代理。
- **缓存**：文章、目录、中心页和图片本地缓存，弱网或离线时优先使用已有内容。

## 下载与安装

项目暂未提供正式 Release，可直接从源码构建。

- 包名：cc.kesardev.scparchive
- 最低版本：Android 7.0（API 24）
- APK 输出：app/build/outputs/apk/debug/app-debug.apk

## 技术实现

- Kotlin / Android SDK 35
- WebView + 原始 Wikidot HTML/CSS
- OkHttp + Jsoup
- SQLite
- Material Components / ViewPager2

## 项目结构

    app/src/main/java/cc/kesardev/scparchive/
      data/         文章、缓存、请求与代理
      home/         发现页
      series/       系列与标签页
      hub/          中心页解析与浏览
      reader/       WebView 阅读器与互动内容
      library/      收藏与阅读历史
      catalog/      目录、搜索与 SQLite 索引
      settings/     主题与代理设置
    app/src/main/assets/   内置目录样本与阅读器资源
    app/src/test/          解析与设置单元测试

## 构建

需要 JDK 17。Windows 使用 gradlew.bat，其他平台使用 ./gradlew。

    ./gradlew assembleDebug

构建结果位于 app/build/outputs/apk/debug/app-debug.apk。

推送 `v*` 标签或在 Actions 里手动运行 `Android build`，会自动跑单元测试并产出 debug APK，作为构建产物上传；打标签时还会把 APK 附到对应的 release。

## 数据与权限

应用仅申请 INTERNET 权限，用于加载 Wiki 页面、目录、图片和可选的代理请求。收藏、阅读历史、阅读进度和缓存均保存在设备本地，不包含账号系统，也不会上传到服务器。

## 已知限制

- 首次阅读需要网络；离线内容取决于此前缓存。
- 阅读器基于 WebView，部分互动条目需要在独立互动版中打开。
- 深色模式对使用自定义内联配色的页面可能不完整。
- 目录与中心页解析依赖 Wiki 当前页面结构，页面改版后可能需要更新解析规则。

## 参考项目

以下项目用于交互和实现思路参考：

- [Anomicon](https://github.com/Suixin04/Anomicon)
- [SCP-Flutter](https://github.com/JLeo0001/SCP-Flutter)
- [SCP-Android](https://github.com/zhufree/SCP-Android)
- [ScpReader](https://github.com/zsakvo/ScpReader)

## 许可与署名

应用源代码采用 [MIT License](LICENSE)。

SCP 相关文字、概念和部分视觉素材不适用 MIT，归各自创作者所有，依 Creative Commons Attribution-ShareAlike 3.0 使用。应用图标来自 Wikipedia 的 SCP_Foundation_(emblem).svg，遵循其原始许可。

使用的开源组件包括 OkHttp、Jsoup、AndroidX 和 Material Components。