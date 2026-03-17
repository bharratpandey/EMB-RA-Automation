package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class VendorAuthPage {
    private final Page page;

    // Locators
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator googleLoginButton;
    private final Locator successToastTitle;
    private final Locator welcomeMessage;
    private final Locator dashboardHeading;
    private final Locator forgotPasswordLink;
    private final Locator resetEmailInput;
    private final Locator openGmailButton;
    private final Locator newPasswordInput;
    private final Locator confirmPasswordInput;
    private final Locator setPasswordButton;
    private final Locator signupLink;
    private final Locator fullNameInput;
    private final Locator registerButton;
    private final Locator otpInputs;
    private final Locator verifyOtpButton;
    private final Locator otpErrorToast;
    private final Locator otpInlineError;
    private final Locator registrationSuccessSubtext;
    private final Locator onboardingSubtext;

    public VendorAuthPage(Page page) {
        this.page = page;

        // Form Fields
        this.emailInput = page.locator("input[name='email']");
        this.passwordInput = page.locator("input[name='password']");

        // Buttons
        this.loginButton = page.locator("button[type='submit']:has-text('Login')");
        this.googleLoginButton = page.locator("button:has-text('Login with Google')");

        // Success Indicators
        this.successToastTitle = page.locator("span.text-toast");
        this.welcomeMessage = page.locator("span.text-body-2").filter(new Locator.FilterOptions().setHasText("Welcome"));
        this.dashboardHeading = page.locator("span.font-semibold-important").getByText("Dashboard");

        // Forgot Password
        this.forgotPasswordLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Forgot Password?"));
        this.resetEmailInput = page.locator("input[name='email']");
        this.openGmailButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open Gmail"));
        this.newPasswordInput = page.locator("input[name='password']");
        this.confirmPasswordInput = page.locator("input[name='confirmPassword']");
        this.setPasswordButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Set password"));

        // Signup & OTP
        this.signupLink = page.locator("a:has-text('Sign Up')");
        this.fullNameInput = page.locator("input[name='fullName']");
        this.registerButton = page.locator("button[type='submit']:has-text('Register')");
        this.otpInputs = page.locator("div.flex.justify-center.gap-3 input");
        this.verifyOtpButton = page.locator("button:has-text('Verify OTP')");
        this.otpErrorToast = page.locator("span:has-text('Your OTP has expired')");
        this.otpInlineError = page.locator("p.text-red-500");
        // We use the unique text from your HTML to find this specific span
        // FIX: Initialize both variables here
        this.onboardingSubtext = page.locator("span:has-text('You are just 1 step away from signing your first deal')");
        this.registrationSuccessSubtext = this.onboardingSubtext; // Pointing to the same locator
    }

    public void navigate() {
        page.navigate("https://uat-vendor.embtalent.ai/login");
    }



    public void signup(String name, String email, String password) {
        this.fullNameInput.fill(name);
        this.page.locator("input[name='email']").fill(email);
        this.page.locator("input[name='password']").fill(password);
        this.registerButton.click();
    }
    public Locator getOtpInlineErrorLocator() {
        return otpInlineError;
    }

    public void enterOTP(String otp) {
        for (int i = 0; i < otp.length(); i++) {
            Locator input = otpInputs.nth(i);
            input.click();
            // Use clear() for a cleaner reset across OS types
            input.clear();
            input.fill(String.valueOf(otp.charAt(i)));
        }
        this.verifyOtpButton.click();
    }


    public String getOtpInlineErrorText() {
        // Wait for the error to appear before grabbing text
        otpInlineError.waitFor();
        return otpInlineError.textContent().trim();
    }

    public Locator getRegistrationSuccessSubtext() {
        return registrationSuccessSubtext;
    }
    public Locator getOnboardingSubtext() {
        return onboardingSubtext;
    }

    // Existing methods
    public void loginWithEmail(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }

    public void clickGoogleLogin() { googleLoginButton.click(); }
    public void clickForgotPassword() { forgotPasswordLink.click(); }
    public void requestReset(String email) {
        resetEmailInput.fill(email);
        page.keyboard().press("Enter");
    }

    // Getters for Assertions
    public Locator getSuccessToastTitle() { return successToastTitle; }
    public Locator getWelcomeMessage() { return welcomeMessage; }
    public Locator getDashboardHeading() { return dashboardHeading; }
    public Locator getOtpErrorToast() { return otpErrorToast; }
}