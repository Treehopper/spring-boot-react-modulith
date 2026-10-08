package com.example.modulith.desktop.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Detects a local desktop session from the operating system and environment variables, and knows each
 * operating system's command for opening a URL in the default browser.
 */
@Service
public class DesktopDetection {

    private final Boolean override;

    DesktopDetection(@Value("${app.desktop:#{null}}") Boolean override) {
        this.override = override;
    }

    /**
     * @return {@code true} on macOS or Windows, or on Linux with a graphical display, unless the
     * application runs in CI or over SSH; overridable with {@code app.desktop=true|false}
     */
    public boolean isLocalDesktop() {
        return override != null ? override : detect(System.getProperty("os.name"), System.getenv()).isPresent();
    }

    /**
     * @return the command that opens {@code url} in the default browser, or empty if this is not a local desktop
     */
    public Optional<List<String>> browserCommand(String url) {
        if (!isLocalDesktop()) {
            return Optional.empty();
        }
        // Forced with app.desktop=true on a system that is not detected as a desktop: no known command.
        return detect(System.getProperty("os.name"), System.getenv()).map(os -> os.openCommand(url));
    }

    /**
     * @return the desktop operating system, or empty if this does not look like a desktop session:
     * CI, SSH, or Linux without a graphical display (servers, containers)
     */
    static Optional<OperatingSystem> detect(String osName, Map<String, String> env) {
        if (env.containsKey("CI") || env.containsKey("SSH_CONNECTION") || env.containsKey("SSH_TTY")) {
            return Optional.empty();
        }
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("mac")) {
            return Optional.of(OperatingSystem.MACOS);
        }
        if (os.startsWith("windows")) {
            return Optional.of(OperatingSystem.WINDOWS);
        }
        if (env.containsKey("DISPLAY") || env.containsKey("WAYLAND_DISPLAY")) {
            return Optional.of(OperatingSystem.LINUX);
        }
        return Optional.empty();
    }

    enum OperatingSystem {
        MACOS("open"),
        WINDOWS("rundll32", "url.dll,FileProtocolHandler"),
        LINUX("xdg-open");

        private final List<String> command;

        OperatingSystem(String... command) {
            this.command = List.of(command);
        }

        List<String> openCommand(String url) {
            return Stream.concat(command.stream(), Stream.of(url)).toList();
        }
    }
}
