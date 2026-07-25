package com.travel.flightdeals.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void loadsDefaultsFromProperties() {
        Properties props = new Properties();
        props.setProperty("amadeus.api.key", "test-key");
        props.setProperty("amadeus.api.secret", "test-secret");

        AppConfig config = AppConfig.fromProperties(props);

        assertEquals(20000, config.maxPriceInr());
        assertEquals(7, config.originAirports().size());
        assertEquals("DEL", config.originAirports().get(0));
    }
}
