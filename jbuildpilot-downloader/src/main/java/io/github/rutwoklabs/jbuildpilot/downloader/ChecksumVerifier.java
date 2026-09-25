package io.github.rutwoklabs.jbuildpilot.downloader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class ChecksumVerifier {
    public boolean verify(Path file, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.trim().isEmpty()) {
            return false; // Security fix: null checksums are unconditionally rejected by default
        }
        return doVerify(file, expectedChecksum);
    }

    public boolean verifyOptional(Path file, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.trim().isEmpty()) {
            System.out.println("WARNING: Legitimate no-checksum-available case triggered. Skipping verification.");
            return true;
        }
        return doVerify(file, expectedChecksum);
    }

    private boolean doVerify(Path file, String expectedChecksum) {
        String expected = expectedChecksum.trim();
        // Select the digest algorithm from the expected hash length so pinned SHA-256
        // (64 hex) and SHA-512 (128 hex) values both verify. Weaker/unknown lengths
        // (e.g. SHA-1's 40 hex) are rejected rather than silently downgraded.
        String algorithm;
        switch (expected.length()) {
            case 64 -> algorithm = "SHA-256";
            case 128 -> algorithm = "SHA-512";
            default -> {
                return false;
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculated = sb.toString();
            return calculated.equalsIgnoreCase(expected);
        } catch (Exception e) {
            return false; // IO Error or Algo absent implies failed verification
        }
    }
}
