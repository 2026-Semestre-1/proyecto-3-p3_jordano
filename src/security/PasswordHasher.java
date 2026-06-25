package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordHasher - SHA-256 password hashing utility.
 *
 * Silberschatz Ch.17 "Protection":
 *   "A common technique is to store a hash of the password.
 *    When a login attempt is made, the hash of the supplied password is compared
 *    to the stored hash. Plain-text passwords must never be stored."
 *
 * Stallings "Operating Systems":
 *   "Cryptographic hash functions like SHA-256 produce a fixed-size digest
 *    from arbitrary input. Even small changes produce completely different digests."
 *
 * We use SHA-256 (256-bit hash). In production, bcrypt or Argon2 with a salt
 * would be preferred, but SHA-256 fulfills the academic requirement here.
 */
public class PasswordHasher {

    private PasswordHasher() {} // utility class — no instantiation

    /**
     * Computes the SHA-256 hex digest of a plaintext password.
     *
     * @param plaintext the raw password string
     * @return hex-encoded SHA-256 hash, or null if SHA-256 is unavailable
     */
    public static String hash(String plaintext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible en esta JVM", e);
        }
    }

    /**
     * Verifies a plaintext password against a stored hash.
     *
     * @param plaintext    the password attempt
     * @param storedHash   the previously stored SHA-256 hash
     * @return true if the password matches
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null) return false;
        return storedHash.equals(hash(plaintext));
    }
}
