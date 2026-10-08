package com.example.modulith.desktop.web;

import com.example.modulith.desktop.core.DesktopDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Opens the application in the default browser once it is ready, if it runs on a local desktop.
 * Uses the operating system's own open command rather than {@code java.awt.Desktop}, which Spring Boot
 * disables (headless mode) and which would put a Java icon in the macOS Dock.
 * Switch off with {@code app.open-browser=false}.
 */
@Component
@ConditionalOnProperty(name = "app.open-browser", havingValue = "true", matchIfMissing = true)
class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private final DesktopDetection desktop;

    BrowserLauncher(DesktopDetection desktop) {
        this.desktop = desktop;
    }

    @EventListener
    void onReady(ApplicationReadyEvent event) {
        String port = event.getApplicationContext().getEnvironment().getProperty("local.server.port");
        if (port == null) {
            return; // not running a web server
        }
        String url = "http://localhost:" + port;
        Optional<List<String>> command = desktop.browserCommand(url);
        if (command.isEmpty()) {
            log.info("Application available at {}", url);
            return;
        }
        try {
            new ProcessBuilder(command.get())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            log.info("Application available at {}, opening it in the default browser", url);
        } catch (IOException e) {
            log.info("Application available at {} (could not open a browser: {})", url, e.getMessage());
        }
    }
}
