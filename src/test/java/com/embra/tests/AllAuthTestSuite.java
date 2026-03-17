package com.embra.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Full Authentication Suite - Admin, Client, and Vendor")
@SelectClasses({
        AdminAuthTest.class,
        ClientAuthTest.class,
        VendorAuthTest.class
})
public class AllAuthTestSuite {
    // This class remains empty.
    // It only acts as a trigger to run the classes listed above.
}