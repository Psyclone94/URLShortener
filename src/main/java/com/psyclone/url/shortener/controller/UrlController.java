package com.psyclone.url.shortener.controller;

import com.psyclone.url.shortener.dto.UrlDtos.CreateUrlRequest;
import com.psyclone.url.shortener.dto.UrlDtos.CreateUrlResponse;
import com.psyclone.url.shortener.domain.UrlMapping;
import com.psyclone.url.shortener.service.URLService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class UrlController {

    private final URLService service;
    private final String baseUrl;

    public UrlController(URLService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        UrlMapping m = service.create(request.longUrl(), request.alias(), request.expiresAt());
        var body = new CreateUrlResponse(
                m.shortCode(),
                baseUrl + "/" + m.shortCode(),
                m.longUrl(),
                m.expiresAt(),
                m.createdAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        UrlMapping m = service.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(m.longUrl()))
                .build();
    }
}
