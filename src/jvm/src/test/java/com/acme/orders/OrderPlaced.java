package com.acme.orders;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPlaced(UUID orderId, String customer, OffsetDateTime placedAt) {}
