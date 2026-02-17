package com.embra.pages;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;          // ← important import
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;

public class RequirementListingPage {

    private final Page page;
    private final Locator newRequirementCta;

    public RequirementListingPage(Page page) {
        this.page = page;

        // Correct way in Playwright Java – use AriaRole enum
        this.newRequirementCta = page.getByRole(AriaRole.LINK)
                .filter(new Locator.FilterOptions()
                        .setHasText("New Requirement"))
                .first();

        // Alternative (more stable in many cases):
        // this.newRequirementCta = page.locator("a[href*='/client']").getByText("New Requirement");
    }

    public boolean clickNewRequirement() {
        try {
            // Give dashboard time to become interactive
            page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(25000));

            newRequirementCta.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(20000));

            safeClick(newRequirementCta);

            // Strong assertion: wait for create requirement page content
            page.waitForSelector("input[name='title']",
                    new Page.WaitForSelectorOptions().setTimeout(20000));

            return true;
        } catch (Exception e) {
            System.err.println("Navigation failed: " + e.getMessage());
            takeScreenshot("navigation_failure");
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