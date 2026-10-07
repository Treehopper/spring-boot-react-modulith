package com.example.modulith.frontend.web;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Serves the React build from Spring Boot's standard static locations
 * ({@code spring.web.resources.static-locations}, default {@code classpath:/static/})
 * and falls back to {@code index.html} for client-side routes.
 * <p>
 * This is a functional route rather than a {@code WebMvcConfigurer}: the actuator management
 * context on its own port inherits {@code WebMvcConfigurer} beans, but not router functions,
 * so the frontend stays on the main port only. Spring Boot's default static mapping is
 * switched off ({@code spring.web.resources.add-mappings=false}) in favour of this one.
 * <p>
 * Caching: Vite's content-hashed bundles under {@code /assets} are cached for a year as immutable.
 * {@code index.html}, client-side routes and other files are revalidated on every request
 * ({@code no-cache}), so a new deployment is picked up immediately. With
 * {@code spring.web.resources.cache.period=0} (the {@code dev} profile) nothing is stored at all.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebProperties.class)
class SpaWebConfig {

    /** Vite's output folder for bundles whose file names contain a content hash ({@code build.assetsDir}). */
    private static final String HASHED_ASSETS = "assets/";

    @Bean
    RouterFunction<ServerResponse> frontendRoutes(WebProperties webProperties, ResourceLoader resourceLoader) {
        WebProperties.Resources resources = webProperties.getResources();
        List<Resource> locations = Arrays.stream(resources.getStaticLocations())
                .map(resourceLoader::getResource)
                .toList();
        if (locations.isEmpty()) {
            throw new IllegalStateException("No spring.web.resources.static-locations configured");
        }

        // The dev profile sets spring.web.resources.cache.period=0, so every rebuild shows up on refresh.
        Duration period = resources.getCache().getPeriod();
        boolean noStore = period != null && period.isZero();
        // A new build changes the hashed file names, so these can be cached for good...
        CacheControl hashed = noStore ? CacheControl.noStore()
                : CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
        // ...while index.html (which references them) and other files must be revalidated every time.
        CacheControl unhashed = noStore ? CacheControl.noStore() : CacheControl.noCache();

        // Static files, one route per location. Spring's resource lookup guards against path traversal.
        RouterFunction<ServerResponse> hashedAssets = locations.stream()
                .map(location -> RouterFunctions.resources("/" + HASHED_ASSETS + "**", relative(location, HASHED_ASSETS),
                        (resource, headers) -> headers.setCacheControl(hashed)))
                .reduce(RouterFunction::and)
                .orElseThrow();
        RouterFunction<ServerResponse> otherFiles = locations.stream()
                .map(location -> RouterFunctions.resources("/**", location,
                        (resource, headers) -> headers.setCacheControl(unhashed)))
                .reduce(RouterFunction::and)
                .orElseThrow();

        // Client-side routes get index.html, looked up per request so the dev profile picks up rebuilds.
        // Only for original requests: the ERROR dispatch that renders a 404 (to /error) must not get index.html.
        RouterFunction<ServerResponse> clientRoutes = RouterFunctions.route(
                RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD)
                        .and(request -> request.servletRequest().getDispatcherType() == DispatcherType.REQUEST)
                        .and(request -> isClientRoute(request.path())),
                request -> indexHtml(locations)
                        .map(index -> ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .cacheControl(unhashed)
                                .body(index))
                        .orElseGet(() -> ServerResponse.notFound().build()));

        return hashedAssets.and(otherFiles).and(clientRoutes);
    }

    /**
     * Paths without a file extension outside {@code /api} are React routes.
     * Missing assets and unknown API endpoints stay 404.
     */
    static boolean isClientRoute(String path) {
        boolean api = path.equals("/api") || path.startsWith("/api/");
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !api && !lastSegment.contains(".");
    }

    private static Optional<Resource> indexHtml(List<Resource> locations) {
        return locations.stream()
                .map(location -> relative(location, "index.html"))
                .filter(Resource::isReadable)
                .findFirst();
    }

    private static Resource relative(Resource location, String path) {
        try {
            return location.createRelative(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot resolve " + path + " in " + location, e);
        }
    }
}
