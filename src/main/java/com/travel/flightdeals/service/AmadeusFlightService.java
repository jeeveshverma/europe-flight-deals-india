package com.travel.flightdeals.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.flightdeals.config.AppConfig;
import com.travel.flightdeals.model.Airport;
import com.travel.flightdeals.model.FlightDeal;
import com.travel.flightdeals.util.CurrencyConverter;
import com.travel.flightdeals.util.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmadeusFlightService {

    private static final Logger log = LoggerFactory.getLogger(AmadeusFlightService.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HttpClient httpClient;
    private final AppConfig config;
    private final CurrencyConverter currencyConverter;
    private String accessToken;
    private long tokenExpiresAtEpochMs;

    public AmadeusFlightService(HttpClient httpClient, AppConfig config, CurrencyConverter currencyConverter) {
        this.httpClient = httpClient;
        this.config = config;
        this.currencyConverter = currencyConverter;
    }

    public List<FlightDeal> findDealsUnderThreshold(List<Airport> europeDestinations) throws IOException, InterruptedException {
        List<FlightDeal> allDeals = new ArrayList<>();
        LocalDate departure = LocalDate.now().plusDays(config.departureDaysAhead());
        LocalDate returnDate = departure.plusDays(config.returnDaysAfterDeparture());

        for (String origin : config.originAirports()) {
            for (Airport destination : europeDestinations) {
                try {
                    Optional<FlightDeal> cheapest =
                            searchCheapestRoundTrip(origin, destination, departure, returnDate);
                    cheapest.ifPresent(deal -> {
                        if (deal.priceInr() <= config.maxPriceInr()) {
                            allDeals.add(deal);
                            log.info("DEAL FOUND: {}", deal);
                        } else {
                            log.debug(
                                    "Above threshold: {} → {} at ₹{}",
                                    origin,
                                    destination.iataCode(),
                                    String.format("%,.0f", deal.priceInr()));
                        }
                    });
                    Thread.sleep(250);
                } catch (Exception e) {
                    log.warn(
                            "Search failed for {} → {}: {}",
                            origin,
                            destination.iataCode(),
                            e.getMessage());
                }
            }
        }

        allDeals.sort(Comparator.comparingDouble(FlightDeal::priceInr));
        return allDeals;
    }

    private Optional<FlightDeal> searchCheapestRoundTrip(
            String origin, Airport destination, LocalDate departure, LocalDate returnDate)
            throws IOException, InterruptedException {
        ensureToken();

        String query = String.format(
                "originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&returnDate=%s"
                        + "&adults=1&currencyCode=INR&max=5&nonStop=false",
                urlEncode(origin),
                urlEncode(destination.iataCode()),
                urlEncode(departure.format(ISO_DATE)),
                urlEncode(returnDate.format(ISO_DATE)));

        URI uri = URI.create(config.apiBaseUrl() + "/v2/shopping/flight-offers?" + query);
        HttpRequest request =
                HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            accessToken = null;
            ensureToken();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        if (response.statusCode() != 200) {
            log.debug("API {} for {}→{}: {}", response.statusCode(), origin, destination.iataCode(), response.body());
            return Optional.empty();
        }

        JsonNode root = JsonParser.mapper().readTree(response.body());
        JsonNode offers = root.path("data");
        if (!offers.isArray() || offers.isEmpty()) {
            return Optional.empty();
        }

        FlightDeal cheapest = null;
        for (JsonNode offer : offers) {
            FlightDeal deal = parseOffer(offer, origin, destination);
            if (cheapest == null || deal.priceInr() < cheapest.priceInr()) {
                cheapest = deal;
            }
        }
        return Optional.ofNullable(cheapest);
    }

    private FlightDeal parseOffer(JsonNode offer, String origin, Airport destination) {
        JsonNode priceNode = offer.path("price");
        double total = priceNode.path("total").asDouble();
        String currency = JsonParser.textOrEmpty(priceNode, "currency");
        double priceInr = currencyConverter.toInr(total, currency);

        JsonNode itinerary = offer.path("itineraries").get(0);
        JsonNode segments = itinerary.path("segments");
        String airline = segments.get(0).path("carrierCode").asText("?");
        int stops = Math.max(0, segments.size() - 1);

        String departureDate = segments.get(0).path("departure").path("at").asText("").substring(0, 10);
        LocalDate returnDate = null;
        if (offer.path("itineraries").size() > 1) {
            JsonNode returnSegments = offer.path("itineraries").get(1).path("segments");
            returnDate = LocalDate.parse(returnSegments.get(0).path("departure").path("at").asText("").substring(0, 10));
        }

        String bookingUrl = "https://www.google.com/travel/flights?q=Flights%20from%20"
                + origin + "%20to%20" + destination.iataCode();

        return new FlightDeal(
                origin,
                destination.iataCode(),
                destination.city(),
                LocalDate.parse(departureDate),
                returnDate,
                priceInr,
                currency,
                total,
                airline,
                stops,
                bookingUrl);
    }

    private void ensureToken() throws IOException, InterruptedException {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAtEpochMs) {
            return;
        }

        String body = "grant_type=client_credentials"
                + "&client_id=" + urlEncode(config.apiKey())
                + "&client_secret=" + urlEncode(config.apiSecret());

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(config.apiBaseUrl() + "/v1/security/oauth2/token"))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to obtain Amadeus token: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = JsonParser.mapper().readTree(response.body());
        accessToken = root.path("access_token").asText();
        int expiresInSec = root.path("expires_in").asInt(1800);
        tokenExpiresAtEpochMs = System.currentTimeMillis() + (expiresInSec - 60L) * 1000L;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
