package com.fangcun.forest.storefront.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CredentialStamp {
    private CredentialStamp() { }
    public static String of(String passwordHash) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(passwordHash.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
