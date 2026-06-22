package com.acme.orders;

import java.util.UUID;

public record OrderInitiated(UUID orderId, String customer, String promotion) {}
