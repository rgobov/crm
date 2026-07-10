package com.tryneuro.aibot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WhisperServiceTest {

    private final WhisperService whisperService = new WhisperService();

    @Test
    @DisplayName("transcribe возвращает null для несуществующего файла")
    void transcribeReturnsNullForMissingFile() {
        Path missing = Path.of("/nonexistent/audio.ogg");
        assertNull(whisperService.transcribe(missing));
    }

    @Test
    @DisplayName("transcribe возвращает null для пустого аудиофайла (whisper упадёт)")
    void transcribeHandlesEmptyFile(@TempDir Path tempDir) throws Exception {
        Path empty = tempDir.resolve("empty.ogg");
        Files.write(empty, new byte[0]);

        String result = whisperService.transcribe(empty);
        assertNull(result);
    }

    @Test
    @DisplayName("transcribe не должен падать с exception — всегда возвращает null при ошибке")
    void transcribeNeverThrows() {
        assertNull(whisperService.transcribe(null));
    }
}