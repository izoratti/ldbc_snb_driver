package com.ldbc.driver.util;

import com.google.common.base.Charsets;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class CsvFileReader implements Iterator<String[]> {
    public static final Pattern COLUMN_SEPARATOR_PATTERN = Pattern.compile("\\|");
    private final Pattern columnSeparatorPattern;
    private final BufferedReader csvReader;

    private String[] next = null;
    private boolean closed = false;

    public CsvFileReader(File csvFile) throws FileNotFoundException {
        this(csvFile, COLUMN_SEPARATOR_PATTERN);
    }

    public CsvFileReader(File csvFile, String regexSeparator) throws FileNotFoundException {
        this(csvFile, Pattern.compile(regexSeparator));
    }

    public CsvFileReader(File csvFile, Pattern regexSeparatorPattern) throws FileNotFoundException {
        this.csvReader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), Charsets.UTF_8));
        this.columnSeparatorPattern = regexSeparatorPattern;
    }

    @Override
    public boolean hasNext() {
        if (closed) return false;
        next = (next == null) ? nextLine() : next;
        // TODO closeReader() really should be closed explicitly
        if (null == next) closed = closeReader();
        return (null != next);
    }

    @Override
    public String[] next() {
        next = (null == next) ? nextLine() : next;
        if (null == next) throw new NoSuchElementException("No more lines to read");
        String[] tempNext = next;
        next = null;
        return tempNext;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private String[] nextLine() {
        String csvLine;
        try {
            csvLine = csvReader.readLine();
            if (null == csvLine) return null;
            return parseLine(csvLine);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error retrieving next csv entry from file [%s]", csvReader), e);
        }
    }

    private String[] parseLine(String csvLine) {
        return columnSeparatorPattern.split(csvLine, -1);
    }

    public boolean closeReader() {
        if (closed) {
            return true;
            // TODO this really should throw an exception
//            String errMsg = "Can not close file multiple times";
//            throw new RuntimeException(errMsg);
        }
        if (null == csvReader) {
            throw new RuntimeException("Can not close file - reader is null");
        }
        try {
            csvReader.close();
        } catch (IOException e) {
            String errMsg = String.format("Error closing file [%s]", csvReader);
            throw new RuntimeException(errMsg, e);
        }
        return true;
    }
}
