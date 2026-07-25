package com.travel.flightdeals.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class AppConfig {

    private final String apiKey;
    private final String apiSecret;
    private final String apiBaseUrl;
    private final int maxPriceInr;
    private final int departureDaysAhead;
    private final int returnDaysAfterDeparture;
    private final int flexibleDays;
    private final List<String> originAirports;
    private final List<String> europeAirports;

    private AppConfig(
            String apiKey,
            String apiSecret,
            String apiBaseUrl,
            int maxPriceInr,
            int departureDaysAhead,
            int returnDaysAfterDeparture,
            int flexibleDays,
            List<String> originAirports,
            List<String> europeAirports) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiBaseUrl = apiBaseUrl;
        this.maxPriceInr = maxPriceInr;
        this.departureDaysAhead = departureDaysAhead;
        this.returnDaysAfterDeparture = returnDaysAfterDeparture;
        this.flexibleDays = flexibleDays;
        this.originAirports = originAirports;
        this.europeAirports = europeAirports;
    }

    public static AppConfig load() throws IOException {
        Properties props = new Properties();
        Path localPath = Path.of("src/main/resources/application.properties");
        if (Files.exists(localPath)) {
            try (InputStream in = Files.newInputStream(localPath)) {
                props.load(in);
            }
        } else {
            try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
                if (in == null) {
                    throw new IOException(
                            "application.properties not found. Copy application.properties.example "
                                    + "to src/main/resources/application.properties and add your API keys.");
                }
                props.load(in);
            }
        }
        return fromProperties(props);
    }

    static AppConfig fromProperties(Properties props) {
        String europeAirportsRaw = props.getProperty("search.europeAirports", "").trim();
        List<String> europeAirports =
                europeAirportsRaw.isEmpty()
                        ? List.of()
                        : Arrays.stream(europeAirportsRaw.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();

        return new AppConfig(
                require(props, "amadeus.api.key"),
                require(props, "amadeus.api.secret"),
                props.getProperty("amadeus.api.baseUrl", "https://test.api.amadeus.com"),
                Integer.parseInt(props.getProperty("price.maxInr", "20000")),
                Integer.parseInt(props.getProperty("search.departureDaysAhead", "30")),
                Integer.parseInt(props.getProperty("search.returnDaysAfterDeparture", "14")),
                Integer.parseInt(props.getProperty("search.flexibleDays", "3")),
                splitCsv(props.getProperty("search.originAirports", "DEL,BOM,BLR,MAA,HYD,CCU,AMD")),
                europeAirports);
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("YOUR_")) {
            throw new IllegalArgumentException("Missing or placeholder value for: " + key);
        }
        return value.trim();
    }

    private static List<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String apiKey() {
        return apiKey;
    }

    public String apiSecret() {
        return apiSecret;
    }

    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    public int maxPriceInr() {
        return maxPriceInr;
    }

    public int departureDaysAhead() {
        return departureDaysAhead;
    }

    public int returnDaysAfterDeparture() {
        return returnDaysAfterDeparture;
    }

    public int flexibleDays() {
        return flexibleDays;
    }

    public List<String> originAirports() {
        return originAirports;
    }

    public List<String> europeAirports() {
        return europeAirports;
    }
}
