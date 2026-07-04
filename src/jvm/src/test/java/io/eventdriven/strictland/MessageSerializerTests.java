package io.eventdriven.strictland;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
final class MessageSerializerTests {

    @Test
    void jacksonSerializerReportsJsonExtension() {
        assertEquals("json", JacksonMessageSerializer.withDefaults().fileExtension());
    }

    @Test
    void csvSerializerReportsCsvExtension() {
        assertEquals("csv", new CsvMessageSerializer().fileExtension());
    }

    @Test
    void binarySerializerReportsBinExtension() {
        assertEquals("bin", new SimpleBinaryMessageSerializer().fileExtension());
    }

    @Test
    void bareSerializerReportsDefaultTxtExtension() {
        MessageSerializer bare = new MessageSerializer() {
            @Override
            public byte[] serialize(Object value) {
                return new byte[0];
            }

            @Override
            public <T> T deserialize(byte[] bytes, Class<T> type) {
                throw new UnsupportedOperationException();
            }
        };

        assertEquals("txt", bare.fileExtension());
    }
}
