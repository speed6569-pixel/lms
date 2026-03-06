package com.example.lms.auth.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 폼 객체
 * - 화면 입력값을 받아 검증하는 용도
 */
public class SignupForm {

    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 30, message = "아이디는 4~30자여야 합니다.")
    private String username;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 형식(8자리)으로 입력해주세요.")
    private String birthDate;

    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별 값이 올바르지 않습니다.")
    private String gender;

    @NotBlank(message = "휴대전화 번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대전화 번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이메일 인증번호를 입력해주세요.")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
    private String emailVerificationCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmailVerificationCode() {
        return emailVerificationCode;
    }

    public void setEmailVerificationCode(String emailVerificationCode) {
        this.emailVerificationCode = emailVerificationCode;
    }
}
