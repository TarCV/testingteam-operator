package com.github.tarcv.tongs.api.result;

public enum StandardFileTypes implements FileType {
    TEST ("tests", "xml"),
    RAW_LOG("logcat", "log"),
    JSON_LOG("logcat_json", "json"),
    SCREENSHOT ("screenshot", "png"),
    ANIMATION ("animation", "gif"),
    SCREENRECORD ("screenrecord", "mp4"),
    COVERAGE ("coverage", "ec"),
    HTML("html", "html"),

    /**
     * Generates filename like `filename.` (with the dot in the end)
     */
    DOT_WITHOUT_EXTENSION("", "")
    ;

    private final String directory;
    private final String suffix;

    StandardFileTypes(String directory, String suffix) {
        this.directory = directory;
        this.suffix = suffix;
    }

    public String getDirectory() {
        return directory;
    }

    public String getSuffix() {
        return suffix;
    }
}
