package com.travel.flightdeals.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

class CurrencyConverterTest {

    @Test
    void convertsInrAsIdentity() {
        CurrencyConverter converter = new CurrencyConverter(HttpClient.newHttpClient(), "https://test.api.amadeus.com");
        assertEquals(15000, converter.toInr(15000, "INR"), 0.01);
    }

    @Test
    void convertsEurUsingFallbackRate() {
        CurrencyConverter converter = new CurrencyConverter(HttpClient.newHttpClient(), "https://test.api.amadeus.com");
        double inr = converter.toInr(100, "EUR");
        assertEquals(9000, inr, 0.01);
    }
}
