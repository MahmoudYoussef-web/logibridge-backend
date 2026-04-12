package com.logibridge.backend.order.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String datePart = LocalDate.now().format(FORMATTER);

        String uniquePart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + datePart + "-" + uniquePart;
    }
}