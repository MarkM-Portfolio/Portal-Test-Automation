/*
 ********************************************************************
 * Licensed Materials - Property of HCL                             *
 *                                                                  *
 * Copyright HCL Technologies Ltd. 2021. All Rights Reserved.       *
 *                                                                  *
 * Note to US Government Users Restricted Rights:                   *
 *                                                                  *
 * Use, duplication or disclosure restricted by GSA ADP Schedule    *
 ********************************************************************
 */

package runner.wcm.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import runner.client.IUTSuiteRunner;

@RunWith(IUTSuiteRunner.class)
@Suite.SuiteClasses({
   // Setup
   com.ibm.workplace.wcm.base.test.SetupRequiredUsersTest.class,
   com.ibm.workplace.wcm.base.test.SetupSharedLibraryTest.class,
   com.ibm.workplace.wcm.base.test.SetupRestSharedLibraryTest.class,
   com.ibm.workplace.wcm.base.test.VerifySharedLibraryTest.class,




})
public class WcmIUTSuite
{

}