package com.bnguimgo.ailoganalyzer.config;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum AiProviderType {

    MOCK("mock"),
    OPENAI("openai");

    private final String value;

    AiProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AiProviderType fromValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "AI provider must not be null or empty. " +
                            "Supported providers: " + supportedValues());
        }

        for (AiProviderType provider : values()) {
            if (provider.value.equalsIgnoreCase(value.trim())) {
                return provider;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported AI provider: " + value +
                        ". Supported providers: " + supportedValues());
    }

    private static String supportedValues() {
        return Arrays.stream(values())
                .map(AiProviderType::getValue)
                .collect(Collectors.joining(", "));
    }
}