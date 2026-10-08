# spring-boot-react-modulith

Template for a Spring Boot backend with an embedded React frontend, organised as a modulith whose
architecture is enforced by ArchUnit. The PoC output is a page that says **Hello, World!**: the text
comes from two backend components, through every layer and the database.

## Build and run

Requirements: JDK 25. Maven runs through the wrapper, and Node is downloaded by the build.

```sh
./mvnw verify                          # builds the frontend, runs all tests, produces one jar
java -jar backend/target/modulith.jar  # http://localhost:8080
```

On a desktop, the application opens http://localhost:8080 in the default browser once it is ready.
It uses the operating system's own command (`open` on macOS, `rundll32` on Windows, `xdg-open` on
Linux), not `java.awt.Desktop`. It skips this in CI (`CI` set), over SSH (`SSH_CONNECTION` or
`SSH_TTY`), and on Linux without a display (`DISPLAY`/`WAYLAND_DISPLAY`), so servers and containers
only log the URL. Turn it off with `--app.open-browser=false` (the tests do this through Surefire),
or override the desktop detection with `--app.desktop=true|false`.

## Releases

Pushing a tag that starts with `v` runs [`.github/workflows/release.yml`](.github/workflows/release.yml):

```sh
git tag v1.0.0 && git push origin v1.0.0
```

The workflow builds and tests the project (`./mvnw verify`) and packs `modulith.jar` into
`modulith-v1.0.0.7z`. It attaches the archive and its `.sha256` checksum to a GitHub release
for the tag, with generated release notes. Re-running the workflow for an existing release replaces
its assets.

```sh
sha256sum -c modulith-v1.0.0.7z.sha256 && 7z x modulith-v1.0.0.7z && java -jar modulith.jar
```

## Local development

Run both commands in separate terminals:

```sh
cd frontend && npm run watch                   # rebuilds frontend/dist on every save
./mvnw -pl backend spring-boot:run -Ddev       # serves frontend/dist from disk on :8080
```

Edit a React file and refresh http://localhost:8080. You don't need to rebuild the Java backend or
restart it. The `-Ddev` flag leaves out the embedded frontend jar and activates the `dev` Spring profile
(`backend/src/main/resources/application-dev.properties`). That profile points
`spring.web.resources.static-locations` at `frontend/dist` and turns off caching. In an IDE, run
`ModulithApplication` with the Spring profile `dev`.

The UI and the API share port 8080 in every mode, and the frontend calls `/api/...` on the same origin.
So there is no CORS configuration anywhere.

## Management port (8081)

Operational tooling runs on Spring Boot Actuator's **separate management port, 8081, bound to
`127.0.0.1`**. Port 8080 still serves only the UI and the API, so the single-port setup for the
application is unchanged.

| URL | What |
|-----|------|
| http://localhost:8081/actuator/swagger-ui | Swagger UI for the API contract (login only with `adminPassword`, see below) |
| http://localhost:8081/actuator/loggers | Read and change log levels at runtime |
| http://localhost:8081/actuator/health | Health check |

Only Swagger UI can be password-protected. `loggers` and `health` are always open, so anyone who can reach this
port can change log levels, for example to flood the logs. Before using it remotely (for example
inside a container), either bind `management.server.address` to an internal network, or extend the
protected paths in `security.web.SecurityConfig`. Only the endpoints listed in
`management.endpoints.web.exposure.include` are exposed.

### Changing log levels at runtime

The `loggers` endpoint changes log levels in the running application, with no restart:

```sh
# Inspect a logger
curl localhost:8081/actuator/loggers/com.example.modulith.greeting

# Switch it to DEBUG (any logger name works, e.g. org.hibernate.SQL)
curl -X POST localhost:8081/actuator/loggers/com.example.modulith.greeting \
     -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'

# Reset it to the configured default
curl -X POST localhost:8081/actuator/loggers/com.example.modulith.greeting \
     -H 'Content-Type: application/json' -d '{}'
```

With `greeting` on DEBUG, every `GET /api/greetings` logs
`Greeting audience 'World' with template 'Hello, %s!'`. Changes last until the application restarts.
`GET localhost:8081/actuator/loggers` lists every logger.

### Swagger UI

[springdoc](https://springdoc.org) serves Swagger UI on the management port
(`springdoc.use-management-port=true`). It shows the **hand-written contract**,
`openapi/modulith-api.yaml`, not a spec derived from the code. The build copies the file onto the
classpath, `apidocs.web.ApiContractEndpoint` serves it at `/actuator/apicontract`, and
`springdoc.swagger-ui.url` points Swagger UI there. springdoc's own code-derived spec is not exposed:
`openapi` is missing from the exposure list on purpose.

By default, Swagger UI is **open without a login**. The management port only listens on `127.0.0.1`,
so only whoever is on this machine can reach it, and they have full control anyway.

To require a login, set `adminPassword`. Swagger UI and the contract then use **HTTP Basic**
authentication with the user `admin`:

```sh
java -DadminPassword=snakeoil -jar modulith.jar      # system property: -D must come before -jar
java -jar modulith.jar --adminPassword=snakeoil      # Spring Boot argument
ADMINPASSWORD=snakeoil java -jar modulith.jar        # environment variable; keeps the password out of `ps`
./mvnw -pl backend spring-boot:run -Ddev -Dspring-boot.run.arguments=--adminPassword=snakeoil
```

| | Management port on loopback (default) | Management port beyond loopback |
|---|---|---|
| `adminPassword` not set | **Open**, no login (logged at INFO) | **Closed**: 403, with a warning at startup |
| `adminPassword` set | Login required | Login required |

There is no default password. The "closed" case is a safeguard: if you set `management.server.address`
to something other than loopback (for example `0.0.0.0` in a container), Swagger UI doesn't become
reachable from the network without a password by accident.

An empty password (`--adminPassword=`) fails startup, because it is almost always a mistake, such as
an unset variable in a start script. The password is held only as a bcrypt hash in memory.

`java -jar modulith.jar -DadminPassword=...` does **not** set the password. Everything after the jar
name is a program argument, not a JVM option. Swagger UI is then open without a login, and the
startup log says so.

"Try it out" is disabled (`springdoc.swagger-ui.supported-submit-methods=`). Swagger UI runs on 8081
and the API on 8080, so its requests would be cross-origin and need CORS on the API. To try requests
anyway, use curl or the frontend.

## API contract (OpenAPI)

The HTTP API is contract-first: [`openapi/modulith-api.yaml`](openapi/modulith-api.yaml) is the single
source of truth, and both sides generate code from it during their normal build:

| Side | Generator | Output |
|------|-----------|--------|
| Backend | `openapi-generator-maven-plugin` (`spring`, interface only) | `GreetingsApi` and `GreetingDto` in `greeting.web.openapi` (under `backend/target/generated-sources`). `GreetingController` implements the interface |
| Frontend | [`@hey-api/openapi-ts`](https://heyapi.dev) via `npm run generate` (part of `build` and `watch`) | Typed fetch client in `frontend/src/api` (git-ignored), for example `getGreeting()` |

If the contract changes, the build fails wherever the code no longer matches. Renaming a field, for
example, breaks `tsc` in the frontend and the integration test in the backend.

In the backend, each component that serves HTTP gets its own plugin execution. That execution generates
only its own tag (`apisToGenerate`/`modelsToGenerate`) into its own `web` layer, so the generated code
stays inside the component's boundaries. After editing the spec, restart `npm run watch` to regenerate
the client.

### Design decisions

**Contract-first rather than code-first.** In a code-first setup, the controllers are the source of
truth, and [springdoc-openapi](https://springdoc.org) derives the spec from them, at runtime under
`/v3/api-docs` or at build time with `springdoc-openapi-maven-plugin`. That plugin starts the
application during `integration-test`. The frontend client is generated from that output.

| | Contract-first (chosen) | Code-first |
|---|---|---|
| Source of truth | `openapi/modulith-api.yaml` | Controller annotations |
| Build order | Spec → backend and frontend independently | Backend must build and start before the frontend can generate its client |
| Fit with the single jar | Works as is | Cycle: the backend embeds the frontend, but the frontend needs the backend's spec |
| API design | Reviewed as a document before implementation; frontend and backend can work in parallel | Spec follows the implementation, so the API emerges from the code |
| Effort | Write YAML by hand | Annotate controllers; the spec quality depends on the annotations |

Code-first is still a valid option if you prefer it. There are two ways to break the build cycle:

1. **Separate assembly module.** Split the backend into a library module with the code and the spec
   generation, and an `app` module. The build order becomes backend library → spec → frontend → `app`,
   and only `app` embeds the frontend and produces the executable jar.
2. **Committed spec snapshot.** Commit the generated spec to git, and generate the frontend from it.
   A backend test compares `/v3/api-docs` with the committed file and fails when they differ, so the
   snapshot must be updated with every API change.

**Interfaces only, no generated controller stubs.** The backend generates interfaces
(`interfaceOnly=true`) that carry all Spring MVC mappings, and hand-written controllers implement them.
The generated methods have no default body (`skipDefaultInterface=true`), so a new operation in the spec
breaks compilation until a controller implements it. Alternatives that were considered:

- **Full stubs (`interfaceOnly=false`).** Stubs generated on every build land in `target/` and are
  overwritten, so logic can't live in them. Stubs generated once into `src/` drift from the spec. In
  both cases, unimplemented operations compile and return `501 Not Implemented` instead of failing the
  build.
- **Delegate pattern (`delegatePattern=true`).** The generator owns a concrete `@RestController` that
  forwards to a `…ApiDelegate` interface you implement. This keeps Spring MVC annotations out of the
  hand-written class, at the cost of an extra indirection. Here the controller already lives in the
  `web` layer, where Spring web types are allowed, so it adds nothing.

Generating stubs once as scaffolding for a large new spec is fine as a developer convenience, but it
is not part of the build.

## How the bundling works

| Piece | What it does |
|-------|--------------|
| `frontend/pom.xml` | `frontend-maven-plugin` installs Node and runs `npm ci`, `lint` and `build`. Vite's `dist/` is packaged as `static/**` in `modulith-frontend.jar` |
| `backend/pom.xml` | Depends on that jar, so the build lands on `classpath:/static/`. `spring-boot-maven-plugin` repackages everything into `modulith.jar` |
| `frontend/web/SpaWebConfig` | A functional route that serves Spring Boot's configured static locations (`spring.web.resources.static-locations`). For extension-less paths outside `/api` (client-side routes) it falls back to `index.html`. Missing assets and unknown API paths stay 404. It is a `RouterFunction` rather than a `WebMvcConfigurer` because the management context inherits configurers but not router functions. That keeps the frontend off port 8081. Caching: Vite's content-hashed `/assets/**` get `max-age=1 year, immutable`. `index.html`, client routes and other files get `no-cache`, so a new deployment shows up immediately. The `dev` profile sends `no-store` everywhere |

## Architecture

```
com.example.modulith
├── ModulithApplication
├── <component>
│   ├── api    published contract: plain Java interfaces and records, the only thing other components may use
│   ├── core   business logic and the persistence ports (interfaces) it needs
│   ├── data   JPA entities, Spring Data repositories, adapters implementing the core ports
│   └── web    REST controllers
```

Example components:

- `audience` (api, core, data): provides the name "World" through `AudienceApi`.
- `greeting` (core, data, web): loads the template "Hello, %s!" and calls `AudienceApi`. Serves
  `GET /api/greetings` by implementing the generated `GreetingsApi`.
- `frontend` (web): serves the React app.
- `apidocs` (web): serves the API contract to Swagger UI on the management port.
- `security` (web): HTTP Basic for Swagger UI when `adminPassword` is set, everything else open.
- `desktop` (core, web): detects a local desktop session and opens the browser at startup.

Core never depends on data. Instead, data implements interfaces that core owns (dependency inversion),
so JPA stays out of the business logic.

### Enforced rules

`backend/src/test/java/com/example/modulith/architecture/ModulithRules.java`, run by `ArchitectureTest`:

| Rule | Meaning |
|------|---------|
| `classesResideInComponentLayers` | Every class lives in `<component>.(api\|core\|data\|web)` |
| `layerDependencies` | web → core → api; data → core; nothing may depend on web or data |
| `persistenceOnlyInDataLayer` | `jakarta.persistence`, Hibernate, Spring Data/ORM/JDBC only in `data` |
| `webOnlyInWebLayer` | Spring Web/MVC, `org.springframework.http`, Servlet API only in `web` |
| `apiIsPlainJava` | `api` packages depend only on `java..` and other `api` packages |
| `componentsOnlyAccessEachOtherThroughApi` | Cross-component dependencies must target the other component's `api` |
| `componentsAreFreeOfCycles` | No dependency cycles between components |

`ModulithRulesTest` runs every rule against the deliberately broken fixtures in
`src/test/java/com/example/archfixtures`. Each fixture must break exactly its own rule and no other.
This shows that the rules really catch violations rather than passing trivially.

### Why not JPMS (Jigsaw)?

JPMS draws boundaries per jar (one `module-info.java` per artifact). Enforcing these per-component,
per-layer boundaries inside one Spring Boot application would mean one Maven module per
component × layer. Spring Boot's executable jar runs on the class path anyway. Spring and Hibernate
also need reflective `opens` everywhere. ArchUnit enforces the same boundaries at the package level
in a single module.

If you want compile-time enforcement of the "no JPA in core" rule, split the layers into separate
Maven modules. The `core` module then cannot see JPA on its class path at all.

## Adding a component

1. Create `com.example.modulith.<name>` with the layers you need.
2. Put anything other components should call into `<name>.api`.
3. If it serves HTTP: add its endpoints to `openapi/modulith-api.yaml` under a new tag. Then add an
   `openapi-generator` execution in `backend/pom.xml` that targets `com.example.modulith.<name>.web.openapi`.
4. Run `./mvnw verify`. The architecture tests fail on any violation.

To rename the base package, move `com.example.modulith`. The rules derive the root from
`ModulithApplication`'s package.
