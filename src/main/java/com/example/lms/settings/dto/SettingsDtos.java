package com.example.lms.settings.dto;

import java.util.List;

public class SettingsDtos {

    public record MeResponse(
            Long id,
            String username,
            String name,
            String email,
            String phone,
            String role,
            Integer pointBalance,
            List<LoginHistoryItem> loginHistory,
            List<DeviceItem> devices
    ) {}

    public record LoginHistoryItem(
            String loginTime,
            String ipAddress,
            String userAgent
    ) {}

    public record DeviceItem(
            String ipAddress,
            String userAgent,
            String lastLoginTime
    ) {}

    public record UpdateProfileRequest(
            String name,
            String phone
    ) {}

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {}
}
