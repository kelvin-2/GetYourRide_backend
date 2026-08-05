package com.example1.getyourride.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Maintains compatibility with legacy booking-status values such as "Confirmed"
 * while persisting the canonical uppercase enum representation.
 */
@Converter
public class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(BookingStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public BookingStatus convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return BookingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}