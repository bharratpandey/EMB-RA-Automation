package com.embra.utils;

import com.microsoft.playwright.*;
import java.nio.file.Paths;

public class GoogleSessionGenerator {
    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            // 1. Open a visible browser
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // 2. Go to Google Login
            page.navigate("https://accounts.google.com");

            System.out.println(">>> ACTION REQUIRED: Log into Google manually in the browser window...");

            // 3. Wait for you to finish. It waits until it sees your Google Account page.
            page.waitForURL("**/myaccount.google.com/**", new Page.WaitForURLOptions().setTimeout(120000));

            // 4. SAVE THE KEY (Cookies & Storage)
            // Create a folder named 'auth' in your project root first!
            context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get("auth/google_state.json")));

            System.out.println(">>> SUCCESS: Login state saved to auth/google_state.json!");
            browser.close();
        }
    }
}