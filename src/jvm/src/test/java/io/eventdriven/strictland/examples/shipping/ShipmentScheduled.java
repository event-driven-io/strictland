package io.eventdriven.strictland.examples.shipping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShipmentScheduled(UUID shipmentId, String recipientName, OffsetDateTime scheduledAt) {}
