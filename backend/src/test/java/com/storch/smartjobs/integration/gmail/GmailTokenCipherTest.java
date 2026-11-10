package com.storch.smartjobs.integration.gmail;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GmailTokenCipherTest {
    @Test
    void encryptsAndAuthenticatesStoredTokens() {
        GmailTokenCipher cipher = new GmailTokenCipher("local-test-secret-that-is-long-enough");
        String encrypted = cipher.encrypt("sensitive-access-token");
        assertNotEquals("sensitive-access-token", encrypted);
        assertEquals("sensitive-access-token", cipher.decrypt(encrypted));
        String userBound = cipher.encrypt("user-token", "user-7");
        assertEquals("user-token", cipher.decrypt(userBound, "user-7"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(userBound, "user-8"));
        assertThrows(IllegalStateException.class, () -> new GmailTokenCipher("different-secret-that-is-long-enough").decrypt(encrypted));
    }

    @Test
    void rejectsShortEncryptionKeys() {
        assertThrows(IllegalArgumentException.class, () -> new GmailTokenCipher("too-short"));
    }
}
