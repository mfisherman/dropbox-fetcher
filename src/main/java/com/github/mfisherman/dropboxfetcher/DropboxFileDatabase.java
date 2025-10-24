package com.github.mfisherman.dropboxfetcher;

import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

class DropboxFileDatabase implements AutoCloseable {

    private final ConnectionSource connectionSource;
    private final Dao<DropboxFile, Integer> fileDao;

    public DropboxFileDatabase(String dbPath) throws SQLException {
        String dbUrl = "jdbc:sqlite:" + dbPath;
        connectionSource = new JdbcConnectionSource(dbUrl);
        fileDao = DaoManager.createDao(connectionSource, DropboxFile.class);

        // Create table if it does not exist.
        TableUtils.createTableIfNotExists(connectionSource, DropboxFile.class);
    }

    public boolean isDropboxContentHashKnown(String dropboxContentHash) throws SQLException {
        DropboxFile existing = fileDao.queryBuilder()
                .where()
                .eq("dropboxContentHash", dropboxContentHash)
                .queryForFirst();
        return existing != null;
    }

    public void addFileRecord(DropboxFile dropboxFile) throws SQLException {
        fileDao.createIfNotExists(dropboxFile);
    }

    @Override
    public void close() throws Exception {
        connectionSource.close();
    }
}
