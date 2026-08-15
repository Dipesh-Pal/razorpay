package com.pal.dipesh.razorpay.common.util;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomizerUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically strong random secret, encoded as a URL-safe
     * Base64 string without padding.
     *
     * <p>The {@code length} parameter specifies the number of random bytes to
     * generate (not the length of the returned string). Because Base64 encodes
     * every 3 bytes into 4 characters and padding is omitted, the resulting
     * string length is {@code ceil(length * 4 / 3)}.
     *
     * <p>Output string length for common inputs:
     * <table border="1">
     *   <caption>Input bytes vs. output string length</caption>
     *   <tr><th>{@code length} (bytes)</th><th>Output string length (chars)</th></tr>
     *   <tr><td>1</td><td>2</td></tr>
     *   <tr><td>2</td><td>3</td></tr>
     *   <tr><td>3</td><td>4</td></tr>
     *   <tr><td>4</td><td>6</td></tr>
     *   <tr><td>8</td><td>11</td></tr>
     *   <tr><td>16</td><td>22</td></tr>
     *   <tr><td>24</td><td>32</td></tr>
     *   <tr><td>32</td><td>43</td></tr>
     *   <tr><td>48</td><td>64</td></tr>
     *   <tr><td>64</td><td>86</td></tr>
     * </table>
     *
     * @param length the number of random bytes to generate; must be non-negative
     * @return a URL-safe, unpadded Base64 encoded random string of length
     *         {@code ceil(length * 4 / 3)}
     */
    public static String randomBase64(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}