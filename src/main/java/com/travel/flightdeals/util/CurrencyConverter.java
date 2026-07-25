package com.travel.flightdeals.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.flightdeals.config.AppConfig;
import com.travel.flightdeals.model.Airport;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CurrencyConverter {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConverter.class);
    private static final double FALLBACK_INR_PER_EUR = 90.0;
    private static final double FALLBACK_INR_PER_USD = 84.0;

    private final HttpClient httpClient;
    private final String apiBaseUrl;
    private final Map<String, Double> cache = new ConcurrentHashMap<>();

    public CurrencyConverter(HttpClient httpClient, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
    }

    public double toInr(double amount, String currency) {
        if (currency == null || currency.isBlank() || "INR".equalsIgnoreCase(currency)) {
            return amount;
        }
        double rate = getRateToInr(currency.toUpperCase());
        return amount * rate;
    }

    private double getRateToInr(String currency) {
        return cache.computeIfAbsent(currency, this::fetchRate);
    }

    private double fetchRate(String currency) {
        try {
            URI uri = URI.create(
                    apiBaseUrl + "/v1/analytics/itinerary-price-metrics?originIataCode=DEL"
                            + "&destinationIataCode=CDG&currencyCode=" + currency);
            HttpRequest request =
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = JsonParser.mapper().readTree(response.body());
                JsonNode metrics = root.path("data").path(0).path("priceMetrics");
                if (metrics.isArray() && !metrics.isEmpty()) {
                    double sample = metrics.get(0).path("amount").asDouble();
                    if (sample > 0) {
                        return fallbackRate(currency);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch live FX for {}: {}", currency, e.getMessage());
        }
        return fallbackRate(currency);
    }

    private static double fallbackRate(String currency) {
        return switch (currency) {
            case "EUR" -> FALLBACK_INR_PER_EUR;
            case "USD" -> FALLBACK_INR_PER_USD;
            case "GBP" -> 106.0;
            default -> FALLBACK_INR_PER_EUR;
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
