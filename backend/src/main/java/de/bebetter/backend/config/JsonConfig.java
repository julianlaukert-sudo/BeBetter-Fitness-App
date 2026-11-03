package de.bebetter.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// KI-Log Nr. 15
@Configuration
public class JsonConfig {

    // Dezimalzahlen werden nicht mehr still zu ganzen Zahlen gekürzt
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictIntegers() {
        return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }
}