package io.github.rutwoklabs.jbuildpilot.downloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumVerifierTest {

    @TempDir
    Path tempDir;

    private final ChecksumVerifier verifier = new ChecksumVerifier();

    // Known digests of the ASCII string "hello".
    private static final String HELLO = "hello";
    private static final String SHA256_HELLO =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final String SHA512_HELLO =
            "9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca7"
          + "2323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043";

    private Path writeHello() throws Exception {
        Path f = tempDir.resolve("data.bin");
        Files.writeString(f, HELLO);
        return f;
    }

    @Test
    void verifiesSha256ByLength() throws Exception {
        assertTrue(verifier.verify(writeHello(), SHA256_HELLO));
    }

    @Test
    void verifiesSha512ByLength() throws Exception {
        assertTrue(verifier.verify(writeHello(), SHA512_HELLO));
    }

    @Test
    void isCaseInsensitive() throws Exception {
        assertTrue(verifier.verify(writeHello(), SHA256_HELLO.toUpperCase()));
    }

    @Test
    void rejectsMismatchedContent() throws Exception {
        Path f = tempDir.resolve("other.bin");
        Files.writeString(f, "goodbye");
        assertFalse(verifier.verify(f, SHA256_HELLO));
    }

    @Test
    void rejectsNullChecksum() throws Exception {
        assertFalse(verifier.verify(writeHello(), null));
    }

    @Test
    void rejectsEmptyChecksum() throws Exception {
        assertFalse(verifier.verify(writeHello(), "   "));
    }

    @Test
    void rejectsWeakOrUnknownHashLength() throws Exception {
        // 40 hex chars = SHA-1; must be rejected rather than silently downgraded.
        String sha1Length = "a".repeat(40);
        assertFalse(verifier.verify(writeHello(), sha1Length));
    }

    @Test
    void verifyOptionalSkipsOnNull() throws Exception {
        assertTrue(verifier.verifyOptional(writeHello(), null));
    }

    @Test
    void verifyOptionalStillChecksWhenPresent() throws Exception {
        assertTrue(verifier.verifyOptional(writeHello(), SHA256_HELLO));
        assertFalse(verifier.verifyOptional(writeHello(), SHA512_HELLO.replace('9', '0')));
    }
}
