# dropbox-fetcher

A lightweight Java utility that automatically downloads all files from a specified Dropbox folder, skips already-downloaded files, and removes them from Dropbox after download.
Designed for scheduled runs (e.g., via cron, systemd timer, or CI job) and supports watch mode to regularly check for files to download.

---

## ✨ Features

- Automatic file download from a Dropbox folder using the Dropbox Java SDK
- Duplicate detection via hashes (stored in an embedded SQLite database)
- Post-download cleanup — deletes files from Dropbox after successful download
- Detailed logging — records all actions (downloaded, skipped, deleted) with hashes and timestamps
- Can be run periodically (ideal for scheduled imports or photo synchronization)

---

## Requirements

- Java 17 or later
- Maven or Gradle for building
- Dropbox API app with access token
- (Optional) cron or similar scheduler if you want periodic execution


## Setup

### 1. Clone and build

```bash
git clone https://github.com/mfisherman/dropbox-fetcher.git
cd dropbox-fetcher
mvn clean package
```

This produces a runnable JAR in `target/dropbox-fetcher-1.0.0-shaded.jar`.

### 2. Configure your Dropbox App

1. Go to the [Dropbox App Console](https://www.dropbox.com/developers/apps)
2. Create a new app with access to your Dropbox files
3. Generate an access token (you can use a permanent one for server-side use)

### 3. Run the program

```bash
java -jar target/dropbox-fetcher.jar \
  --access-token=<YOUR_ACCESS_TOKEN> \
  --dropbox-path="your/folder" \
  --local-download-dir="/path/to/local/folder/to/store/data" \
  --db="/path/to/hashdb.sqlite" \
  --log="/path/to/logfile.log"
```

Example

```bash
java -jar target/dropbox-fetcher.jar \
  --access-token=sl.BCDEF12345xyz \
  --dropbox-path="/Fotos" \
  --local-download-dir="/home/user/photos" \
  --db="/home/user/dropbox-fetcher.db" \
  --log="/home/user/dropbox-fetcher.log"
```

### 4. Run periodically (optional)

Example cron job to run every day at 2am:

```bash
0 2 * * * java -jar /home/user/dropbox-fetcher-1.0.0-shaded.jar \
  --access-token=... \
  --dropbox-path="/Fotos" \
  --local-download-dir="/home/user/photos" \
  --db="/home/user/dropbox-fetcher.db" \
  --log="/home/user/dropbox-fetcher.log"
```

### 5. Run in watch mode (optional)

```bash
java -jar target/dropbox-fetcher.jar \
  --access-token=sl.BCDEF12345xyz \
  --dropbox-path="/Fotos" \
  --local-download-dir="/home/user/photos" \
  --db="/home/user/dropbox-fetcher.db" \
  --log="/home/user/dropbox-fetcher.log" \
  --mode=WATCH
```

## Logging

Every run creates (or appends to) a log file that lists each action.

---

## Developer notes

- Uses **ORMLite** for lightweight SQLite persistence, avoiding raw SQL
- Automatically creates the database and tables on first run
- Deletes only files that were successfully downloaded
- Hashes prevent re-downloading duplicate files

---

## License

GNU General Public License, Version 3.0
See [`LICENSE`](LICENSE) for details.

---


## Support / Contributions

Pull requests welcome.
If you find bugs or want new features, open an [issue](https://github.com/mfisherman/dropbox-fetcher/issues).




