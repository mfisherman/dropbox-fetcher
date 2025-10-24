package com.github.mfisherman.dropboxfetcher;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    private static final int DROPBOX_BLOCK_SIZE = 4 * 1024 * 1024; // 4 MiB

    private HashUtils() {
        // Private constructor to prevent instantiation.
    }

    // Standard SHA-256 computation.
    public static String computeSha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return bytesToHex(digest.digest());
    }

    // Dropbox Content Hash computation.
    public static String computeDropboxContentHash(File file) throws IOException, NoSuchAlgorithmException {
        try (ByteArrayOutputStream blockHashes = new ByteArrayOutputStream()) {

            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[DROPBOX_BLOCK_SIZE];
                int bytesRead;

                MessageDigest blockDigest = MessageDigest.getInstance("SHA-256");
                while ((bytesRead = in.read(buffer)) != -1) {
                    blockDigest.update(buffer, 0, bytesRead);
                    blockHashes.write(blockDigest.digest());
                }
            }

            byte[] finalHash = MessageDigest.getInstance("SHA-256")
                    .digest(blockHashes.toByteArray());
            return bytesToHex(finalHash);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
