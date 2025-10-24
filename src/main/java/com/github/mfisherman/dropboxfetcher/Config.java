package com.github.mfisherman.dropboxfetcher;

import picocli.CommandLine.Option;

public class Config {

    @Option(names = {"--access-token"}, description = "Dropbox access token", required = true)
    private String accessToken;

    @Option(names = {"--dropbox-path"}, description = "Dropbox folder path", required = true)
    private String dropboxPath;

    @Option(names = {"--local-download-dir"}, description = "Local download folder to save files", required = true)
    private String localDownloadDir;

    @Option(names = {"--db"}, description = "SQLite database file path", required = true)
    private String dbPath;

    @Option(names = {"--log"}, description = "Log file path", required = true)
    private String logFile;

    public String getAccessToken() {
        return accessToken;
    }

    public String getDropboxPath() {
        return dropboxPath;
    }

    public String getLocalDownloadDir() {
        return localDownloadDir;
    }

    public String getDbPath() {
        return dbPath;
    }

    public String getLogFile() {
        return logFile;
    }
}

