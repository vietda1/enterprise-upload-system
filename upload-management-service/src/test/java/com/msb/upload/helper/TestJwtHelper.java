package com.msb.upload.helper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generates signed JWT tokens for use in tests.
 * Uses the same secret as application.yml default value.
 */
public class TestJwtHelper {

    public static final String JWT_SECRET =
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    public static String token(String userId, String deptId, String... roles) {
        return Jwts.builder()
            .subject(userId)
            .claim("departmentId", deptId)
            .claim("username", userId)
            .claim("roles", List.of(roles))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    /** Standard user, FIN-001 department */
    public static String userToken() {
        return token("user-1", "FIN-001", "ROLE_USER");
    }

    /** Department manager, FIN-001 department */
    public static String managerToken() {
        return token("mgr-1", "FIN-001", "ROLE_DEPARTMENT_MANAGER");
    }

    /** System admin, no specific department */
    public static String adminToken() {
        return token("admin-1", "SYS-DEPT", "ROLE_ADMIN");
    }

    /** Bearer header value */
    public static String bearer(String token) {
        return "Bearer " + token;
    }
}