package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;

public class LoginPage {

    private final Page page;

    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;

    public LoginPage(Page page) {
        this.page = page;

        // Multiple selectors for robustness
        this.emailInput = page.locator("input#email, input[name='email']");
        this.passwordInput = page.locator("input#password, input[name='password']");

        // Using AriaRole.BUTTON - correct enum usage
        this.loginButton = page.getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText("LogIn"));
    }

    public boolean login(String email, String password) {
        try {
            // Wait for email field to be ready
            emailInput.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(15000));

            emailInput.fill(email);
            passwordInput.fill(password);

            safeClick(loginButton);

            // Improved post-login validation
            // Check multiple possible dashboard URLs
            page.waitForURL(url ->
                            url.contains("/client") ||
                                    url.contains("/dashboard") ||
                                    url.contains("/requirement") ||
                                    url.contains("/requirements"),
                    new Page.WaitForURLOptions().setTimeout(25000));

            // Wait for a strong indicator that we're on the dashboard
            page.waitForSelector("text=New Requirement",
                    new Page.WaitForSelectorOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(20000));

            return true;
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            takeScreenshot("login_failure");
            return false;
        }
    }

    private void safeClick(Locator locator) {
        locator.scrollIntoViewIfNeeded();

        try {
            locator.click();
        } catch (Exception e1) {
            try {
                locator.click(new Locator.ClickOptions().setForce(true));
            } catch (Exception e2) {
                // JavaScript fallback click
                page.evaluate("el => el.click()", locator);
            }
        }
    }

    private void takeScreenshot(String prefix) {
        String filename = "screenshots/" + prefix + "_" + System.currentTimeMillis() + ".png";
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get(filename))
                .setFullPage(true));
    }
}