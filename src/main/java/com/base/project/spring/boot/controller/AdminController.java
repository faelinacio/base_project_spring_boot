package com.base.project.spring.boot.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Demonstrates role-based access control: only ADMIN-role JWTs may call this. */
@Tag(name = "Admin", description = "Example ADMIN-role-gated endpoint.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Operation(summary = "Admin health check", description = "Requires an access token whose role is ADMIN.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    @ApiResponse(responseCode = "403", description = "Access token role is not ADMIN")
    @GetMapping("/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "scope", "admin");
    }

}
