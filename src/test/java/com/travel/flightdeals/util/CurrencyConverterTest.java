package com.travel.flightdeals.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CurrencyConverterTest {

    private final CurrencyConverter converter = new CurrencyConverter();

    @Test
    void convertsInrAsIdentity() {
        assertEquals(15000, converter.toInr(15000, "INR"), 0.01);
    }

    @Test
    void treatsMissingCurrencyAsInr() {
        assertEquals(15000, converter.toInr(15000, null), 0.01);
        assertEquals(15000, converter.toInr(15000, " "), 0.01);
    }

    @Test
    void convertsKnownCurrenciesAtFixedRates() {
        assertEquals(9000, converter.toInr(100, "EUR"), 0.01);
        assertEquals(8400, converter.toInr(100, "USD"), 0.01);
        assertEquals(10600, converter.toInr(100, "GBP"), 0.01);
    }

    @Test
    void currencyCodeIsCaseInsensitive() {
        assertEquals(9000, converter.toInr(100, "eur"), 0.01);
    }

    @Test
    void unknownCurrencyFallsBackToEurRate() {
        assertEquals(9000, converter.toInr(100, "CHF"), 0.01);
    }
}
