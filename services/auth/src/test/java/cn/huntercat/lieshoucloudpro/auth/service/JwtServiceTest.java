package cn.huntercat.lieshoucloudpro.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import java.util.List;

class JwtServiceTest {

  private JwtService jwt;

  @BeforeEach
  void setUp() {
    jwt = new JwtService("test-secret-must-be-at-least-32-bytes-long-1234", 1800L, 604800L, "test");
    jwt.init();
  }

  @Test
  void generateAccessToken_thenParse() {
    String token =
        jwt.generateAccessToken(
            1L, 1L, "huntercat", "futurewl", List.of("USER"), List.of("legal:use", "crm:use"));
    assertNotNull(token);
    assertTrue(jwt.validate(token));

    Claims c = jwt.parse(token);
    assertEquals("futurewl", c.getSubject());
    assertEquals(1L, c.get("uid", Long.class));
    assertEquals(1L, c.get("tid", Long.class));
    assertEquals("huntercat", c.get("tcode", String.class));
    assertEquals(List.of("USER"), c.get("roles", List.class));
    assertEquals(List.of("legal:use", "crm:use"), c.get("permissions", List.class));
    assertEquals("access", c.get("typ"));
  }

  @Test
  void generateRefreshToken_thenParse() {
    String token = jwt.generateRefreshToken(2L, "alice");
    assertNotNull(token);

    Claims c = jwt.parse(token);
    assertEquals("alice", c.getSubject());
    assertEquals(2L, c.get("uid", Long.class));
    assertEquals("refresh", c.get("typ"));
  }

  @Test
  void invalidToken_rejected() {
    assertFalse(jwt.validate("not.a.real.jwt"));
    assertFalse(jwt.validate(""));
  }

  @Test
  void shortSecret_failsAtInit() {
    JwtService bad = new JwtService("too-short", 1800L, 604800L, "test");
    try {
      bad.init();
      org.junit.jupiter.api.Assertions.fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("JWT_SECRET"));
    }
  }
}
