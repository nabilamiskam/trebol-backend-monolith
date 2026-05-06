package org.trebol.order.adapters.tokens;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;
import org.trebol.order.application.ports.TokenGenerator;

@Component
public class SecureTokenGenerator implements TokenGenerator {

    private static final int BYTES = 32; // 32 bytes -> 64 hex chars
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[BYTES];
        secureRandom.nextBytes(bytes);
        return toHex(bytes); // 64 chars
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hex);
    }
}