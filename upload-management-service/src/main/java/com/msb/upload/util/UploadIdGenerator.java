package com.msb.upload.util;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates human-readable upload IDs: UPL-YYYY-NNNNNN
 * Thread-safe using AtomicInteger sequence.
 */
public final class UploadIdGenerator {

    private static final AtomicInteger seq = new AtomicInteger(0);

    private UploadIdGenerator() {}

    public static String generate() {
        int year = LocalDateTime.now().getYear();
        int n    = seq.incrementAndGet() % 1_000_000;
        return String.format("UPL-%d-%06d", year, n);
    }
}
