package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class ScheduleAssessmentPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // LOCATORS
    // ──────────────────────────────────────────────────────────────

    // Admin
    private final Locator requirementListingLink;
    private final Locator candidatesTab;
    private final Locator updateStatusDropdown;
    private final Locator submitStatusBtn;

    // Vendor
    private final Locator vendorProjectsTab;
    private final Locator selectTimeBtn;
    private final Locator submitTimeSlotsBtn;

    // Admin Schedule Modal
    private final Locator scheduleAssessmentCta;
    private final Locator timeSlotsDropdown;
    private final Locator finalScheduleBtn;

    public ScheduleAssessmentPage(Page page) {
        this.page = page;

        // Admin Navigation
        this.requirementListingLink = page.locator("a[href='/hiring-requests']");
        this.candidatesTab = page.getByRole(AriaRole.TAB).filter(new Locator.FilterOptions().setHasText("Candidates"));

        // Admin Status Update
        this.updateStatusDropdown = page.locator("button[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select an option"));
        this.submitStatusBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Submit"));

        // Vendor Navigation
        this.vendorProjectsTab = page.locator("a[href='/projects']");
        this.selectTimeBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Select Time"));
        this.submitTimeSlotsBtn = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Submit Time Slots"));

        // Admin Schedule Assessment
        this.scheduleAssessmentCta = page.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Schedule Assessment"));
        this.timeSlotsDropdown = page.locator("div[role='combobox']").filter(new Locator.FilterOptions().setHasText("Select time slots"));
        this.finalScheduleBtn = page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Schedule Assessment"));
    }

    // ──────────────────────────────────────────────────────────────
    // ADMIN ACTIONS
    // ──────────────────────────────────────────────────────────────

    public void navigateToRequirementListing() {
        DashboardManager.log("📂 Navigating to Requirement Listing (Admin)...");
        requirementListingLink.click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    public void openRequirement(String reqTitle) {
        DashboardManager.log("🔎 Searching for Requirement: " + reqTitle);
        page.getByText(reqTitle).first().click();
        page.waitForTimeout(2000);
    }

    public void clickCandidatesTab() {
        DashboardManager.log("👉 Clicking 'Candidates' Tab...");
        candidatesTab.click();
        page.waitForTimeout(1000);
    }

    public void openCandidateAndVerify(String candidateName, String vendorName) {
        DashboardManager.log("👤 Opening Candidate: " + candidateName + " (" + vendorName + ")");
        Locator row = page.locator("tr")
                .filter(new Locator.FilterOptions().setHasText(candidateName))
                .filter(new Locator.FilterOptions().setHasText(vendorName));

        row.locator("a").first().click();
        page.waitForTimeout(2000);

        Locator statusBadge = page.locator("div.text-white").filter(new Locator.FilterOptions().setHasText("Applied"));
        if (statusBadge.isVisible()) {
            DashboardManager.log("   ✅ Candidate Status: Applied");
        } else {
            DashboardManager.log("   ❌ Candidate Status NOT 'Applied'");
        }
    }

    public void adminUpdateStatusToAssessment() {
        DashboardManager.log("🔄 Admin: Updating Status to 'Schedule Assessment'...");
        updateStatusDropdown.click();
        page.getByRole(AriaRole.OPTION).filter(new Locator.FilterOptions().setHasText("Schedule Assessment")).click();
        submitStatusBtn.click();

        if (waitForToast("Status updated successfully!")) {
            DashboardManager.log("   ✅ Toast Verified: Status updated successfully!");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // VENDOR ACTIONS (UPDATED: 2 DAYS x 2 SLOTS = 4 SLOTS)
    // ──────────────────────────────────────────────────────────────

    public void vendorNavigateToProjects() {
        DashboardManager.log("\n--- 🏢 VENDOR PORTAL ---");
        DashboardManager.log("📂 Navigating to Projects...");
        vendorProjectsTab.click();
        page.waitForTimeout(2000);
    }

    public void vendorOpenProjectAndVerify(String reqTitle) {
        DashboardManager.log("🔎 Opening Project: " + reqTitle);
        Locator projectTitle = page.locator("h3").filter(new Locator.FilterOptions().setHasText(reqTitle));
        try {
            projectTitle.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            projectTitle.click();
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Specific H3 not found, trying generic text click...");
            page.getByText(reqTitle).first().click();
        }
        page.waitForTimeout(2000);
    }

    public void vendorOpenCandidateAndVerify(String candidateName) {
        DashboardManager.log("👤 Opening Candidate: " + candidateName);
        page.getByText(candidateName).first().click();
        page.waitForTimeout(2000);

        if (page.getByText("Assessment Ongoing").isVisible()) {
            DashboardManager.log("   ✅ Candidate Status: Assessment Ongoing");
        } else {
            DashboardManager.log("   ❌ Status mismatch (Expected: Assessment Ongoing)");
        }
    }

    public void vendorSelectTimeSlots() {
        DashboardManager.log("📅 Vendor: Selecting Time Slots...");
        selectTimeBtn.click();

        // Wait for Calendar Container
        Locator calendar = page.locator("div.flex.flex-wrap.gap-y-\\[9px\\]");
        try {
            calendar.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        } catch (Exception ignored) {
            DashboardManager.log("      ⚠️ Calendar container wait timed out, attempting to proceed...");
        }

        // Get all active dates (try standard active, then fallback to hover style)
        Locator activeDates = page.locator("div.w-11.h-11.rounded-full:not(.opacity-40):not(.cursor-not-allowed)");
        if (activeDates.count() < 2) {
            activeDates = page.locator("div.w-11.h-11.rounded-full.hover\\:bg-\\[var\\(--primary-700\\)\\]");
        }

        int totalAvailableDates = activeDates.count();
        DashboardManager.log("   -> Found " + totalAvailableDates + " active dates in calendar.");

        int successfulDays = 0;
        int requiredDays = 2;

        // Loop through all available dates until we find 2 valid ones
        for (int i = 0; i < totalAvailableDates; i++) {
            if (successfulDays >= requiredDays) break; // Stop if we have 2 days

            DashboardManager.log("   -> Checking Date Index: " + i);
            activeDates.nth(i).click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(1000); // Wait for slots to load

            // Attempt to select 2 slots on this specific day
            if (selectTwoTimeSlotsForDay()) {
                DashboardManager.log("      ✅ Successfully selected 2 slots for this day.");
                successfulDays++;
            } else {
                DashboardManager.log("      ⚠️ Not enough slots (need 2). Skipping this day...");
            }
        }

        if (successfulDays < requiredDays) {
            DashboardManager.log("❌ CRITICAL: Could not find 2 days with at least 2 slots each.");
            throw new RuntimeException("Test Failed: Vendor could not select required time slots.");
        }

        DashboardManager.log("   -> Submitting Time Slots...");
        submitTimeSlotsBtn.scrollIntoViewIfNeeded();
        submitTimeSlotsBtn.click(new Locator.ClickOptions().setForce(true));

        page.waitForTimeout(2000);
    }

    // Helper: Selects exactly 2 slots if available
    private boolean selectTwoTimeSlotsForDay() {
        Locator slots = page.locator("div.overflow-y-scroll button");
        try {
            slots.first().waitFor(new Locator.WaitForOptions().setTimeout(2000));
        } catch (Exception e) { return false; }

        if (slots.count() >= 2) {
            // Select Slot 1
            slots.nth(0).click();
            DashboardManager.log("         Selected: " + slots.nth(0).innerText());

            // Select Slot 2
            slots.nth(1).click();
            DashboardManager.log("         Selected: " + slots.nth(1).innerText());
            return true;
        }
        return false;
    }

    // ──────────────────────────────────────────────────────────────
    // ADMIN FINALIZE (FIXED: DATA VALIDATION & VERIFIED SELECTION)
    // ──────────────────────────────────────────────────────────────
    public void adminScheduleAssessmentAction() {
        DashboardManager.log("\n--- 👮 ADMIN PORTAL (Finalize) ---");
        DashboardManager.log("📅 Clicking 'Schedule Assessment' CTA...");
        scheduleAssessmentCta.click();

        // 1. Check for blocking errors
        Locator missingRoleError = page.locator("p.text-red-500").filter(new Locator.FilterOptions().setHasText("Please add Assessment Role ID"));
        try {
            if (missingRoleError.isVisible()) {
                throw new RuntimeException("Automation Stopped: Missing Assessment Role ID.");
            }
        } catch (Exception ignored) {}

        // 2. Open Dropdown & Capture Data
        DashboardManager.log("   -> Opening Time Slots Dropdown...");
        timeSlotsDropdown.click();

        Locator popover = page.locator("div.bg-popover.absolute.z-50");
        popover.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

        // Get list of slot texts
        Locator optionsLocator = popover.locator("div.relative.flex");
        List<String> slotTexts = optionsLocator.allInnerTexts();
        int count = slotTexts.size();
        DashboardManager.log("   -> Found " + count + " available slots: " + slotTexts);

        // 3. ROBUST CLICK SELECTION LOOP
        Locator searchInput = page.locator("input[placeholder='Search...']");

        if (count > 0) {
            for (String slotText : slotTexts) {
                String cleanText = slotText.trim();
                DashboardManager.log("      🔍 Processing: [" + cleanText + "]");

                // Step A: Ensure Dropdown is Open
                if (!popover.isVisible()) {
                    timeSlotsDropdown.click();
                    popover.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                }

                // Step B: Type Search Text
                searchInput.fill(cleanText);
                page.waitForTimeout(500); // Wait for React to filter the list

                // Step C: Pinpoint the Option & Click
                // After searching, there should be only one option visible.
                // We target the *visible* one specifically.
                Locator targetOption = popover.locator("div.relative.flex").first();

                if (targetOption.isVisible()) {
                    targetOption.hover(); // Hover first to trigger UI state
                    targetOption.click(); // Standard click
                    DashboardManager.log("         ✅ Clicked.");
                } else {
                    DashboardManager.log("         ⚠️ Option not visible after search!");
                }

                // Step D: Reset Search (CRITICAL)
                // We must clear the search bar manually so the list resets or allows the next search
                searchInput.fill("");
                page.waitForTimeout(200);
            }

            // 4. Force Validation (Blur)
            page.keyboard().press("Escape"); // Close dropdown
            page.locator("label").filter(new Locator.FilterOptions().setHasText("Assessment Timeslots")).click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(1000);

        } else {
            DashboardManager.log("   ⚠️ No time slots found in dropdown!");
        }

        // 5. Submit
        DashboardManager.log("   -> Clicking Final Schedule Button...");
        Locator submitBtn = page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Schedule Assessment"));
        submitBtn.scrollIntoViewIfNeeded();

        try {
            submitBtn.click();
        } catch (Exception e) {
            DashboardManager.log("      ⚠️ Standard click failed, attempting Force Click...");
            submitBtn.click(new Locator.ClickOptions().setForce(true));
        }

        // 6. Verify
        if (waitForToast("Assessment scheduled successfully!")) {
            DashboardManager.log("   ✅ Success Toast Verified.");
        } else {
            DashboardManager.log("   ❌ Success Toast NOT found.");
            Locator errorMsg = page.locator("p.text-xs.text-red-500");
            if (errorMsg.isVisible()) {
                DashboardManager.log("      ❌ VALIDATION ERROR: " + errorMsg.innerText());
            }
        }
    }

    public void captureAssessmentDetails() {
        DashboardManager.log("📋 Capturing Assessment Details...");
        Locator detailsCard = page.locator("div.bg-white.border.rounded-lg").filter(new Locator.FilterOptions().setHasText("Assessment Details"));

        if (detailsCard.isVisible()) {
            DashboardManager.log("------------------------------------------------");
            DashboardManager.log(detailsCard.innerText());
            DashboardManager.log("------------------------------------------------");
        } else {
            DashboardManager.log("   ⚠️ Assessment Details card not visible.");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🆕 ADMIN: UPLOAD ASSESSMENT RESULT (FIXED)
    // ──────────────────────────────────────────────────────────────

    public void adminUploadAssessmentResult(String dummyPdfPath) {
        DashboardManager.log("\n--- 📝 ADMIN: UPLOADING ASSESSMENT RESULT ---");

        // 1. Click 'Upload Assessment Result'
        Locator uploadBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("Upload Assessment Result"));
        uploadBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        uploadBtn.click();
        DashboardManager.log("   -> Opened Upload Modal.");

        // 2. Fill Score
        Locator scoreInput = page.locator("input[name='score']");
        scoreInput.fill("85");
        DashboardManager.log("   -> Filled Score: 85");

        // 3. Upload File
        Locator fileInput = page.locator("input[type='file']").last();
        fileInput.setInputFiles(java.nio.file.Paths.get(dummyPdfPath));
        DashboardManager.log("   -> Uploaded File: " + dummyPdfPath);

        // 4. Fill Feedback
        Locator feedbackInput = page.locator("textarea[name='feedback']");
        feedbackInput.fill("this is automated test feedback");
        DashboardManager.log("   -> Filled Feedback.");

        // 5. WAIT FOR ENABLE -> WAIT 1 SEC -> CLICK
        Locator submitBtn = page.locator("button.bg-green-600").filter(new Locator.FilterOptions().setHasText("Submit"));

        DashboardManager.log("   ⏳ Waiting for Submit button to enable (File uploading)...");
        try {
            long startTime = System.currentTimeMillis();
            long timeout = 20000; // 20s timeout

            while (!submitBtn.isEnabled()) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new RuntimeException("Timeout: Submit button did not enable within 20s.");
                }
                page.waitForTimeout(500);
            }

            DashboardManager.log("   ✅ Button is Enabled.");
            DashboardManager.log("   ⏳ Waiting 1 second before clicking...");
            page.waitForTimeout(1000);

            submitBtn.click();
            DashboardManager.log("   ✅ Clicked Submit.");

        } catch (Exception e) {
            DashboardManager.log("   ❌ Error interacting with Submit button: " + e.getMessage());
            throw e;
        }

        // 6. VERIFY SUCCESS TOAST
        DashboardManager.log("   ⏳ Waiting for Success Toast...");
        if (waitForToast("Assessment feedback submitted successfully!")) {
            DashboardManager.log("   ✅ Toast Verified.");
        } else {
            DashboardManager.log("   ⚠️ Toast missed or delayed. Checking dashboard...");
        }

        // 7. CAPTURE & PRINT RESULT CARD
        DashboardManager.log("   🔍 Capturing Assessment Result Details...");
        try {
            // Wait for refresh (Data to appear)
            page.waitForTimeout(2000);

            // Find the Header "Assessment Result"
            Locator resultHeader = page.locator("h5").filter(new Locator.FilterOptions().setHasText("Assessment Result"));
            resultHeader.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

            // Select the Parent Card container (The div wrapping the H5 and the content)
            Locator resultCard = resultHeader.locator("..");

            if (resultCard.isVisible()) {
                DashboardManager.log("\n------------------------------------------------");
                DashboardManager.log(resultCard.innerText()); // Prints Score, Date, Feedback, File exactly as seen
                DashboardManager.log("------------------------------------------------\n");
            } else {
                DashboardManager.log("   ❌ Header found, but card content is not visible.");
            }
        } catch (Exception e) {
            DashboardManager.log("   ❌ Failed to capture Assessment Result: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🆕 VENDOR: VERIFY STATUS (FIXED)
    // ──────────────────────────────────────────────────────────────

    public void verifyVendorAssessmentStatus(String vendorUrl, String email, String password, String reqName) {
        DashboardManager.log("\n--- 🏢 VENDOR: VERIFYING STATUS ---");

        // 1. Login
        page.navigate(vendorUrl);
        page.locator("input[name='email']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForTimeout(3000);

        // 2. Go to Projects
        Locator projectTab = page.locator("a[href='/projects']");
        projectTab.click();
        page.waitForTimeout(2000);

        // 3. Open Requirement (Direct Click on Name)
        DashboardManager.log("   -> Searching for Project: " + reqName);

        // We target the H3 text specifically based on your HTML snippet
        Locator projectTitle = page.locator("h3").filter(new Locator.FilterOptions().setHasText(reqName));

        try {
            projectTitle.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            projectTitle.click();
            DashboardManager.log("      ✅ Found & Clicked Project Name.");
        } catch (Exception e) {
            DashboardManager.log("      ❌ Could not find project with name: " + reqName);
            // Fallback: Click first project if specific one not found (for debugging)
            page.locator("h3").first().click();
        }

        page.waitForTimeout(2000);

        // 4. Verify Candidate 1 Listing Status
        DashboardManager.log("   -> Verifying Candidate 1 in Listing...");
        Locator candidateRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Candidate 1"));

        // Check for "Assessment Completed" status (Green)
        Locator completedStatus = candidateRow.locator("span.status-green-text");
        if (completedStatus.isVisible()) {
            DashboardManager.log("      ✅ Listing Status: [" + completedStatus.innerText() + "]");
        } else {
            // Fallback: Print whatever status is visible
            Locator anyStatus = candidateRow.locator("span[class*='status']");
            if (anyStatus.isVisible()) {
                DashboardManager.log("      ℹ️ Listing Status Found: [" + anyStatus.innerText() + "]");
            } else {
                DashboardManager.log("      ❌ No status text found in listing.");
            }
        }

        // 5. Open Candidate Details
        candidateRow.click();
        page.waitForTimeout(2000);

        // 6. Verify Assessment Details inside Candidate View
        DashboardManager.log("   -> Verifying Inside Candidate Details...");

        // Locate the Assessment Row
        Locator assessmentRow = page.locator("tr").filter(new Locator.FilterOptions().setHasText("Assessment"));

        // A. Verify Status "Assessment Completed"
        Locator detailStatus = assessmentRow.locator("span.status-green-text");
        if (detailStatus.isVisible() && detailStatus.innerText().contains("Assessment Completed")) {
            DashboardManager.log("      ✅ Status: Assessment Completed");
        } else {
            DashboardManager.log("      ❌ Status Mismatch. Row Text: " + assessmentRow.innerText());
        }

        // B. Verify Score "85"
        if (assessmentRow.filter(new Locator.FilterOptions().setHasText("85")).isVisible()) {
            DashboardManager.log("      ✅ Score: 85 is visible.");
        } else {
            DashboardManager.log("      ❌ Score 85 NOT found.");
        }

        // C. Verify "View" Link
        if (assessmentRow.locator("a:has-text('View')").isVisible()) {
            DashboardManager.log("      ✅ Report Link ('View') is present.");
        } else {
            DashboardManager.log("      ❌ Report Link missing.");
        }
    }


    private boolean waitForToast(String message) {
        try {
            page.getByText(message).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}