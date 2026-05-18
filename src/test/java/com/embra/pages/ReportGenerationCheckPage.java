package com.embra.pages;

import com.embra.utils.DashboardManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;

public class ReportGenerationCheckPage {

    private final Page page;

    private static final String ADMIN_URL = "https://admin.embtalent.ai/login";
    private static final String EMAIL     = "bharat.pandey@emb.global";
    private static final String PASSWORD  = "Emb@1234";

    public ReportGenerationCheckPage(Page page) {
        this.page = page;
    }

    // ──────────────────────────────────────────────────────────────
    // 1. LOGIN
    // ──────────────────────────────────────────────────────────────

    public void login() {
        DashboardManager.log("\n--- 🔑 ADMIN LOGIN ---");
        page.navigate(ADMIN_URL);
        page.locator("input[name='email']").fill(EMAIL);
        page.locator("input[name='password']").fill(PASSWORD);
        page.locator("button[type='submit']").click();

        try {
            page.locator("li[data-sonner-toast][data-type='success']")
                    .filter(new Locator.FilterOptions().setHasText("Login Successful"))
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            DashboardManager.log("   ✅ Login Successful toast verified.");
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Login toast not found. Proceeding...");
        }
        page.waitForLoadState();
        page.waitForTimeout(2000);
    }

    // ──────────────────────────────────────────────────────────────
    // 2. NAVIGATE TO REQUIREMENT LISTING
    // ──────────────────────────────────────────────────────────────

    public void navigateToRequirementListing() {
        DashboardManager.log("\n--- 📋 NAVIGATING TO REQUIREMENT LISTING ---");
        page.locator("a[href='/hiring-requests']").first().click();
        page.waitForLoadState();
        page.waitForTimeout(2000);
        DashboardManager.log("   ✅ Requirement Listing page loaded.");
    }

    // ──────────────────────────────────────────────────────────────
    // 3. OPEN SEARCH & FILTERS
    // ──────────────────────────────────────────────────────────────

    public void openSearchFilters() {
        DashboardManager.log("\n--- 🔍 OPENING SEARCH & FILTERS ---");
        page.locator("div.font-semibold")
                .filter(new Locator.FilterOptions().setHasText("Search & Filters"))
                .first().click();
        page.waitForTimeout(1000);
        DashboardManager.log("   ✅ Search & Filters expanded.");
    }

    // ──────────────────────────────────────────────────────────────
    // 4. CLICK DOWNLOAD REPORTS & CAPTURE TIME
    // ──────────────────────────────────────────────────────────────

    public String clickDownloadReports() {
        DashboardManager.log("\n--- 📥 CLICKING DOWNLOAD REPORTS ---");

        Locator downloadBtn = page.locator("button")
                .filter(new Locator.FilterOptions().setHasText("Download Reports"))
                .first();
        downloadBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

        try {
            Response response = page.waitForResponse(
                    r -> r.url().contains("/api/v1/emb/requirement/all") &&
                            r.url().contains("report=true"),
                    new Page.WaitForResponseOptions().setTimeout(15000),
                    () -> downloadBtn.click()
            );
            DashboardManager.log("   ✅ API Status: " + response.status() + " — " + response.url());
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ API response capture failed: " + e.getMessage());
            try { downloadBtn.click(); } catch (Exception ignored) {}
        }

        String clickTime = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US));
        DashboardManager.log("   ✅ Download Reports clicked at: " + clickTime);

        try {
            page.locator("li[data-sonner-toast][data-type='success']")
                    .filter(new Locator.FilterOptions().setHasText("Reports has been generated"))
                    .waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE).setTimeout(15000));
            DashboardManager.log("   ✅ Toast: Reports has been generated and sent to your email");
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Success toast not found: " + e.getMessage());
        }

        return clickTime;
    }

    // ──────────────────────────────────────────────────────────────
    // 5. OPEN GMAIL & WAIT FOR REPORT EMAIL
    // Returns the download link found inside the email body
    // ──────────────────────────────────────────────────────────────

    public String openGmailAndVerifyReportEmail(Page gmailPage, String reportClickTime) {
        DashboardManager.log("\n--- 📧 OPENING GMAIL TO VERIFY REPORT EMAIL ---");
        DashboardManager.log("   -> Report was requested at: " + reportClickTime);

        String downloadLink = "Not found";

        gmailPage.bringToFront();
        gmailPage.navigate("https://mail.google.com");
        gmailPage.waitForLoadState();
        gmailPage.waitForTimeout(3000);

        // Convert click time to minutes for comparison
        int clickMinutes = -1;
        try {
            clickMinutes = convertToMinutes(reportClickTime);
            DashboardManager.log("   -> Click time in minutes: " + clickMinutes
                    + " (" + reportClickTime + ")");
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not convert click time: " + e.getMessage());
        }

        DashboardManager.log("   -> Waiting for report email (refreshing every 5s, max 3 min)...");

        boolean emailFound = false;
        int maxAttempts = 36;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            DashboardManager.log("   -> Attempt " + attempt + "/" + maxAttempts + " — checking inbox...");

            gmailPage.reload();
            gmailPage.waitForTimeout(3000);

            // Find emails from Team EMBTalent with report subject
            Locator emailRows = gmailPage.locator("tr.zA")
                    .filter(new Locator.FilterOptions().setHasText("Team EMBTalent"))
                    .filter(new Locator.FilterOptions().setHasText("EMBTalent: Your Requested Report"));

            int count = emailRows.count();
            DashboardManager.log("   -> Found " + count + " matching email(s).");

            for (int i = 0; i < count; i++) {
                Locator row = emailRows.nth(i);
                boolean shouldSkip = false;

                try {
                    String titleAttr = row.locator("td.xW span[title]").getAttribute("title");
                    DashboardManager.log("   -> Email title attr: " + titleAttr);

                    if (titleAttr != null && clickMinutes >= 0) {
                        // Extract time from "Thu, May 14, 2026, 4:07 PM"
                        String timePart = titleAttr.substring(titleAttr.lastIndexOf(", ") + 2)
                                .replaceAll("[^0-9:AaPpMm ]", "")
                                .replaceAll("\\s+", " ")
                                .trim();
                        DashboardManager.log("   -> Email received at: " + timePart);

                        try {
                            int emailMinutes = convertToMinutes(timePart);
                            DashboardManager.log("   -> Email minutes: " + emailMinutes
                                    + ", Click minutes: " + clickMinutes);

                            if (emailMinutes < clickMinutes) {
                                DashboardManager.log("   ⏭️ Email at " + timePart
                                        + " is before click time " + reportClickTime + " — skipping.");
                                shouldSkip = true;
                            } else {
                                DashboardManager.log("   ✅ Email time " + timePart
                                        + " is at or after click time — valid!");
                            }
                        } catch (Exception ex) {
                            DashboardManager.log("   ⚠️ Time comparison failed: "
                                    + ex.getMessage() + " — skipping to be safe.");
                            shouldSkip = true;
                        }
                    }
                } catch (Exception e) {
                    DashboardManager.log("   ⚠️ Could not read email time: " + e.getMessage());
                }

                if (shouldSkip) continue;

                // Open the email
                row.click();
                gmailPage.waitForLoadState();
                gmailPage.waitForTimeout(2000);
                DashboardManager.log("   ✅ Email opened.");

                // Verify content and extract download link
                try {
                    String body = gmailPage.locator("div.a3s").first().innerText().trim();
                    DashboardManager.log("   -> Email body preview: "
                            + body.substring(0, Math.min(200, body.length())));

                    if (body.contains("Your Requested Report") || body.contains("Download Report")) {
                        DashboardManager.log("   ✅ Email content verified — Download Report link present.");
                    } else {
                        DashboardManager.log("   ❌ Email content mismatch.");
                    }

                    // Extract download link from anchor tag inside email body
                    try {
                        Locator linkLocator = gmailPage.locator("div.a3s a")
                                .filter(new Locator.FilterOptions().setHasText("Download Report"))
                                .first();
                        downloadLink = linkLocator.getAttribute("href");
                        if (downloadLink != null && !downloadLink.isEmpty()) {
                            DashboardManager.log("   ✅ Download link extracted: " + downloadLink);
                        } else {
                            DashboardManager.log("   ⚠️ Download link href is empty.");
                            downloadLink = "Not found";
                        }
                    } catch (Exception ex) {
                        DashboardManager.log("   ⚠️ Could not extract download link: " + ex.getMessage());
                    }

                } catch (Exception e) {
                    DashboardManager.log("   ⚠️ Could not read email body: " + e.getMessage());
                }

                emailFound = true;
                break;
            }

            if (emailFound) break;

            if (attempt < maxAttempts) {
                gmailPage.waitForTimeout(5000);
            }
        }

        if (!emailFound) {
            DashboardManager.log("   ❌ Report email NOT received within 3 minutes.");
        }

        return downloadLink;
    }

    // ──────────────────────────────────────────────────────────────
    // HELPER: Convert time string to minutes since midnight
    // Handles "2:56 PM", "04:13 PM", "12:05 AM" etc.
    // ──────────────────────────────────────────────────────────────

    private int convertToMinutes(String timeStr) {
        timeStr = timeStr.trim().toUpperCase(java.util.Locale.US);
        boolean isPM = timeStr.contains("PM");
        timeStr = timeStr.replace("AM", "").replace("PM", "").trim();
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0].trim());
        int minutes = Integer.parseInt(parts[1].trim());
        if (isPM && hours != 12) hours += 12;
        if (!isPM && hours == 12) hours = 0;
        return hours * 60 + minutes;
    }
}