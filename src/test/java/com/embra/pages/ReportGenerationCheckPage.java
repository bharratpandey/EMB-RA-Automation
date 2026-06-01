package com.embra.pages;

import com.embra.utils.DashboardManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.*;

import java.util.Properties;

public class ReportGenerationCheckPage {

    private final Page page;

    private static final String ADMIN_URL    = "https://admin.embtalent.ai/login";
    private static final String EMAIL        = "bharat.pandey@emb.global";
    private static final String PASSWORD     = "Emb@1234";

    // ── IMAP credentials for email check ──────────────────────────
    // Use app password for bharat.pandey@emb.global from myaccount.google.com/apppasswords
    private static final String IMAP_EMAIL   = "bharat.pandey@emb.global";
   // private static final String IMAP_APP_PWD = "vgctlqqxazodwets"; // ← replace this

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
    // 5. CHECK EMAIL VIA IMAP — no browser session needed
    // Returns the download link found inside the email body
    // ──────────────────────────────────────────────────────────────

    public String openGmailAndVerifyReportEmail(Page gmailPage, String reportClickTime) {
        DashboardManager.log("\n--- 📧 CHECKING EMAIL VIA IMAP ---");
        DashboardManager.log("   -> Report was requested at: " + reportClickTime);

        String downloadLink = "Not found";

        // Convert click time to minutes for comparison
        int clickMinutes = -1;
        try {
            clickMinutes = convertToMinutes(reportClickTime);
            DashboardManager.log("   -> Click time in minutes: " + clickMinutes
                    + " (" + reportClickTime + ")");
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not convert click time: " + e.getMessage());
        }

        // Get app password from env or fallback
        // Get app password — from env var (CI) or .env file (local)
        String envPassword = System.getenv("IMAP_APP_PASSWORD");
        if (envPassword == null || envPassword.isEmpty()) {
            try {
                io.github.cdimascio.dotenv.Dotenv dotenv =
                        io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
                envPassword = dotenv.get("IMAP_APP_PASSWORD", "");
            } catch (Exception ignored) {}
        }
        String appPassword = envPassword;

        // IMAP connection properties
        Properties props = new Properties();
        props.put("mail.imap.host", "imap.gmail.com");
        props.put("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "imap.gmail.com");

        DashboardManager.log("   -> Connecting to IMAP...");

        Store store = null;
        Folder inbox = null;

        try {
            Session session = Session.getInstance(props);
            store = session.getStore("imap");
            store.connect("imap.gmail.com", IMAP_EMAIL, appPassword);
            DashboardManager.log("   ✅ IMAP connected.");

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            DashboardManager.log("   -> Waiting for report email (checking every 5s, max 3 min)...");

            int maxAttempts = 36;
            boolean emailFound = false;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                DashboardManager.log("   -> Attempt " + attempt + "/" + maxAttempts + " — checking inbox...");

                // Search for emails with matching subject
                SearchTerm subjectTerm = new SubjectTerm("EMBTalent: Your Requested Report");
                SearchTerm fromTerm = new FromStringTerm("team@embtalent.ai");
                SearchTerm combinedTerm = new AndTerm(subjectTerm, fromTerm);

                Message[] messages = inbox.search(combinedTerm);
                DashboardManager.log("   -> Found " + messages.length + " matching email(s).");

                // Check from newest to oldest
                for (int i = messages.length - 1; i >= 0; i--) {
                    Message msg = messages[i];

                    // Get received date
                    java.util.Date receivedDate = msg.getReceivedDate();
                    if (receivedDate == null) receivedDate = msg.getSentDate();

                    if (receivedDate != null && clickMinutes >= 0) {
                        // Convert received time to minutes
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(receivedDate);
                        int emailHours = cal.get(java.util.Calendar.HOUR_OF_DAY);
                        int emailMins = cal.get(java.util.Calendar.MINUTE);
                        int emailMinutes = emailHours * 60 + emailMins;

                        String emailTimeStr = String.format("%02d:%02d", emailHours, emailMins);
                        DashboardManager.log("   -> Email received at: " + emailTimeStr
                                + " (" + emailMinutes + " min), Click at: " + clickMinutes + " min");

                        if (emailMinutes < clickMinutes) {
                            DashboardManager.log("   ⏭️ Email is before click time — skipping.");
                            continue;
                        }
                        DashboardManager.log("   ✅ Email time is at or after click time — valid!");
                    }

                    // Extract body and download link
                    DashboardManager.log("   -> Reading email body...");
                    String body = getEmailBody(msg);

                    if (body.contains("Your Requested Report") || body.contains("Download Report")) {
                        DashboardManager.log("   ✅ Email content verified — Download Report present.");
                    }

                    // Extract download link using regex
                    java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile(
                            "href=[\"'](https?://[^\"']+)[\"']",
                            java.util.regex.Pattern.CASE_INSENSITIVE);
                    java.util.regex.Matcher matcher = linkPattern.matcher(body);

                    while (matcher.find()) {
                        String href = matcher.group(1);
                        // Look for blob storage or download links
                        if (href.contains("blob") || href.contains("download") || href.contains("report")) {
                            downloadLink = href;
                            DashboardManager.log("   ✅ Download link extracted: " + downloadLink);
                            break;
                        }
                    }

                    emailFound = true;
                    break;
                }

                if (emailFound) break;

                if (attempt < maxAttempts) {
                    Thread.sleep(5000);
                }
            }

            if (!emailFound) {
                DashboardManager.log("   ❌ Report email NOT received within 3 minutes.");
            }

        } catch (Exception e) {
            DashboardManager.log("   ❌ IMAP error: " + e.getMessage());
        } finally {
            // Close connections
            try { if (inbox != null) inbox.close(false); } catch (Exception ignored) {}
            try { if (store != null) store.close(); } catch (Exception ignored) {}
        }

        return downloadLink;
    }

    // ──────────────────────────────────────────────────────────────
    // HELPER: Extract text body from email message
    // ──────────────────────────────────────────────────────────────

    private String getEmailBody(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof MimeMultipart) {
                return getTextFromMimeMultipart((MimeMultipart) content);
            }
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not read email body: " + e.getMessage());
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) {
        StringBuilder result = new StringBuilder();
        try {
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    result.append(bodyPart.getContent());
                } else if (bodyPart.isMimeType("text/html")) {
                    result.append(bodyPart.getContent());
                } else if (bodyPart.getContent() instanceof MimeMultipart) {
                    result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
                }
            }
        } catch (Exception e) {
            DashboardManager.log("   ⚠️ Could not parse multipart: " + e.getMessage());
        }
        return result.toString();
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