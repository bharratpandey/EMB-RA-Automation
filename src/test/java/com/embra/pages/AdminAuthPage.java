package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class AdminAuthPage {
    private final Page page;

    // Locators - Login
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator dashboardHeading;

    // Locators - Forgot Password
    private final Locator forgotPasswordLink;
    private final Locator sendResetLinkBtn;
    private final Locator newPasswordInput;
    private final Locator confirmPasswordInput;
    private final Locator setPasswordBtn;

    public AdminAuthPage(Page page) {
        this.page = page;
        // Login
        this.emailInput = page.locator("input[name='email']");
        this.passwordInput = page.locator("input[name='password']");
        this.loginButton = page.locator("button[type='submit']:has-text('LogIn')");
        this.dashboardHeading = page.locator("h1:has-text('Dashboard')");

        // Forgot Password
        this.forgotPasswordLink = page.locator("a:has-text('Forgot Password?')");
        this.sendResetLinkBtn = page.locator("button:has-text('Send Reset Link')");
        this.newPasswordInput = page.locator("input[name='password']");
        this.confirmPasswordInput = page.locator("input[name='confirmPassword']");
        this.setPasswordBtn = page.locator("button:has-text('Set Password')");
    }

    public void navigate() {
        page.navigate("https://uat-admin.embtalent.ai/login");
    }

    public void login(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
    }

    public void clickForgotPassword() {
        forgotPasswordLink.click();
    }

    public void requestReset(String email) {
        forgotPasswordLink.click();
        emailInput.fill(email);
        sendResetLinkBtn.click();
    }

    public void setNewPassword(String password) {
        newPasswordInput.fill(password);
        confirmPasswordInput.fill(password);
        setPasswordBtn.click();
    }

    public Locator getDashboardHeading() {
        return dashboardHeading;
    }

    // Update this method in AdminAuthPage.java
    // In AdminAuthPage.java
    public Locator getToastByTitle(String title) {
        // This finds any element containing the exact text of the toast title
        // It's much more resilient than data attributes
        return page.locator("li[data-sonner-toast]").getByText(title).first();
    }
    public void waitForToast(String title) {
        page.locator("div[data-title='" + title + "']").waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000)
        );
    }
}