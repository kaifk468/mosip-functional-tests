package io.mosip.testrig.apirig.testscripts;

import static io.mosip.testrig.apirig.service.BaseTestCase.getRequestJson;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.mosip.testrig.apirig.admin.fw.util.AdminTestException;
import io.mosip.testrig.apirig.admin.fw.util.AdminTestUtil;
import io.mosip.testrig.apirig.admin.fw.util.TestCaseDTO;
import io.mosip.testrig.apirig.authentication.fw.dto.OutputValidationDto;
import io.mosip.testrig.apirig.authentication.fw.util.AuthenticationTestException;
import io.mosip.testrig.apirig.authentication.fw.util.OutputValidationUtil;
import io.mosip.testrig.apirig.authentication.fw.util.ReportUtil;
import io.mosip.testrig.apirig.ida.certificate.PartnerRegistration;
import io.mosip.testrig.apirig.kernel.util.ConfigManager;
import io.mosip.testrig.apirig.service.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.restassured.response.Response;

public class BioAuth extends AdminTestUtil implements ITest {
	private static final Logger logger = Logger.getLogger(BioAuth.class);
	protected String testCaseName = "";
	public Response response = null;
	public boolean isInternal = false;
	
	@BeforeClass
	public static void setLogLevel() {
		if (ConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		isInternal = Boolean.parseBoolean(context.getCurrentXmlTest().getLocalParameters().get("isInternal"));
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}
	

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {
		testCaseName = testCaseDTO.getTestCaseName();
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException("Target env health check failed " + HealthChecker.healthCheckFailureMapS);
		}
		if (testCaseDTO.getTestCaseName().contains("uin") || testCaseDTO.getTestCaseName().contains("UIN")) {
			if (!BaseTestCase.getSupportedIdTypesValueFromActuator().contains("UIN")
					&& !BaseTestCase.getSupportedIdTypesValueFromActuator().contains("uin")) {
				throw new SkipException("Idtype UIN is not supported. Hence skipping the testcase");
			}
		}
		
		if (testCaseDTO.getTestCaseName().contains("vid") || testCaseDTO.getTestCaseName().contains("VID")) {
			if (!BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
					&& !BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
				throw new SkipException("Idtype VID is not supported. Hence skipping the testcase");
			}
		}
			
		if (testCaseDTO.getEndPoint().contains("$PartnerKeyURL$")) {
			testCaseDTO.setEndPoint(
					testCaseDTO.getEndPoint().replace("$PartnerKeyURL$", PartnerRegistration.partnerKeyUrl));
		}
		
		if (testCaseDTO.getEndPoint().contains("$KycPartnerKeyURL$")) {
			testCaseDTO.setEndPoint(
					testCaseDTO.getEndPoint().replace("$KycPartnerKeyURL$", PartnerRegistration.ekycPartnerKeyUrl));
		}
		
		if (testCaseDTO.getEndPoint().contains("$PartnerName$")) {
			testCaseDTO.setEndPoint(testCaseDTO.getEndPoint().replace("$PartnerName$", PartnerRegistration.partnerId));
		}
		
		if (testCaseDTO.getEndPoint().contains("$KycPartnerName$")) {
			testCaseDTO.setEndPoint(testCaseDTO.getEndPoint().replace("$KycPartnerName$", PartnerRegistration.ekycPartnerId));
		}
		String request = testCaseDTO.getInput();
		request = buildIdentityRequest(request);

		String inputJSON = getJsonFromTemplate(request, testCaseDTO.getInputTemplate());

		String url = ConfigManager.getAuthDemoServiceUrl();

		response = postWithBodyAndCookie(url + testCaseDTO.getEndPoint(), inputJSON, COOKIENAME, testCaseDTO.getRole(),
				testCaseDTO.getTestCaseName());

		String ActualOPJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());

		if (testCaseDTO.getTestCaseName().contains("uin") || testCaseDTO.getTestCaseName().contains("UIN")) {
			if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("UIN")
					|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("uin")) {
				ActualOPJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());
			} else {
				if (testCaseDTO.getTestCaseName().contains("auth_EkycBio")) {
					ActualOPJson = AdminTestUtil.getRequestJson("config/errorUINKyc.json").toString();
				} else {
					ActualOPJson = AdminTestUtil.getRequestJson("config/errorUIN.json").toString();
				}

			}
		} else {
			if (testCaseDTO.getTestCaseName().contains("vid") || testCaseDTO.getTestCaseName().contains("VID")) {
				if (BaseTestCase.getSupportedIdTypesValueFromActuator().contains("VID")
						|| BaseTestCase.getSupportedIdTypesValueFromActuator().contains("vid")) {
					ActualOPJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());
				} else {
					if (testCaseDTO.getTestCaseName().contains("auth_EkycBio")) {
						ActualOPJson = AdminTestUtil.getRequestJson("config/errorUINKyc.json").toString();
					} else {
						ActualOPJson = AdminTestUtil.getRequestJson("config/errorUIN.json").toString();
					}

				}
			}
		}

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil
				.doJsonOutputValidation(response.asString(), ActualOPJson, testCaseDTO.isCheckErrorsOnlyInResponse());
		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid))
			throw new AdminTestException("Failed at output validation");

		if (testCaseName.toLowerCase().contains("kyc")) {
			JSONObject resJsonObject = new JSONObject(response.asString());
			String res = "";
			try {
				res = resJsonObject.get("response").toString();
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
		}


	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}

	}

	@AfterClass
	public static void authTestTearDown() {
	}
}
