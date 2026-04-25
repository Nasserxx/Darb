package com.darb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.darb.support.PostgresIntegrationTestBase;

@SpringBootTest
@ActiveProfiles("test")
class DarbApplicationTests extends PostgresIntegrationTestBase {

	@Test
	void contextLoads() {
	}

}
