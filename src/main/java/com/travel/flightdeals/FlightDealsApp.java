package com.travel.flightdeals;

import com.travel.flightdeals.config.AppConfig;
import com.travel.flightdeals.model.Airport;
import com.travel.flightdeals.model.FlightDeal;
import com.travel.flightdeals.service.AmadeusFlightService;
import com.travel.flightdeals.util.CurrencyConverter;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlightDealsApp {

    private static final Logger log = LoggerFactory.getLogger(FlightDealsApp.class);

    private FlightDealsApp() {}

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        CurrencyConverter currencyConverter = new CurrencyConverter(httpClient, config.apiBaseUrl());
        AmadeusFlightService flightService = new AmadeusFlightService(httpClient, config, currencyConverter);

        List<Airport> europeDestinations = CurrencyConverter.EuropeAirports.resolve(config);
        LocalDate searchDate = LocalDate.now().plusDays(config.departureDaysAhead());

        printBanner(config, europeDestinations.size(), searchDate);

        List<FlightDeal> deals = flightService.findDealsUnderThreshold(europeDestinations);
        printResults(deals, config.maxPriceInr());
    }

    private static void printBanner(AppConfig config, int destinationCount, LocalDate departure) {
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("  EUROPE FLIGHT DEALS — India → Europe under ₹" + config.maxPriceInr());
        System.out.println("=".repeat(72));
        System.out.printf("  Origins      : %s%n", String.join(", ", config.originAirports()));
        System.out.printf("  Destinations : %d European airports%n", destinationCount);
        System.out.printf("  Departure    : ~%s (+%d day return)%n", departure, config.returnDaysAfterDeparture());
        System.out.println("=".repeat(72));
        System.out.println();
    }

    private static void printResults(List<FlightDeal> deals, int maxPriceInr) {
        if (deals.isEmpty()) {
            System.out.println("No deals found under ₹" + maxPriceInr + " right now.");
            System.out.println();
            System.out.println("Tips:");
            System.out.println("  • Try increasing search.departureDaysAhead (deals often appear 4–8 weeks out)");
            System.out.println("  • Check during airline sales (Air India, IndiGo, Turkish, Qatar promos)");
            System.out.println("  • Eastern Europe (BUD, WAW, PRG) is often cheaper than Western Europe");
            System.out.println("  • Run this script daily and track — sub-₹20k deals are rare but real!");
            return;
        }

        System.out.printf("Found %d deal(s) under ₹%,d:%n%n", deals.size(), maxPriceInr);
        int rank = 1;
        for (FlightDeal deal : deals) {
            System.out.printf("%2d. %s%n", rank++, deal);
            System.out.printf("    Book: %s%n%n", deal.bookingUrl());
        }
        System.out.println("=".repeat(72));
        log.info("Search complete — {} deal(s) under ₹{}", deals.size(), maxPriceInr);
    }
}
