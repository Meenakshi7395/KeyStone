package com.KeyStone.DeliveryService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context smoke test. Needs a running PostgreSQL (see README), so it is
 * disabled for plain `mvn test`; the unit tests in Service/ run without a DB.
 */
@SpringBootTest
@Disabled("Requires PostgreSQL — start the database and remove @Disabled to run")
class DeliveryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
