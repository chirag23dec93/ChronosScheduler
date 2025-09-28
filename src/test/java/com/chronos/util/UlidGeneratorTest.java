package com.chronos.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UlidGeneratorTest {

    private static final Pattern ULID_PATTERN = Pattern.compile("^[0123456789ABCDEFGHJKMNPQRSTVWXYZ]{26}$");

    @Test
    void generate_ReturnsValidUlid() {
        // When
        String ulid = UlidGenerator.generate();

        // Then
        assertNotNull(ulid);
        assertEquals(26, ulid.length());
        assertTrue(ULID_PATTERN.matcher(ulid).matches());
    }

    @Test
    void generate_ReturnsUppercaseOnly() {
        // When
        String ulid = UlidGenerator.generate();

        // Then
        assertEquals(ulid.toUpperCase(), ulid);
    }

    @Test
    void generate_DoesNotContainAmbiguousCharacters() {
        // When
        String ulid = UlidGenerator.generate();

        // Then
        assertFalse(ulid.contains("I"));
        assertFalse(ulid.contains("L"));
        assertFalse(ulid.contains("O"));
        assertFalse(ulid.contains("U"));
    }

    @RepeatedTest(100)
    void generate_ReturnsUniqueValues() {
        // Given
        Set<String> generatedUlids = new HashSet<>();

        // When
        for (int i = 0; i < 1000; i++) {
            String ulid = UlidGenerator.generate();
            
            // Then
            assertTrue(generatedUlids.add(ulid), 
                "ULID should be unique, but found duplicate: " + ulid);
        }
    }

    @Test
    void generate_TimestampPortionIncreases() throws InterruptedException {
        // Given
        String ulid1 = UlidGenerator.generate();
        
        // Wait a small amount to ensure timestamp difference
        Thread.sleep(2);
        
        // When
        String ulid2 = UlidGenerator.generate();

        // Then
        // First 10 characters represent timestamp
        String timestamp1 = ulid1.substring(0, 10);
        String timestamp2 = ulid2.substring(0, 10);
        
        // Second timestamp should be greater than or equal to first
        assertTrue(timestamp2.compareTo(timestamp1) >= 0);
    }

    @Test
    void generate_RandomnessPortionVaries() {
        // Given
        Set<String> randomnessParts = new HashSet<>();

        // When
        for (int i = 0; i < 100; i++) {
            String ulid = UlidGenerator.generate();
            String randomnessPart = ulid.substring(10); // Last 16 characters
            randomnessParts.add(randomnessPart);
        }

        // Then
        // Should have high variability in randomness part
        assertTrue(randomnessParts.size() > 95, 
            "Randomness part should vary significantly, got " + randomnessParts.size() + " unique values");
    }

    @Test
    void generate_PerformanceTest() {
        // Given
        int iterations = 10000;
        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < iterations; i++) {
            UlidGenerator.generate();
        }

        // Then
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 1000, 
            "Generating " + iterations + " ULIDs should take less than 1 second, took " + duration + "ms");
    }

    @Test
    void generate_ThreadSafety() throws InterruptedException {
        // Given
        int threadCount = 10;
        int ulidsPerThread = 1000;
        Set<String> allUlids = new HashSet<>();
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ulidsPerThread; j++) {
                    String ulid = UlidGenerator.generate();
                    synchronized (allUlids) {
                        allUlids.add(ulid);
                    }
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertEquals(threadCount * ulidsPerThread, allUlids.size(),
            "All generated ULIDs should be unique across threads");
    }

    @Test
    void generate_LexicographicalOrdering() throws InterruptedException {
        // Given
        String ulid1 = UlidGenerator.generate();
        Thread.sleep(1); // Ensure different timestamp
        String ulid2 = UlidGenerator.generate();
        Thread.sleep(1);
        String ulid3 = UlidGenerator.generate();

        // When & Then
        assertTrue(ulid1.compareTo(ulid2) <= 0);
        assertTrue(ulid2.compareTo(ulid3) <= 0);
        assertTrue(ulid1.compareTo(ulid3) <= 0);
    }

    @Test
    void generate_ValidBase32Encoding() {
        // Given
        String validBase32Chars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

        // When
        String ulid = UlidGenerator.generate();

        // Then
        for (char c : ulid.toCharArray()) {
            assertTrue(validBase32Chars.indexOf(c) >= 0,
                "Character '" + c + "' is not a valid Crockford Base32 character");
        }
    }

    @Test
    void generate_ConsistentLength() {
        // When & Then
        for (int i = 0; i < 100; i++) {
            String ulid = UlidGenerator.generate();
            assertEquals(26, ulid.length(), 
                "ULID length should always be 26 characters");
        }
    }
}
