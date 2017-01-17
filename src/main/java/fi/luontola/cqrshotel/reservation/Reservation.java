// Copyright © 2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.AggregateRoot;
import fi.luontola.cqrshotel.framework.EventListener;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.events.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Reservation extends AggregateRoot {

    public static final Duration PRICE_VALIDITY_DURATION = Duration.ofMinutes(30);

    private UUID id;
    private final List<PriceOffered> priceOffers = new ArrayList<>();
    private int lineItems = 0;

    @Override
    public UUID getId() {
        return id;
    }

    @EventListener
    private void apply(ReservationInitialized event) {
        id = event.reservationId;
    }

    @EventListener
    private void apply(PriceOffered event) {
        priceOffers.add(event);
    }

    @EventListener
    private void apply(LineItemCreated event) {
        lineItems++;
    }

    public void initialize(UUID reservationId) {
        publish(new ReservationInitialized(reservationId));
    }

    public void searchForAccommodation(LocalDate startDate, LocalDate endDate, PricingEngine pricing, Clock clock) {
        publish(new SearchedForAccommodation(id, startDate, endDate));
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            makePriceOffer(date, pricing, clock);
        }
    }

    private void makePriceOffer(LocalDate date, PricingEngine pricing, Clock clock) {
        if (hasValidPriceOffer(date, clock)) {
            return;
        }
        pricing.getAccommodationPrice(date).ifPresent(price -> {
            Instant expires = clock.instant().plus(PRICE_VALIDITY_DURATION);
            publish(new PriceOffered(id, date, price, expires));
        });
    }

    private boolean hasValidPriceOffer(LocalDate date, Clock clock) {
        return findValidPriceOffer(date, clock).isPresent();
    }

    private Optional<PriceOffered> findValidPriceOffer(LocalDate date, Clock clock) {
        return priceOffers.stream()
                .filter(offer -> offer.date.equals(date) && offer.isStillValid(clock))
                .findFirst();
    }

    public void updateContactInformation(String name, String email) {
        publish(new ContactInformationUpdated(id, name, email));
    }

    public void makeReservation(LocalDate startDate, LocalDate endDate, Clock clock) {
        Instant checkInTime = startDate
                .atTime(Hotel.CHECK_IN_TIME)
                .atZone(Hotel.TIMEZONE)
                .toInstant();
        Instant checkOutTime = endDate
                .atTime(Hotel.CHECK_OUT_TIME)
                .atZone(Hotel.TIMEZONE)
                .toInstant();
        publish(new ReservationMade(id, checkInTime, checkOutTime));

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            PriceOffered offer = findValidPriceOffer(date, clock).get();
            publish(new LineItemCreated(id, lineItems + 1, offer.date, offer.price));
        }
    }
}
