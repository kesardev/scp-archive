package cc.kesardev.scparchive.reader

/**
 * Reader theme contract.
 *
 * LIGHT is the default. DARK is selected from Settings (explicit dark mode or
 * the system theme when following the system) and passed into
 * ReaderShellBuilder so the WebView starts with the correct body class.
 */
enum class ReaderTheme(val bodyClass: String?, val attrValue: String) {
    LIGHT(null, "light"),
    DARK("reader-dark", "dark")
}
