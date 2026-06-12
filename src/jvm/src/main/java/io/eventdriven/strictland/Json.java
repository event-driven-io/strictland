package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON entry points for building {@link SpecificationOptions}, grouping the serializers that read
 * and write JSON. Reach here when your messages are JSON, the common case; for another format, write a
 * {@link MessageSerializer} and start from {@link SpecificationOptions#serializer(MessageSerializer)}.
 *
 * <p>{@link Jackson} is the only implementation today.
 *
 * {@snippet :
 * MessageContract.specification(Json.Jackson.defaults())
 *     .given(new OrderPlaced(orderId, "Alice", placedAt))
 *     .whenSerialized()
 *     .thenContractIsUnchanged();
 * }
 */
public interface Json {

    /**
     * JSON options backed by Jackson, the serializer most applications already use. {@link
     * #defaults()} fits when you haven't customized Jackson; {@link #of(ObjectMapper)} binds your own
     * mapper so the check pins the exact bytes you ship.
     *
     * {@snippet :
     * MessageContract.specification(Json.Jackson.of(myObjectMapper))
     *     .given(new OrderPlaced(orderId, "Alice", placedAt))
     *     .whenSerialized()
     *     .thenContractIsUnchanged();
     * }
     */
    interface Jackson {

        /**
         * JSON options on Strictland's default Jackson mapper, which writes ISO-8601 dates, keeps null
         * fields, and ignores unknown properties on read. Use it when your application hasn't
         * customized Jackson; otherwise pass your own mapper to {@link #of(ObjectMapper)}.
         *
         * @return options bound to the default Jackson serializer
         */
        static SpecificationOptions defaults() {
            return SpecificationOptions.serializer(JacksonMessageSerializer.withDefaults());
        }

        /**
         * JSON options bound to your own Jackson mapper, so the check serializes the exact bytes you
         * ship: snake_case naming, a custom date format, {@code NON_NULL} inclusion, and so on.
         *
         * @param mapper the Jackson mapper your application serializes with
         * @return options bound to a Jackson serializer using that mapper
         */
        static SpecificationOptions of(ObjectMapper mapper) {
            return SpecificationOptions.serializer(new JacksonMessageSerializer(mapper));
        }
    }
}
