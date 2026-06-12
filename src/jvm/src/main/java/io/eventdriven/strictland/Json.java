package io.eventdriven.strictland;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The JSON serializers for a specification. Take a {@link SpecificationOptions} from {@link Jackson}
 * and pass it to {@link MessageContract#specification(SpecificationOptions)}. For another format, write
 * a {@link MessageSerializer} and start from {@link SpecificationOptions#serializer(MessageSerializer)}.
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
     * JSON options built on Jackson. {@link #of(ObjectMapper)} serializes with your application's own
     * mapper, checking the exact bytes you ship; {@link #defaults()} uses Strictland's default mapper
     * when you have none to reuse.
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
         * JSON options using Strictland's default Jackson mapper, which writes ISO-8601 dates, keeps null
         * fields, and ignores unknown properties on read. Use it when your application hasn't
         * customized Jackson; otherwise pass your own mapper to {@link #of(ObjectMapper)}.
         *
         * @return options bound to the default Jackson serializer
         */
        static SpecificationOptions defaults() {
            return SpecificationOptions.serializer(JacksonMessageSerializer.withDefaults());
        }

        /**
         * JSON options bound to your own Jackson mapper. The check then serializes the exact bytes you
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
