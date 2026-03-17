package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import com.embra.utils.DashboardManager;

public class CreateRequirementPage {

    private final Page page;

    // ──────────────────────────────────────────────────────────────
    // STATIC LOCATORS
    // ──────────────────────────────────────────────────────────────
    private final Locator addNewRequirementBtn;
    private final Locator createRequirementBtn;

    public CreateRequirementPage(Page page) {
        this.page = page;

        this.addNewRequirementBtn = page.getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText("Add New Requirement"));

        this.createRequirementBtn = page.getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText("Create Requirement"));
    }

    // ──────────────────────────────────────────────────────────────
    // MAIN METHOD
    // ──────────────────────────────────────────────────────────────
    public boolean createMultipleRequirements(List<RequirementData> requirements, String successToast) {
        if (requirements == null || requirements.isEmpty()) {
            DashboardManager.log("❌ No requirements provided to create.");
            return false;
        }

        int total = requirements.size();
        DashboardManager.log("=================================================");
        DashboardManager.log("🚀 STARTING BATCH CREATION: " + total + " Requirements");
        DashboardManager.log("=================================================");

        try {
            for (int i = 0; i < total; i++) {
                DashboardManager.log("\n-------------------------------------------------");
                DashboardManager.log("🔄 Processing Requirement #" + (i + 1));
                DashboardManager.log("-------------------------------------------------");

                page.reload();

                if (i > 0) {
                    DashboardManager.log("   Clicking 'Add New Requirement' button...");
                    scrollToElement(addNewRequirementBtn);
                    safeClick(addNewRequirementBtn);

                    DashboardManager.log("   Waiting for new form block...");
                    page.locator("input[name='title']").nth(i)
                            .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
                    DashboardManager.log("   ✅ New form block visible.");
                }

                Locator currentScope = (i == 0) ? page.locator("body") : getLatestRequirementBlock();

                boolean ok = fillRequirementBlock(requirements.get(i), currentScope, i);
                if (!ok) {
                    DashboardManager.log("❌ Failed to fill Requirement #" + (i + 1));
                    return false;
                }
                DashboardManager.log("✅ Successfully filled Requirement #" + (i + 1));
            }

            DashboardManager.log("\n-------------------------------------------------");
            DashboardManager.log("🏁 All blocks filled. Clicking final 'Create Requirement'...");
            scrollToElement(createRequirementBtn);
            safeClick(createRequirementBtn);

            DashboardManager.log("   Waiting for success toast: '" + successToast + "'...");
            boolean success = waitForToast(successToast);

            if(success) {
                DashboardManager.log("✅ SUCCESS: Final toast validation passed.");
            } else {
                DashboardManager.log("❌ FAILURE: Final toast validation failed.");
            }
            return success;

        } catch (Exception e) {
            System.err.println("❌ EXCEPTION in createMultipleRequirements: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot("multi-create-error");
            return false;
        }
    }

    private boolean fillRequirementBlock(RequirementData data, Locator base, int index) {
        try {
            long uniqueId = Instant.now().toEpochMilli();
            String title = "ReqTest-" + uniqueId;


            DashboardManager.log("   [Block " + index + "] Filling Basic Details...");
            base.locator("input[name='title']").fill(title);
            base.locator("input[name='clientName']").fill("Client-Automated");
            base.locator("input[name='clientEmail']").fill("autoTest@yopmail.com");
            DashboardManager.log("      -> Title set to: " + title);

            DashboardManager.log("   [Block " + index + "] Selecting Domain: Frontend Engineer");
            selectFromDropdown(base.getByRole(AriaRole.COMBOBOX)
                    .filter(new Locator.FilterOptions().setHasText("Select domain")), "Frontend Engineer");

            DashboardManager.log("   [Block " + index + "] Filling Experience/Resources...");
            base.locator("input[name='numberOfPeople']").fill("2");
            base.locator("input[name='min_experience']").fill("3");
            base.locator("input[name='experience']").fill("5");

            // LOGIC FOR ENGAGEMENT TYPES
            DashboardManager.log("   [Block " + index + "] Setting Engagement Type: " + data.engagementType);
            Locator engTypeTrigger = base.getByRole(AriaRole.COMBOBOX)
                    .filter(new Locator.FilterOptions().setHasText("Engagement Type"));

            selectFromDropdown(engTypeTrigger, data.engagementType);

            if (data.engagementType.equalsIgnoreCase("Contractual")) {
                DashboardManager.log("      -> Filling fields for Contractual...");
                base.locator("input[name='duration']").fill("9");

                selectFromDropdown(base.getByRole(AriaRole.COMBOBOX)
                        .filter(new Locator.FilterOptions().setHasText("Select budget type")), "Monthly");

                selectFromDropdown(base.getByRole(AriaRole.COMBOBOX)
                        .filter(new Locator.FilterOptions().setHasText("Select currency")), "India (₹)");

                base.locator("input[name='budgetMin']").fill("1200000");
                base.locator("input[name='budgetMax']").fill("1500000");

            } else if (data.engagementType.equalsIgnoreCase("Contract To Hire")) {
                DashboardManager.log("      -> Filling fields for Contract To Hire...");
                base.locator("input[name='duration']").fill("9");

                selectFromDropdown(base.getByRole(AriaRole.COMBOBOX)
                        .filter(new Locator.FilterOptions().setHasText("Select currency")), "India (₹)");

                base.locator("input[name='budgetMin']").fill("1200000");
                base.locator("input[name='budgetMax']").fill("1500000");

            } else { // Full Time
                DashboardManager.log("      -> Filling fields for Full Time...");
                selectFromDropdown(base.getByRole(AriaRole.COMBOBOX)
                        .filter(new Locator.FilterOptions().setHasText("Select currency")), "India (₹)");

                base.locator("input[name='budgetMin']").fill("1200000");
                base.locator("input[name='budgetMax']").fill("1500000");
            }

            page.waitForTimeout(500);

            // UPDATED LOGIC FOR ENGAGEMENT MODE
            DashboardManager.log("   [Block " + index + "] Setting Mode: " + data.engagementMode);
            Locator mode = base.getByRole(AriaRole.COMBOBOX)
                    .filter(new Locator.FilterOptions().setHasText("Select mode of engagement"));
            selectFromDropdown(mode, data.engagementMode);

            // Onsite and Hybrid share the same flow (Location selection)
            if (data.engagementMode.equalsIgnoreCase("Onsite") || data.engagementMode.equalsIgnoreCase("Hybrid")) {
                DashboardManager.log("   [Block " + index + "] Triggering Location Selection...");
                selectServiceableLocation(base);
            } else {
                DashboardManager.log("   [Block " + index + "] Triggering Timezone Selection...");
                selectTimezone(base, "India");
            }

            DashboardManager.log("   [Block " + index + "] Uploading JD...");
            uploadJD(base, data.jdFilePath);

            DashboardManager.log("   [Block " + index + "] Adding Skills: " + data.primarySkill + ", " + data.preferredSkill);
            addSkills(base, data.primarySkill, data.preferredSkill);

            base.locator("textarea[name='description']").fill("Automated description.");

            DashboardManager.log("   [Block " + index + "] Validating Role ID...");
            String roleRes = fillAndValidateRoleId(base.locator("input[name='role_id']"), data.roleId, base);
            DashboardManager.log("      -> Validation Result: " + roleRes);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Block fill failed for index " + index + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Locator getLatestRequirementBlock() {
        Locator blocks = page.locator("div[data-req-index]");
        if (blocks.count() == 0) {
            System.err.println("❌ No requirement blocks found in DOM.");
            throw new RuntimeException("No requirement blocks found after 'Add New Requirement'");
        }
        return blocks.nth(blocks.count() - 1);
    }

    // ──────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ──────────────────────────────────────────────────────────────

    private void selectServiceableLocation(Locator scope) {
        DashboardManager.log("      Opening Location Selection Modal...");

        Locator addBtn = scope.locator("button").filter(new Locator.FilterOptions().setHasText("Add More"));
        scrollToElement(addBtn);
        safeClick(addBtn);

        Locator dialog = page.locator("div[role='dialog'][data-state='open']");
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(8000));
        DashboardManager.log("      ✅ Dialog Visible.");

        dialog.locator("button[role='tab']").filter(new Locator.FilterOptions().setHasText("All")).click();
        DashboardManager.log("      -> Switched to 'All' tab.");

        DashboardManager.log("      -> Searching for 'Delhi'...");
        Locator searchInput = dialog.locator("input[name='search']");
        searchInput.fill("Delhi");
        page.waitForTimeout(1500);

        Locator exactCheckbox = dialog.locator("xpath=//span[text()='Delhi, India']/..//button[@role='checkbox']");

        if (exactCheckbox.isVisible()) {
            exactCheckbox.click();
            DashboardManager.log("      ✅ Checked 'Delhi, India' (XPath match).");
        } else {
            DashboardManager.log("      ⚠️ XPath match failed. Trying fallback container match...");
            Locator row = dialog.locator("div.flex.items-center.justify-between")
                    .filter(new Locator.FilterOptions().setHas(page.getByText("Delhi, India", new Page.GetByTextOptions().setExact(true))));

            if (row.count() > 0) {
                row.locator("button[role='checkbox']").click();
                DashboardManager.log("      ✅ Checked 'Delhi, India' (Container match).");
            } else {
                DashboardManager.log("      ❌ ERROR: 'Delhi, India' NOT found in list.");
            }
        }

        dialog.locator("button").filter(new Locator.FilterOptions().setHasText("Save Selection")).click();
        DashboardManager.log("      -> Clicked Save Selection.");

        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(10000));
        DashboardManager.log("      ✅ Location Modal Closed.");
    }

    private void selectTimezone(Locator scope, String value) {
        DashboardManager.log("      Selecting Timezone: " + value);

        Locator trigger = scope.locator("div[role='combobox']")
                .filter(new Locator.FilterOptions().setHasText("Select Serviceable Timezone"));
        scrollToElement(trigger);
        safeClick(trigger);

        Locator searchInput = page.locator("input[placeholder='Search...']").first();
        searchInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

        searchInput.fill(value);
        page.waitForTimeout(1000);

        Locator option = page.locator("div.bg-popover").locator("span")
                .getByText(value, new Locator.GetByTextOptions().setExact(true));

        if (option.count() > 0) {
            option.first().click();
            DashboardManager.log("      ✅ Selected '" + value + "'");
        } else {
            DashboardManager.log("      ⚠️ Timezone '" + value + "' not found via exact match. Clicking first visible.");
            page.locator("div.bg-popover div.relative.flex").first().click();
        }
    }

    private void uploadJD(Locator scope, String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            Path p = Paths.get(filePath).toAbsolutePath();
            if (!Files.exists(p)) {
                DashboardManager.log("❌ ERROR: JD File missing at " + p);
                return;
            }

            Locator fileInput = scope.locator("input[type='file']");
            if (fileInput.count() > 1) {
                fileInput.last().setInputFiles(p);
            } else {
                fileInput.setInputFiles(p);
            }
            DashboardManager.log("      -> JD Uploaded. Waiting for extraction toast...");

            page.getByText("JD data extracted successfully", new Page.GetByTextOptions().setExact(false))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(30000));

            DashboardManager.log("      ✅ JD Extraction Verified.");

        } catch (Exception e) {
            DashboardManager.log("❌ JD Upload failed: " + e.getMessage());
        }
    }

    private void addSkills(Locator scope, String primary, String secondary) {
        Locator addButtons = scope.getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Add$")));

        if (primary != null && !primary.isBlank()) {
            scope.locator("input[placeholder*='primary skill']").fill(primary);
            if (addButtons.count() > 0) {
                addButtons.first().click();
                DashboardManager.log("      -> Added Primary: " + primary);
            }
        }
        if (secondary != null && !secondary.isBlank()) {
            scope.locator("input[placeholder*='secondary skill']").fill(secondary);
            if (addButtons.count() > 1) {
                addButtons.nth(1).click();
                DashboardManager.log("      -> Added Secondary: " + secondary);
            }
        }
    }

    private void selectFromDropdown(Locator trigger, String optionText) {
        trigger.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        safeClick(trigger);
        page.waitForTimeout(300);

        Locator option = page.getByRole(AriaRole.OPTION)
                .getByText(optionText, new Locator.GetByTextOptions().setExact(true));

        if (option.count() == 0) {
            option = page.locator("div[role='option']").filter(new Locator.FilterOptions().setHasText(optionText));
        }

        if (option.count() > 0) {
            option.first().click();
            DashboardManager.log("      -> Selected Dropdown: " + optionText);
        } else {
            DashboardManager.log("      ⚠️ Option '" + optionText + "' not found. Closing dropdown.");
            page.keyboard().press("Escape");
        }
    }

    private String fillAndValidateRoleId(Locator input, String id, Locator scope) {
        if (id == null || id.isEmpty()) return "skipped";
        input.fill(id);
        Locator valBtn = scope.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText("Validate"));
        safeClick(valBtn);

        DashboardManager.log("      ⏳ Waiting 2 seconds for validation processing...");
        page.waitForTimeout(2000);

        return waitForRoleValidationResult(scope);
    }

    private String waitForRoleValidationResult(Locator scope) {
        try {
            page.waitForSelector("li[data-sonner-toast]", new Page.WaitForSelectorOptions().setTimeout(5000));
            return "validated";
        } catch (Exception e) {
            return "timeout";
        }
    }

    private boolean waitForToast(String text) {
        try {
            page.getByText(text, new Page.GetByTextOptions().setExact(false))
                    .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(20000));
            return true;
        } catch (Exception e) {
            DashboardManager.log("❌ Success Toast '" + text + "' not found.");
            return false;
        }
    }

    private void safeClick(Locator loc) {
        loc.scrollIntoViewIfNeeded();
        try {
            loc.click();
        } catch (Exception e) {
            try {
                loc.click(new Locator.ClickOptions().setForce(true));
            } catch (Exception e2) {
                page.evaluate("el => el.click()", loc.elementHandle());
            }
        }
    }

    private void scrollToElement(Locator loc) {
        loc.scrollIntoViewIfNeeded();
        page.waitForTimeout(300);
    }

    private void takeScreenshot(String name) {
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots/" + name + ".png")));
            DashboardManager.log("📸 Screenshot saved: " + name);
        } catch (Exception ignored) { }
    }

    // --- UPDATED DATA CLASS ---
    public static class RequirementData {
        public String engagementType;

        public String engagementMode; // Changed from boolean onsite to String
        public String primarySkill;
        public String preferredSkill;
        public String roleId;
        public String jdFilePath;

        public RequirementData(String eType, String eMode, String p, String s, String r, String j) {
            this.engagementType = eType;
            this.engagementMode = eMode;
            this.primarySkill = p;
            this.preferredSkill = s;
            this.roleId = r;
            this.jdFilePath = j;
        }
    }

    public void verifyRequirementStatuses() {
        DashboardManager.log("\n=================================================");
        DashboardManager.log("🔍 VERIFYING STATUS OF CREATED REQUIREMENTS");
        DashboardManager.log("=================================================");

        try {
            page.waitForTimeout(3000);
            Locator statusBadges = page.locator("td div.inline-flex.items-center.rounded-full");
            statusBadges.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

            int count = statusBadges.count();
            int limit = Math.min(count, 1);

            if (count == 0) {
                DashboardManager.log("❌ No requirement statuses found in the table!");
                return;
            }

            for (int i = 0; i < limit; i++) {
                String statusText = statusBadges.nth(i).innerText();
                DashboardManager.log("✅ Requirement " + (i + 1) + " Status: [" + statusText + "]");
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to capture requirement statuses: " + e.getMessage());
            takeScreenshot("status-verification-error");
        }
    }
}