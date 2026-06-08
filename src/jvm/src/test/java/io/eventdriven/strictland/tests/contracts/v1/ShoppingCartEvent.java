package io.eventdriven.strictland.tests.contracts.v1;

import io.eventdriven.strictland.tests.contracts.v1.productitems.ProductItem;
import java.time.OffsetDateTime;
import java.util.UUID;

public sealed interface ShoppingCartEvent extends HasShoppingCartId {

    record ShoppingCartOpened(UUID shoppingCartId, UUID clientId) implements ShoppingCartEvent {}

    record ProductItemAddedToShoppingCart(UUID shoppingCartId, ProductItem productItem) implements ShoppingCartEvent {}

    record ShoppingCartConfirmed(UUID shoppingCartId, OffsetDateTime confirmedAt) implements ShoppingCartEvent {}
}
