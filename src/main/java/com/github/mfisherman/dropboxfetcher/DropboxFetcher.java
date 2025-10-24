package com.github.mfisherman.dropboxfetcher;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import picocli.CommandLine;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Downloads files from a Dropbox folder, skips duplicates,
 * deletes downloaded files from Dropbox, and logs all activity.
 * Uses SQLite via ORMLite to track previously downloaded files.
 */
public final class DropboxFetcher {
    private DropboxFetcher() {
    }

    private static final Logger logger = Logger.getLogger("DropboxFetcher");
    private static final int LOGGER_MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final int LOGGER_MAX_FILE_COUNT = 10;

    public static void main(String[] args) {
        try {
            // Read command line arguments.
            Config config = new Config();
            CommandLine.populateCommand(config, args);

            setupLogger(config);
            logger.info("START DropboxFetcher");

            DbxClientV2 client = createDropboxClient(config);
            File localDownloadDir = getLocalDownloadDir(config);

            try (DropboxFileDatabase db = new DropboxFileDatabase(config.getDbPath())) {
                processDropboxFolder(client, db, localDownloadDir, config.getDropboxPath());
            }

            logger.info("FINISHED DropboxFetcher");

        } catch (Exception e) {
            logger.severe("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupLogger(Config config) throws Exception {
        FileHandler fh = new FileHandler(config.getLogFile(), LOGGER_MAX_FILE_SIZE, LOGGER_MAX_FILE_COUNT, true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        // Also log to console.
        logger.setUseParentHandlers(true);
    }

    private static DbxClientV2 createDropboxClient(Config config) {
        DbxRequestConfig dropboxConfig = DbxRequestConfig.newBuilder("mfisherman/dropbox-fetcher").build();
        return new DbxClientV2(dropboxConfig, config.getAccessToken());
    }

    private static File getLocalDownloadDir(Config config) throws Exception {
        File localDownloadDir = new File(config.getLocalDownloadDir());
        if (!localDownloadDir.exists()) {
            if (!localDownloadDir.mkdirs()) {
                throw new Exception("Failed to create local download directory: " + localDownloadDir.getAbsolutePath());
            }
        }
        return localDownloadDir;
    }

    private static void processDropboxFolder(DbxClientV2 client, DropboxFileDatabase db, File localDownloadDir,
            String dropboxFolder) throws Exception {
        logger.info("PROCESSING Dropbox folder: " + dropboxFolder);

        // List files in the Dropbox folder.
        ListFolderResult listFolderResult = client.files().listFolder(dropboxFolder);
        while (true) {
            for (Metadata metadata : listFolderResult.getEntries()) {
                if (!(metadata instanceof FileMetadata)) {
                    logger.info("SKIPPED non-file entry: " + metadata.getPathLower());
                    continue;
                }

                processDropboxFile((FileMetadata) metadata, client, db, localDownloadDir);
            }
            if (!listFolderResult.getHasMore()) {
                break;
            }
            listFolderResult = client.files().listFolderContinue(listFolderResult.getCursor());
        }
    }

    private static void processDropboxFile(FileMetadata fileMetadata, DbxClientV2 client,
            DropboxFileDatabase db, File localDownloadDir) throws Exception {
        String dropboxContentHash = fileMetadata.getContentHash();
        String dropboxFilePath = fileMetadata.getPathLower();
        String dropboxFileName = fileMetadata.getName();

        if (db.isDropboxContentHashKnown(dropboxContentHash)) {
            // Skip if the content hash is known.
            logger.info(String.format("SKIPPED duplicate: dropbox_file_path=%s, dropbox_content_hash=%s",
                    dropboxFilePath, dropboxContentHash));
            return;
        }

        // If the file already exists, we use the content hash as suffix, which should
        // be unique.
        File localFile = new File(localDownloadDir, dropboxFileName);
        if (localFile.exists()) {
            int dotIndex = dropboxFileName.lastIndexOf('.');
            String baseName = (dotIndex == -1) ? dropboxFileName : dropboxFileName.substring(0, dotIndex);
            String extension = (dotIndex == -1) ? "" : dropboxFileName.substring(dotIndex);
            localFile = new File(localDownloadDir, baseName + "_" + dropboxContentHash + extension);
        }

        // Download the file.
        try (FileOutputStream out = new FileOutputStream(localFile)) {
            client.files().download(dropboxFilePath).download(out);
            logger.info(String.format("DOWNLOADED: dropbox_file_path=%s, dropbox_content_hash=%s",
                dropboxFilePath, dropboxContentHash));
        } catch (Exception e) {
            logger.severe(String.format("DOWNLOAD FAILED: dropbox_file_path=%s, local_file=%s, reason=%s",
                    dropboxFilePath, localFile.getAbsolutePath(), e.getMessage()));
            if (!localFile.delete()) {
                logger.severe("ERROR to delete corrupted file: " + localFile.getAbsolutePath());
            }
            return;
        }

        // Check downloaded file's Dropbox content hash.
        String computedDropboxContentHash = HashUtils.computeDropboxContentHash(localFile);
        if (!computedDropboxContentHash.equals(dropboxContentHash)) {
            logger.severe(String.format(
                    "HASH MISMATCH: dropbox_file_path=%s, dropbox_content_hash=%s, local_file=%s, computed_dropbox_content_hash=%s",
                    dropboxFilePath, dropboxContentHash, localFile.getAbsolutePath(),
                    computedDropboxContentHash));
            if (!localFile.delete()) {
                logger.severe("ERROR to delete corrupted file: " + localFile.getAbsolutePath());
            }
            return;
        }

        logger.info(String.format("HASH VERIFIED: %s (hash=%s)", localFile.getName(),
                computedDropboxContentHash));

        // Insert into the database.
        DropboxFile dropboxFile = new DropboxFile(localFile.getName(), dropboxFilePath, dropboxFileName,
                HashUtils.computeSha256(localFile), dropboxContentHash);
        db.addFileRecord(dropboxFile);
        logger.info(String.format("UPDATED database: dropbox_file_path=%s, dropbox_content_hash=%s",
                dropboxFilePath, dropboxContentHash));

        // Always try to delete the file: it has been downloaded.
        try {
            client.files().deleteV2(dropboxFilePath);
            logger.info(String.format("DELETED: dropbox_file_path=%s, dropbox_content_hash=%s",
                    dropboxFilePath, dropboxContentHash));
        } catch (Exception ex) {
            logger.warning(String.format("DELETE FAILED: dropbox_file_path=%s, reason=%s",
                    dropboxFilePath, ex.getMessage()));
        }
    }
}
