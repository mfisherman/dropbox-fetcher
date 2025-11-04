package com.github.mfisherman.dropboxfetcher;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "downloaded_files")
class DropboxFile {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String localFileName;

    @DatabaseField(canBeNull = false)
    private String dropboxPath;

    @DatabaseField(canBeNull = false)
    private String dropboxFileName;

    @DatabaseField(unique = true, canBeNull = false)
    private String sha256;

    @DatabaseField(unique = true, canBeNull = false)
    private String dropboxContentHash;

    @DatabaseField(canBeNull = false)
    private Date downloadedAt;

    public DropboxFile() {
        // ORMLite requires a no-arg constructor
    }

    public DropboxFile(String localFileName, String dropboxPath, String dropboxFileName, String sha256,
            String dropboxContentHash) {
        this.localFileName = localFileName;
        this.dropboxPath = dropboxPath;
        this.dropboxFileName = dropboxFileName;
        this.sha256 = sha256;
        this.dropboxContentHash = dropboxContentHash;
        this.downloadedAt = new java.util.Date();
    }
}
