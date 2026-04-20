package com.example.lms.admin.dto;

public class AdminUserDtos {
    public record UserUpdateRequest(String role, String status) {}
}
