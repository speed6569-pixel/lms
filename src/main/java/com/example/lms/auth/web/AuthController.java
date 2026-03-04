package com.example.lms.auth.web;

import com.example.lms.auth.service.EmailSenderService;
import com.example.lms.auth.service.EmailVerificationService;
import com.example.lms.auth.service.PasswordResetService;
import com.example.lms.auth.service.UserService;
import com.example.lms.auth.web.form.SignupForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 인증 화면 컨트롤러 (입문용 단순 구조)
 *
 * 현재 단계:
 * - 화면 라우팅
 * - 회원가입 폼 검증
 * - 실제 DB 저장은 다음 단계에서 Service/Repository와 연결
 */
@Controller
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final EmailSenderService emailSenderService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public AuthController(EmailVerificationService emailVerificationService,
                          EmailSenderService emailSenderService,
                          UserService userService,
                          PasswordResetService passwordResetService) {
        this.emailVerificationService = emailVerificationService;
        this.emailSenderService = emailSenderService;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        if (loggedIn || session.getAttribute("loginUserId") != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String loginIdOrEmail,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        passwordResetService.requestReset(loginIdOrEmail, baseUrl);
        redirectAttributes.addFlashAttribute("message", "계정이 존재하면 비밀번호 재설정 메일이 발송됩니다. 메일을 확인해주세요.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        boolean valid = passwordResetService.isTokenValid(token);
        model.addAttribute("token", token);
        model.addAttribute("valid", valid);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (newPassword == null || newPassword.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            model.addAttribute("token", token);
            model.addAttribute("valid", passwordResetService.isTokenValid(token));
            model.addAttribute("error", "새 비밀번호를 입력해주세요.");
            return "auth/reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("valid", passwordResetService.isTokenValid(token));
            model.addAttribute("error", "비밀번호 확인이 일치하지 않습니다.");
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("message", "비밀번호 변경이 완료되었습니다. 다시 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("valid", false);
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup";
    }

    @GetMapping("/signup/sso")
    public String ssoSignupPage() {
        return "auth/sso-signup";
    }

    @PostMapping("/signup/send-code")
    public String sendEmailVerificationCode(
            @ModelAttribute("signupForm") SignupForm signupForm,
            Model model,
            HttpSession session
    ) {
        if (signupForm.getEmail() == null || signupForm.getEmail().isBlank()) {
            model.addAttribute("emailError", "이메일을 먼저 입력해주세요.");
            return "auth/signup";
        }

        if (!emailVerificationService.canResend(session)) {
            long remain = emailVerificationService.getRemainResendSeconds(session);
            model.addAttribute("emailError", "인증번호 재전송은 " + remain + "초 후 가능합니다.");
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
            return "auth/signup";
        }

        String code = emailVerificationService.issueCode(signupForm.getEmail(), session);

        try {
            emailSenderService.sendVerificationCode(signupForm.getEmail(), code);
            model.addAttribute("emailMessage", "인증번호를 이메일로 보냈습니다. (유효시간 5분)");
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
        } catch (Exception e) {
            // 개발/로컬 환경 fallback: SMTP 미설정 시 화면에 코드 노출(운영에서는 제거 권장)
            model.addAttribute("emailError", "이메일 전송에 실패했습니다. 개발모드 인증번호를 사용하세요.");
            model.addAttribute("devVerificationCode", code);
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
        }

        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("signupForm") SignupForm signupForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
            return "auth/signup";
        }

        boolean validCode = emailVerificationService.isValid(
                signupForm.getEmail(),
                signupForm.getEmailVerificationCode(),
                session
        );

        if (!validCode) {
            model.addAttribute("emailError", "인증번호가 올바르지 않습니다. 올바른 번호를 입력해주세요.");
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
            return "auth/signup";
        }

        try {
            userService.registerUser(
                    signupForm.getUsername(),
                    signupForm.getPassword(),
                    signupForm.getName()
            );
        } catch (IllegalArgumentException e) {
            model.addAttribute("emailError", e.getMessage());
            model.addAttribute("expireSeconds", emailVerificationService.getRemainExpireSeconds(session));
            return "auth/signup";
        }

        redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }
}
