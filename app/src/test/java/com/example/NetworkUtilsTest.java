package com.example;

import com.example.utils.NetworkUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Testes unitários para utilitários de rede e formatação.
 */
public class NetworkUtilsTest {

    @Test
    public void testFormatSpeedKb() {
        String speed = NetworkUtils.formatSpeed(1024 * 50, false);
        assertEquals("50.0 KB/s", speed);
    }

    @Test
    public void testFormatSpeedMb() {
        String speed = NetworkUtils.formatSpeed(1024 * 1024 * 2, true);
        assertEquals("2.00 MB/s", speed);
    }

    @Test
    public void testFormatBytes() {
        String bytes = NetworkUtils.formatBytes(1024 * 1024 * 5);
        assertEquals("5.0 MB", bytes);
    }

    @Test
    public void testChecksum() {
        byte[] buffer = new byte[] { 0x45, 0x00, 0x00, 0x3c, 0x1c, 0x46, 0x40, 0x00, 0x40, 0x06, 0x00, 0x00 };
        int checksum = NetworkUtils.computeChecksum(buffer, 0, buffer.length);
        assertTrue(checksum > 0);
    }
}
