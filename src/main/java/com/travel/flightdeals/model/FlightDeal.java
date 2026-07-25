package com.travel.flightdeals.model;

import java.time.LocalDate;
import java.util.Objects;

public record FlightDeal(
        String origin,
        String destination,
        String destinationCity,
        LocalDate departureDate,
        LocalDate returnDate,
        double priceInr,
        String currency,
        double originalPrice,
        String airline,
        int stops,
        String bookingUrl) {

    public FlightDeal {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(destination);
        Objects.requireNonNull(departureDate);
    }

    @Override
    public String toString() {
        return String.format(
                "₹%,.0f | %s → %s (%s) | %s → %s | %s | %d stop(s)",
                priceInr,
                origin,
                destination,
                destinationCity,
                departureDate,
                returnDate != null ? returnDate : "one-way",
                airline,
                stops);
    }
}
