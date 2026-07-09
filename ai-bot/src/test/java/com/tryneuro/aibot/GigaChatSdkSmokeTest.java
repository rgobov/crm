package com.tryneuro.aibot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GigaChatSdkSmokeTest {

    @Test
    void sdkClassesAreResolvable() {
        assertDoesNotThrow(() -> {
            Class.forName("chat.giga.client.GigaChatClient");
            Class.forName("chat.giga.model.completion.CompletionRequest");
            Class.forName("chat.giga.model.completion.ChatMessage");
            Class.forName("chat.giga.model.completion.ChatFunction");
            Class.forName("chat.giga.model.Scope");
        });
    }
}
