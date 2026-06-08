package io.eventdriven.strictland.tests.contracts.v2;

import java.util.Objects;
import java.util.UUID;

public class ShoppingCartSummary implements Comparable<ShoppingCartSummary> {
    public final UUID shoppingCartId;
    private final ShoppingCartStatus status;

    public ShoppingCartSummary(UUID shoppingCartId, ShoppingCartStatus status) {
        this.shoppingCartId = shoppingCartId;
        this.status = status;
    }

    public ShoppingCartStatus getStatus() {
        return status;
    }

    @Override
    public int compareTo(ShoppingCartSummary other) {
        return shoppingCartId.compareTo(other.shoppingCartId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingCartSummary other)) return false;
        return shoppingCartId.equals(other.shoppingCartId) && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shoppingCartId, status);
    }

    @Override
    public String toString() {
        return "ShoppingCartSummary{shoppingCartId=" + shoppingCartId + ", status=" + status + "}";
    }
}
