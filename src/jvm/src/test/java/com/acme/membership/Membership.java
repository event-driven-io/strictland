package com.acme.membership;

import java.util.UUID;

public interface Membership {
    record MemberJoined(UUID userId, String email) {}
}
