package com.embra.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Full Onboarding & User Creation Suite")
@SelectClasses({
        ClientOnboardingTest.class,
        VendorOnboardingTest.class,
        AdminUserCreationTest.class
})
public class AllOnboardingTestSuite {
    // This class remains empty.
    // It acts as a container to trigger the three onboarding tests.
}