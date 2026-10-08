package com.example.modulith.desktop.core;

import com.example.modulith.desktop.core.DesktopDetection.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopDetectionTest {

    private static final String URL = "http://localhost:8080";

    @Test
    void detectsDesktops() {
        assertThat(DesktopDetection.detect("Mac OS X", Map.of())).contains(OperatingSystem.MACOS);
        assertThat(DesktopDetection.detect("Windows 11", Map.of())).contains(OperatingSystem.WINDOWS);
        assertThat(DesktopDetection.detect("Linux", Map.of("DISPLAY", ":0"))).contains(OperatingSystem.LINUX);
        assertThat(DesktopDetection.detect("Linux", Map.of("WAYLAND_DISPLAY", "wayland-0"))).contains(OperatingSystem.LINUX);
    }

    @Test
    void rejectsServersCiAndSsh() {
        assertThat(DesktopDetection.detect("Linux", Map.of())).isEmpty();
        assertThat(DesktopDetection.detect("Mac OS X", Map.of("SSH_CONNECTION", "10.0.0.1 22 10.0.0.2 22"))).isEmpty();
        assertThat(DesktopDetection.detect("Linux", Map.of("DISPLAY", ":0", "SSH_TTY", "/dev/pts/0"))).isEmpty();
        assertThat(DesktopDetection.detect("Windows 11", Map.of("CI", "true"))).isEmpty();
        assertThat(DesktopDetection.detect(null, Map.of())).isEmpty();
    }

    @Test
    void usesEachOperatingSystemsOpenCommand() {
        assertThat(OperatingSystem.MACOS.openCommand(URL)).isEqualTo(List.of("open", URL));
        assertThat(OperatingSystem.WINDOWS.openCommand(URL)).isEqualTo(List.of("rundll32", "url.dll,FileProtocolHandler", URL));
        assertThat(OperatingSystem.LINUX.openCommand(URL)).isEqualTo(List.of("xdg-open", URL));
    }

    @Test
    void appDesktopPropertyOverridesDetection() {
        assertThat(new DesktopDetection(true).isLocalDesktop()).isTrue();
        assertThat(new DesktopDetection(false).isLocalDesktop()).isFalse();
        assertThat(new DesktopDetection(false).browserCommand(URL)).isEmpty();
    }
}
