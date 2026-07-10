package com.tryneuro.aibot.service;

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

    public String transcribe(Path audioFile) {
        if (audioFile == null || !Files.exists(audioFile)) {
            log.warn("Audio file not found: {}", audioFile);
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    WHISPER_BIN,
                    "-m", MODEL_PATH,
                    "-l", "ru",
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

            String stdout = new String(process.getInputStream().readAllBytes()).strip();
            log.info("Whisper transcription done in {}ms, output_len={}", elapsed, stdout.length());
            log.debug("Whisper output: {}", stdout);

            if (stdout.isEmpty()) {
                log.warn("Whisper returned empty output for {}", audioFile);
                return null;
            }

            return stdout;
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