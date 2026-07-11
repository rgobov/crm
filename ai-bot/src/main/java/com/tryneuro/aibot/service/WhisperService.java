package com.tryneuro.aibot.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class WhisperService {

    private static final Logger log = LoggerFactory.getLogger(WhisperService.class);

    private static final String WHISPER_BIN = "/usr/local/bin/whisper-cli";
    private static final String MODEL_PATH = "/opt/whisper/ggml-base.bin";
    private static final int TIMEOUT_SECONDS = 60;

    private boolean available;

    @PostConstruct
    void init() {
        boolean binExists = Files.exists(Path.of(WHISPER_BIN));
        boolean modelExists = Files.exists(Path.of(MODEL_PATH));
        available = binExists && modelExists;
        if (available) {
            log.info("Whisper service available: bin={}, model={}", WHISPER_BIN, MODEL_PATH);
        } else {
            log.warn("Whisper NOT available (bin={}, model={}) — voice messages will not work. "
                    + "Place whisper-cli and ggml-base.bin on the host and mount /opt/whisper volume.",
                    binExists ? "OK" : "missing", modelExists ? "OK" : "missing");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public String transcribe(Path audioFile) {
        if (!available) {
            log.warn("Whisper not available, skipping transcription");
            return null;
        }
        if (audioFile == null || !Files.exists(audioFile)) {
            log.warn("Audio file not found: {}", audioFile);
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    WHISPER_BIN,
                    "-m", MODEL_PATH,
                    "-l", "ru",
                    "-ng",
                    "-f", audioFile.toAbsolutePath().toString(),
                    "-otxt"
            );
            pb.redirectErrorStream(true);

            long startMs = System.currentTimeMillis();
            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startMs;

            if (!finished) {
                log.error("Whisper transcription timed out after {}ms", elapsed);
                process.destroyForcibly();
                return null;
            }

            String outputFile = audioFile.toAbsolutePath() + ".txt";
            Path txtPath = Path.of(outputFile);
            if (!Files.exists(txtPath)) {
                log.warn("Whisper output file not found: {}", outputFile);
                return null;
            }
            String text = Files.readString(txtPath).strip();
            try { Files.delete(txtPath); } catch (Exception ignored) {}
            log.info("Whisper transcription done in {}ms, text_len={}", elapsed, text != null ? text.length() : 0);
            log.debug("Whisper output: {}", text);

            if (text == null || text.isEmpty()) {
                log.warn("Whisper returned empty text for {}", audioFile);
                return null;
            }

            return text;
        } catch (IOException e) {
            log.error("Whisper process failed for {}: {}", audioFile, e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Whisper transcription interrupted for {}", audioFile);
            return null;
        }
    }
}