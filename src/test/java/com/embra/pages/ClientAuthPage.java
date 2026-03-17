package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.util.regex.Pattern;

public class ClientAuthPage {
    private final Page page;

    // Locators
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator googleLoginButton;
    private final Locator dashboardHeading;
    private final Locator forgotPasswordLink;
    private final Locator resetEmailInput;
    private final Locator registerLink;
    private final Locator fullNameInput;
    private final Locator registerButton;
    private final Locator otpInputs;
    private final Locator verifyOtpButton;

    public ClientAuthPage(Page page) {
        this.page = page;

        // Form Fields
        this.emailInput = page.locator("input[name='email']");
        this.passwordInput = page.locator("input[name='password']");
        this.fullNameInput = page.locator("input[name='fullName']");

        // Buttons & Links
        this.loginButton = page.locator("button[type='submit']").filter(new Locator.FilterOptions().setHasText("Login"));
        this.googleLoginButton = page.locator("button").filter(new Locator.FilterOptions().setHasText("Login with Google"));
        this.registerLink = page.locator("a[href='/register']");
        this.registerButton = page.locator("button[type='submit']").filter(new Locator.FilterOptions().setHasText("Register"));

        // Dashboard Indicator
        this.dashboardHeading = page.locator("span").filter(new Locator.FilterOptions().setHasText("Dashboard")).first();

        // Forgot Password
        this.forgotPasswordLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Forgot Password?"));
        this.resetEmailInput = page.locator("input[name='email']");

        // OTP
        this.otpInputs = page.locator("div.flex.justify-center.gap-3 input");
        this.verifyOtpButton = page.locator("button:has-text('Verify OTP')");
    }

    public void navigate() {
        page.navigate("https://uat-client.embtalent.ai/login");
    }

    public void loginWithEmail(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }

    public void clickGoogleLogin() { googleLoginButton.click(); }

    public void clickRegister() { registerLink.click(); }

    public void signup(String name, String email, String password) {
        fullNameInput.fill(name);
        this.emailInput.fill(email);
        this.passwordInput.fill(password);
        registerButton.click();
    }

    public void enterOTP(String otp) {
        for (int i = 0; i < otp.length(); i++) {
            Locator input = otpInputs.nth(i);
            input.click();
            input.clear(); // 👈 Add this to clear the wrong OTP before entering the right one
            input.fill(String.valueOf(otp.charAt(i)));
        }
        verifyOtpButton.click();
    }

    public void clickForgotPassword() { forgotPasswordLink.click(); }

    public Locator getDashboardHeading() { return dashboardHeading; }
}