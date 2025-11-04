package com.github.mfisherman.dropboxfetcher;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleLogFormatter extends Formatter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(record.getMillis())))
          .append(" ")
          .append(record.getLevel().getName())
          .append(": ")
          .append(formatMessage(record))
          .append(System.lineSeparator());
        return sb.toString();
    }
}
