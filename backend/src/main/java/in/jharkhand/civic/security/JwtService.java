package in.jharkhand.civic.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
  private final SecretKey key;
  private final Duration ttl;

  public JwtService(@Value("${app.jwt-secret}") String secret,
                    @Value("${app.jwt-expiration-hours:12}") long hours) {
    if (secret == null || secret.isBlank() || secret.length() < 32 || secret.startsWith("change-this") || secret.startsWith("replace-with")) {
      throw new IllegalStateException("JWT_SECRET must be configured with at least 32 random characters.");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.ttl = Duration.ofHours(Math.max(1, hours));
  }

  public String create(String id, String email, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(id)
      .claim("email", email)
      .claim("role", role)
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plus(ttl)))
      .signWith(key)
      .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
