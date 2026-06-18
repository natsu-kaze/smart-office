package com.natsukaze.smartoffice.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigInteger;

@Configuration
public class JacksonSafeLongConfig {

    private static final long JS_MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private static final BigInteger JS_MAX_SAFE_BIG_INTEGER = BigInteger.valueOf(JS_MAX_SAFE_INTEGER);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaScriptSafeLongCustomizer() {
        JavaScriptSafeLongSerializer longSerializer = new JavaScriptSafeLongSerializer();
        JavaScriptSafeBigIntegerSerializer bigIntegerSerializer = new JavaScriptSafeBigIntegerSerializer();
        return builder -> builder
                .serializerByType(Long.class, longSerializer)
                .serializerByType(Long.TYPE, longSerializer)
                .serializerByType(BigInteger.class, bigIntegerSerializer);
    }

    private static class JavaScriptSafeLongSerializer extends JsonSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value > JS_MAX_SAFE_INTEGER || value < -JS_MAX_SAFE_INTEGER) {
                gen.writeString(value.toString());
                return;
            }
            gen.writeNumber(value);
        }
    }

    private static class JavaScriptSafeBigIntegerSerializer extends JsonSerializer<BigInteger> {

        @Override
        public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            if (value.abs().compareTo(JS_MAX_SAFE_BIG_INTEGER) > 0) {
                gen.writeString(value.toString());
                return;
            }
            gen.writeNumber(value);
        }
    }
}
