package com.syncflow.syncflow_api;

import com.syncflow.SyncFlowApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context smoke test. Needs a reachable SQL Server and credentials, so it only runs when
 * SYNCFLOW_IT_DB=true (e.g. locally with config/application-local.yml in place).
 */
@SpringBootTest(classes = SyncFlowApplication.class)
@EnabledIfEnvironmentVariable(named = "SYNCFLOW_IT_DB", matches = "true")
class SyncflowApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
