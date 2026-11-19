package com.workorder.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${token.expire:86400000}")
    private long expire;

    @Value("${token.signature:work-order}")
    private String signature;

    public Map<String, Object> parseToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .setSigningKey(signature.getBytes())
                .parseClaimsJws(token);
        return jws.getBody();
    }

    public String makeToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, signature.getBytes())
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .compact();
    }
}
