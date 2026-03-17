package com.embra.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class VendorOnboardingPage {
    private final Page page;

    // --- Step 1: About Organization ---
    private final Locator aboutOrgHeading;
    private final Locator agencyNameInput;
    private final Locator websiteInput;
    private final Locator countryOriginDropdown;
    private final Locator phoneInput;
    private final Locator nextButton;

    // --- Step 2: Organization Details ---
    private final Locator orgDetailsHeading;
    private final Locator orgSizeDropdown;
    private final Locator turnoverDropdown;
    private final Locator hiringServicesAllBtn;
    private final Locator engagementModeAllBtn;
    private final Locator addMoreLocationBtn;
    private final Locator locationSearchInput;
    private final Locator saveSelectionBtn;
    private final Locator timezoneDropdown;
    private final Locator popupSearchInput;

    // --- Step 3: Capabilities ---
    private final Locator capabilitiesHeading;
    private final Locator selectCapabilitiesBtn;
    private final Locator skillSearchInput;
    private final Locator completeSetupBtn;

    // --- Step 4: Final Address ---
    private final Locator addressLine1Input;
    private final Locator countryInput;
    private final Locator zipCodeInput;
    private final Locator autoFillToast;
    private final Locator finalSubmitBtn;
    private final Locator thankYouHeading;

    public VendorOnboardingPage(Page page) {
        this.page = page;

        // Step 1
        this.aboutOrgHeading = page.locator("h2:has-text('About your organization')");
        this.agencyNameInput = page.locator("input[name='agencyName']");
        this.websiteInput = page.locator("input[name='websiteLink']");
        this.countryOriginDropdown = page.getByRole(AriaRole.COMBOBOX).filter(new Locator.FilterOptions().setHasText("Select your country of origin"));
        this.phoneInput = page.locator("input[name='contactNumber']");
        this.nextButton = page.locator("button[type='submit']:has-text('Next')");

        // Step 2
        this.orgDetailsHeading = page.locator("h2:has-text('Organization Details')");
        this.orgSizeDropdown = page.getByRole(AriaRole.COMBOBOX).filter(new Locator.FilterOptions().setHasText("Select your organization size"));
        this.turnoverDropdown = page.getByRole(AriaRole.COMBOBOX).filter(new Locator.FilterOptions().setHasText("Select your organization annual turnover"));
        this.hiringServicesAllBtn = page.locator("button:has-text('All')").first();
        this.engagementModeAllBtn = page.locator("button:has-text('All')").last();
        this.addMoreLocationBtn = page.locator("button:has-text('Add More')");
        this.locationSearchInput = page.locator("input[placeholder='Try entering a city or state']");
        this.saveSelectionBtn = page.locator("button:has-text('Save Selection')");
        this.timezoneDropdown = page.locator("div[role='combobox']:has-text('Select Timezones')");
        this.popupSearchInput = page.locator("input[placeholder='Search...']");

        // Step 3
        this.capabilitiesHeading = page.locator("h2:has-text('Your Organization Capabilities')");
        this.selectCapabilitiesBtn = page.locator("button:has-text('Select Capabilities')");
        this.skillSearchInput = page.locator("input[name='search']");
        this.completeSetupBtn = page.locator("button:has-text('Complete Setup')");

        // Step 4
        this.addressLine1Input = page.locator("input[name='addressLine1']");
        this.countryInput = page.locator("input[placeholder='Eg: India, UK']");
        this.zipCodeInput = page.locator("input[name='zipCode']");
        this.autoFillToast = page.locator("span:has-text('Auto-filled state and city information')");
        this.finalSubmitBtn = page.locator("button[type='submit']:has-text('Submit')");
        this.thankYouHeading = page.locator("h1:has-text('Thank you for submitting your details')");
    }

    // Inside com.embra.pages.VendorOnboardingPage

    public void selectSkill(String skillName) {
        // 1. Clear and fill the search input
        skillSearchInput.clear();
        skillSearchInput.fill(skillName);
        page.waitForTimeout(1000); // Give time for the list to filter

        // 2. Locate the EXACT text span first
        Locator exactText = page.getByText(skillName, new Page.GetByTextOptions().setExact(true));

        // 3. Find the specific row that contains that exact text AND a checkbox
        // The container class 'flex.w-full.flex-row' matches your reference logic
        Locator row = page.locator("div.flex.w-full.flex-row")
                .filter(new Locator.FilterOptions().setHas(exactText))
                .filter(new Locator.FilterOptions().setHas(page.locator("button[role='checkbox']")));

        if (row.count() > 0) {
            Locator checkbox = row.first().locator("button[role='checkbox']");

            // 4. Only click if it's not already checked (data-state check)
            if (!"checked".equals(checkbox.getAttribute("data-state"))) {
                checkbox.click();
            }
        } else {
            // Fallback: If exact match isn't found in a row, try clicking the first available checkbox
            // This handles cases where the DOM structure might be slightly different
            page.locator("button[role='checkbox']").first().click();
        }

        // Brief pause before searching for the next skill
        page.waitForTimeout(300);
    }

    public void selectTimezone(String tz) {
        popupSearchInput.fill(tz);
        page.locator("div.relative.flex.cursor-pointer").filter(new Locator.FilterOptions().setHasText(tz)).click();
    }



    // Getters
    public Locator getAboutOrgHeading() { return aboutOrgHeading; }
    public Locator getOrgDetailsHeading() { return orgDetailsHeading; }
    public Locator getCapabilitiesHeading() { return capabilitiesHeading; }
    public Locator getThankYouHeading() { return thankYouHeading; }
    public Locator getAgencyNameInput() { return agencyNameInput; }
    public Locator getWebsiteInput() { return websiteInput; }
    public Locator getCountryOriginDropdown() { return countryOriginDropdown; }
    public Locator getPhoneInput() { return phoneInput; }
    public Locator getNextButton() { return nextButton; }
    public Locator getOrgSizeDropdown() { return orgSizeDropdown; }
    public Locator getTurnoverDropdown() { return turnoverDropdown; }
    public Locator getHiringServicesAllBtn() { return hiringServicesAllBtn; }
    public Locator getEngagementModeAllBtn() { return engagementModeAllBtn; }
    public Locator getAddMoreLocationBtn() { return addMoreLocationBtn; }
    public Locator getLocationSearchInput() { return locationSearchInput; }
    public Locator getSaveSelectionBtn() { return saveSelectionBtn; }
    public Locator getTimezoneDropdown() { return timezoneDropdown; }
    public Locator getSelectCapabilitiesBtn() { return selectCapabilitiesBtn; }
    public Locator getCompleteSetupBtn() { return completeSetupBtn; }
    public Locator getAddressLine1Input() { return addressLine1Input; }
    public Locator getCountryInput() { return countryInput; }
    public Locator getZipCodeInput() { return zipCodeInput; }
    public Locator getAutoFillToast() { return autoFillToast; }
    public Locator getFinalSubmitBtn() { return finalSubmitBtn; }
}