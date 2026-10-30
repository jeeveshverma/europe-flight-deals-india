package com.travel.flightdeals.util;

import com.travel.flightdeals.config.AppConfig;
import com.travel.flightdeals.model.Airport;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts offer prices to INR. Searches ask Amadeus for {@code currencyCode=INR}, so this is only a
 * safety net for an offer that comes back in another currency: it applies a fixed, approximate rate and
 * logs a warning, because a fare near the threshold may then be misjudged.
 */
public final class CurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConverter.class);
    static final double INR_PER_EUR = 90.0;
    static final double INR_PER_USD = 84.0;
    static final double INR_PER_GBP = 106.0;

    public double toInr(double amount, String currency) {
        if (currency == null || currency.isBlank() || "INR".equalsIgnoreCase(currency)) {
            return amount;
        }
        String code = currency.toUpperCase();
        double rate = approximateRateToInr(code);
        log.warn("Offer priced in {} instead of INR; converting at a fixed approximate rate of {}", code, rate);
        return amount * rate;
    }

    private static double approximateRateToInr(String currency) {
        return switch (currency) {
            case "USD" -> INR_PER_USD;
            case "GBP" -> INR_PER_GBP;
            default -> INR_PER_EUR;
        };
    }

    public static final class EuropeAirports {
        private EuropeAirports() {}

        public static final List<Airport> DEFAULT = List.of(
                new Airport("LHR", "London", "UK"),
                new Airport("CDG", "Paris", "France"),
                new Airport("AMS", "Amsterdam", "Netherlands"),
                new Airport("FCO", "Rome", "Italy"),
                new Airport("BCN", "Barcelona", "Spain"),
                new Airport("MAD", "Madrid", "Spain"),
                new Airport("FRA", "Frankfurt", "Germany"),
                new Airport("MUC", "Munich", "Germany"),
                new Airport("VIE", "Vienna", "Austria"),
                new Airport("PRG", "Prague", "Czech Republic"),
                new Airport("BUD", "Budapest", "Hungary"),
                new Airport("WAW", "Warsaw", "Poland"),
                new Airport("ATH", "Athens", "Greece"),
                new Airport("LIS", "Lisbon", "Portugal"),
                new Airport("CPH", "Copenhagen", "Denmark"),
                new Airport("OSL", "Oslo", "Norway"),
                new Airport("ARN", "Stockholm", "Sweden"),
                new Airport("HEL", "Helsinki", "Finland"),
                new Airport("DUB", "Dublin", "Ireland"),
                new Airport("ZRH", "Zurich", "Switzerland"),
                new Airport("BRU", "Brussels", "Belgium"),
                new Airport("IST", "Istanbul", "Turkey"));

        public static List<Airport> resolve(AppConfig config) {
            if (config.europeAirports().isEmpty()) {
                return DEFAULT;
            }
            return config.europeAirports().stream()
                    .map(code -> DEFAULT.stream()
                            .filter(a -> a.iataCode().equalsIgnoreCase(code))
                            .findFirst()
                            .orElse(new Airport(code, code, "Europe")))
                    .toList();
        }
    }
}
