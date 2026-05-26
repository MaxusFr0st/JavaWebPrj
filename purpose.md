# JavaWebPrj — Spring Boot Study Guide

**Project:** `hr.algebra:JavaWebPrj` — e-commerce web shop (Thymeleaf MVC + REST API + JWT + PayPal + Railway PostgreSQL)  
**Stack:** Spring Boot 4.0.6, Java 25, Spring Security 6, Spring Data JPA, Thymeleaf, PostgreSQL, PayPal Server SDK, JJWT  
**Package root:** `hr.algebra.javawebprj` — **76 Java compilation units** (documented in §8)

---

## Table of Contents

1. [Infrastructure Overview](#1-infrastructure-overview)
2. [Spring Annotations Reference](#2-spring-annotations-reference)
3. [Request Flow Diagrams](#3-request-flow-diagrams)
4. [Configuration Profiles](#4-configuration-profiles)
5. [Railway Deployment](#5-railway-deployment)
6. [PayPal Integration](#6-paypal-integration)
7. [Templates and Static Assets](#7-templates-and-static-assets)
8. [Class Reference (All 76 Types)](#8-class-reference-all-76-types)
   - [8.1 Root](#81-roothralgebrajavawebprj)
   - [8.2 api](#82-api)
   - [8.3 config](#83-config)
   - [8.4 controller](#84-controller)
   - [8.5 controller.api](#85-controllerapi)
   - [8.6 dto](#86-dto)
   - [8.7 exception](#87-exception)
   - [8.8 model](#88-model)
   - [8.9 repository](#89-repository)
   - [8.10 security](#810-security)
   - [8.11 service](#811-service)
   - [8.12 web](#812-web)

---

## 1. Infrastructure Overview

Spring Boot applications are organized in **layers**. Each package in this project maps to one layer or cross-cutting concern. Data flows **down** from the web layer to persistence and **up** through services that enforce business rules.

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Browser / HTTP client                                                  │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ controller    │     │ controller.api  │     │ security        │
│ (MVC pages)   │     │ (REST JSON)     │     │ (filters/events)│
└───────┬───────┘     └────────┬────────┘     └────────┬────────┘
        │                      │                       │
        │         ┌────────────┴────────────┐          │
        │         ▼                         ▼          │
        │    ┌─────────┐              ┌──────────┐     │
        └───►│  dto    │◄─────────────│   web    │     │
             └────┬────┘              │ (advice) │     │
                  │                   └──────────┘     │
                  ▼                                    │
             ┌─────────┐                               │
             │ service │◄──────────────────────────────┘
             └────┬────┘
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
┌───────────────┐   ┌───────────────┐
│  repository   │   │  exception    │
└───────┬───────┘   └───────────────┘
        │
        ▼
┌───────────────┐
│    model      │  ← JPA @Entity (database tables)
└───────────────┘

┌───────────────┐   ┌───────────────┐
│    config     │   │     api       │  ← startup, beans, env (not request path)
└───────────────┘   └───────────────┘
```

### Package: `hr.algebra.javawebprj` (root)

| Class | Layer | Why it exists |
|-------|-------|---------------|
| `JavaWebPrjApplication` | Bootstrap | Entry point; registers PayPal properties and Railway DB initializer before context starts |

### Package: `config`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `SecurityConfig`, `AsyncConfig`, `PayPalConfig`, `DataInitializer`, seeders, validators | **Infrastructure / cross-cutting** | Wire Spring beans: security filter chain, PayPal SDK client, async execution, demo data, production guards |
| `RailwayDatabase*` + `PayPalProperties` | **Infrastructure** | Map Railway/Postgres env vars to `spring.datasource.*` before JPA connects; bind PayPal credentials |

**Not in the HTTP request path** — they run at application startup or configure the servlet container.

### Package: `controller`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `HomeController`, `ShopController`, `CartController`, `AuthController`, `CheckoutController`, `AccountController`, `AdminController` | **Presentation (MVC)** | Handle browser requests; return Thymeleaf view names or redirects |
| `CartSseController`, `PayPalCheckoutController`, `HealthController` | **Presentation (REST fragment)** | SSE stream, PayPal JSON API, health probe |

Controllers stay thin: validate input, call services, populate `Model`, choose view/redirect.

### Package: `controller.api`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `AuthRestController`, `ProductRestController` | **Presentation (REST)** | JSON API for JWT login and product catalog; consumed by tools/mobile, not Thymeleaf |

### Package: `service`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `CartService`, `OrderService`, `ProductService`, … | **Business logic** | Transactions, stock checks, cart merge, order placement, PayPal order creation |
| `JwtService`, `AuditService`, `CartSseService` | **Business / technical services** | Token lifecycle, async audit writes, in-memory SSE fan-out |

Single place for rules like “cannot order more than stock” or “merge anonymous cart on login”.

### Package: `repository`

| Interfaces | Layer | Why they exist |
|------------|-------|----------------|
| `UserRepository`, `CartRepository`, `OrderRepository`, … | **Persistence (DAO)** | Spring Data JPA queries; no business rules |

### Package: `model`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `User`, `Product`, `Cart`, `Order`, … | **Domain / persistence** | JPA entities mapped to PostgreSQL tables |
| `Role`, `PaymentMethod` | **Domain enums** | Stored as strings in DB |

### Package: `dto`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `ProductDto`, `CartSummary`, `RegisterForm`, … | **Transfer objects** | Safe shapes for views/API/forms; decouple entities from HTML/JSON and validation |

### Package: `security`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `JwtAuthFilter`, `UserDetailsServiceImpl`, listeners | **Security cross-cutting** | Authenticate REST (`Bearer` JWT) and form login; merge cart; audit logins |
| `RequestContextHelper` | **Utility** | Access current HTTP session/IP from non-controller code |

### Package: `exception`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `ResourceNotFoundException`, handlers | **Cross-cutting error handling** | Map domain “not found” to 404 HTML or JSON |

### Package: `web`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `MvcConstants`, `CartModelAdvice` | **MVC support** | Shared view names/paths; global `cartItemCount` in navbar |

### Package: `api`

| Classes | Layer | Why they exist |
|---------|-------|----------------|
| `ApiConstants` | **Shared constants** | JSON error field name for REST (`"error"`) |

---

## 2. Spring Annotations Reference

For each annotation: **what it is**, **why this project needs it**, **which layer**.

### Core stereotype & configuration

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@SpringBootApplication` | Meta-annotation: `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` | Boots the shop app and auto-configures Data JPA, Security, Thymeleaf | Bootstrap |
| `@EnableConfigurationProperties(PayPalProperties.class)` | Registers `@ConfigurationProperties` type as a bean | Binds `paypal.*` / env vars to typed PayPal settings | config |
| `@Configuration` | Class defines `@Bean` methods processed by Spring | `SecurityConfig`, `PayPalConfig`, `DataInitializer`, `AsyncConfig` | config |
| `@Bean` | Method return value becomes a singleton bean | `SecurityFilterChain`, `PasswordEncoder`, `PaypalServerSdkClient`, `CommandLineRunner` seeders | config |
| `@Component` | Generic stereotype bean | `AdminAccountSeeder`, `PayPalStartupLogger`, `UserDataRepairRunner`, security listeners | config / security |
| `@Service` | Stereotype for business logic | All classes in `service` package; `UserDetailsServiceImpl` | service / security |
| `@Repository` | Stereotype for persistence (via Spring Data) | Implicit on `JpaRepository` interfaces | repository |
| `@Controller` | MVC controller; return value = view name | Page controllers (`ShopController`, `AdminController`, …) | controller |
| `@RestController` | `@Controller` + `@ResponseBody` on all methods | `HealthController`, `CartSseController`, `PayPalCheckoutController`, `controller.api.*` | controller |
| `@ControllerAdvice` | Global MVC advice (e.g. `@ModelAttribute`, `@ExceptionHandler`) | `CartModelAdvice` adds cart badge count to every page | web |
| `@RestControllerAdvice` | `@ControllerAdvice` limited to REST controllers | `RestExceptionHandler` for `/api/**` JSON errors | exception |

### Web mapping & binding

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@RequestMapping` | Base path for controller | `/shop`, `/cart`, `/admin`, `/api/products` | controller |
| `@GetMapping` / `@PostMapping` | HTTP method + path | Every route (cart add, checkout COD, PayPal capture) | controller |
| `@PathVariable` | URI template variable | `{productId}`, `{orderId}`, PayPal `{orderId}` | controller |
| `@RequestParam` | Query or form parameter | Cart quantity, product list `categoryId` filter | controller |
| `@RequestBody` | Deserialize JSON body | `AuthRequest` on `/api/auth/login` | controller |
| `@ModelAttribute` | Form object binding / global model | `RegisterForm`, `ProductForm`, `OrderFilterForm`, `cartItemCount` | controller / web |
| `@ResponseBody` | Write return value to HTTP body | `CartController.itemCount()` JSON map | controller |
| `@Valid` | Trigger Bean Validation on object | Admin forms, registration | controller |
| `BindingResult` | Validation errors (not annotation) | Re-show form with errors in admin/register | controller |

### Security

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@EnableWebSecurity` | Enables Spring Security filter chain | Form login for browser + JWT filter for API | config |
| `@Profile("prod")` | Bean active only when profile `prod` | `ProductionStartupValidator` — fail fast on weak JWT in production | config |
| `@Order(n)` | `CommandLineRunner` execution order | Admin seed (1) after role repair (0) | config |
| `@ConditionalOnProperty` | Bean/runner only if property set | Admin seeder, catalog seed, demo customer, PayPal client bean | config |
| `@Value("${...}")` | Inject property | JWT secret, seed passwords, customer username | config / service |
| `@RequiredArgsConstructor` (Lombok) | Constructor for `final` fields | Constructor injection everywhere (preferred over `@Autowired` field) | all layers |

*Thymeleaf security:* `xmlns:sec` + `sec:authorize` in `layout.html` — not Java annotations but uses `hasRole('ADMIN')` from `SecurityConfig` rules.

### Persistence & transactions

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@Entity` | JPA persistent class | `User`, `Product`, `Cart`, `Order`, … | model |
| `@Table` | DB table name | `users`, `orders`, `cart_items`, … | model |
| `@Id` / `@GeneratedValue` | Primary key | All entities | model |
| `@Column` | Column mapping | `session_id`, `paypal_order_id`, unique constraints | model |
| `@ManyToOne` / `@OneToMany` / `@OneToOne` | Associations | Cart↔items, Order↔user, Product↔category | model |
| `@JoinColumn` | FK column name | `user_id`, `cart_id`, `category_id` | model |
| `@Enumerated(STRING)` | Enum stored as string | `Role`, `PaymentMethod` | model |
| `@Transactional` | Declarative transactions | `CartService`, `OrderService`, `UserDataRepairRunner.run` | service / config |
| `@Transactional(readOnly = true)` | Read-only optimization | List/find methods in services | service |
| `@Query` | Custom JPQL | Eager-fetch cart items, admin order search | repository |
| `@Param` | Named query parameter | `:sessionId`, `:username` in `@Query` | repository |

### Validation (Jakarta)

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@NotBlank`, `@Size`, `@Email`, `@NotNull`, `@Min` | Bean Validation constraints | `RegisterForm`, `ProductForm`, `CategoryForm` | dto |

### Async & events

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@EnableAsync` | Enables `@Async` methods | `AuditService.recordLoginAsync` does not block login response | config |
| `@Async` | Method runs on thread pool | Login audit persistence | service |
| `@EventListener` | Handle application events | `ProductionStartupValidator` on `ApplicationReadyEvent` | config |
| `ApplicationListener<T>` | Interface-based event listener | `AuthSuccessListener`, `CartMergeOnLoginListener` on `AuthenticationSuccessEvent` | security |

### Boot extension points (non-annotation types)

| Type | What it is | Why THIS project needs it | Layer |
|------|------------|---------------------------|-------|
| `EnvironmentPostProcessor` | Mutate `Environment` before context refresh | `RailwayDatabaseEnvironmentPostProcessor` sets JDBC URL from `DATABASE_URL` | config |
| `ApplicationContextInitializer` | Early context setup | `RailwayDatabaseEnvironmentInitializer` (same DB logic, called from `main`) | config |
| `CommandLineRunner` | Run code after context started | Seed admin/catalog, PayPal startup log, user repair | config |
| `OncePerRequestFilter` | Servlet filter base class | `JwtAuthFilter` parses `Authorization: Bearer` | security |

### Lombok (compile-time)

| Annotation | What it is | Why THIS project needs it | Layer |
|------------|------------|---------------------------|-------|
| `@Getter` / `@Setter` | Accessors | Entities, DTOs, forms | model / dto |
| `@Builder` | Builder pattern | Entity/DTO construction in services | model / dto |
| `@Slf4j` | Logger field `log` | Config runners, PayPal logger, audit | config / service |
| `@AllArgsConstructor` / `@NoArgsConstructor` | Constructors | JPA requires no-arg constructor on entities | model |

---

## 3. Request Flow Diagrams

### 3.1 Browser form login (session cookie)

```
Browser                    SecurityFilterChain           UserDetailsServiceImpl      AuditService
   |                              |                              |                      |
   | POST /login                  |                              |                      |
   | username, password           |                              |                      |
   |----------------------------->|                              |                      |
   |                              | DaoAuthenticationProvider    |                      |
   |                              | loadUserByUsername()         |                      |
   |                              |----------------------------->|                      |
   |                              | UserDetails + BCrypt check   |                      |
   |                              |<-----------------------------|                      |
   |                              | AuthenticationSuccessEvent   |                      |
   |                              |----------------------------+--+------------------>|
   |                              |                            |  | recordLoginAsync   |
   |                              | CartMergeOnLoginListener     |  | (async)            |
   |                              | mergeSessionCartOnLogin()    |                      |
   |                              |                            |                        |
   | 302 redirect /               |                              |                      |
   | Set-Cookie: JSESSIONID       |                              |                      |
   |<-----------------------------|                              |                      |
```

**Key classes:** `SecurityConfig` (form login URLs), `UserDetailsServiceImpl`, `AuthSuccessListener`, `CartMergeOnLoginListener`, `CartService.mergeSessionCartOnLogin`.

**Authorization after login:** `SecurityContext` stored in session; Thymeleaf `sec:authorize` reads roles; `/checkout/**` requires `ROLE_USER` or `ROLE_ADMIN`.

---

### 3.2 REST JWT login and authenticated API call

```
Client                 AuthRestController    AuthenticationManager    JwtService    JwtAuthFilter    ProductRestController
  |                          |                      |                  |              |                    |
  | POST /api/auth/login     |                      |                  |              |                    |
  | {username,password}      |                      |                  |              |                    |
  |------------------------->| authenticate()       |                  |              |                    |
  |                          |--------------------->|                  |              |                    |
  |                          | generateToken()      |                  |              |                    |
  |                          |----------------------------------------->|              |                    |
  |<-------------------------| {token: "eyJ..."}    |                  |              |                    |
  |                          |                      |                  |              |                    |
  | GET /api/products        |                      |                  |              |                    |
  | Authorization: Bearer eyJ|                      |                  |              |                    |
  |------------------------------------------------------------------------>|                    |
  |                          |                      |                  | extractUsername|
  |                          |                      |                  | loadUserDetails|
  |                          |                      |                  | set SecurityContext
  |                          |                      |                  |              |------------------>|
  |                          |                      |                  |              | ProductService  |
  |<------------------------------------------------------------------------| JSON List<ProductDto>
```

**Security rule:** `/api/auth/**` permitAll; `/api/**` authenticated (`SecurityConfig`).

---

### 3.3 Cart SSE (live navbar badge)

```
Browser (layout.html)     cart.js              CartSseController      CartSseService         CartService
      |                      |                        |                    |                    |
      | EventSource /cart/sse|                        |                    |                    |
      |--------------------->| GET /cart/sse          |                    |                    |
      |                      |----------------------->| subscribe()        |                    |
      |                      |                        | getTotalItemCount()|------------------>|
      |                      |                        | SseEmitter created |                    |
      |<----- event: cart-count, data: N ------------|                    |                    |
      |                      |                        |                    |                    |
 [User adds to cart]         |                        |                    |                    |
      | POST /cart/add       |                        |                    |                    |
      |                      |                        | publishCount()   |                    |
      |<----- event: cart-count, data: N+1 ----------|                    |                    |
```

**Fallback:** If SSE errors, `cart.js` polls `GET /cart/count` JSON.

**Note:** Emitters keyed by **HTTP session id**; logged-in users still use session id for SSE while cart data may be stored per user in DB.

---

### 3.4 PayPal checkout

```
Browser (checkout.html)   paypal-checkout.js   PayPalCheckoutController   PayPalService      OrderService
        |                        |                        |                      |                  |
        | Load PayPal JS SDK     |                        |                      |                  |
        | (client-id from server)|                        |                      |                  |
        | User clicks PayPal     |                        |                      |                  |
        | createOrder()          | POST /checkout/paypal/orders                 |                  |
        |----------------------->|----------------------->| createOrder(cart)    |                  |
        |                        |                        |--------------------->| PayPal API       |
        |<-----------------------|<-----------------------| {id: paypalOrderId}  |                  |
        | PayPal popup approve   |                        |                      |                  |
        | onApprove()            | POST .../orders/{id}/capture               |                  |
        |----------------------->|----------------------->| captureOrder()       |                  |
        |                        |                        |--------------------->|                  |
        |                        |                        | placeOrder(PAYPAL)   |----------------->|
        |                        |                        | clearCart, decrement stock              |
        | redirect /checkout/confirm/{shopOrderId}       |                      |                  |
        |<-----------------------|<-----------------------|                      |                  |
```

**COD path (no PayPal):** `CheckoutController.cashOnDelivery` → `OrderService.placeOrder(COD)` → redirect confirm page.

**Guards:** `PayPalService.isServerReady()` requires configured credentials **and** `PaypalServerSdkClient` bean.

---

### 3.5 Railway startup (database before JPA)

```
Railway platform          JavaWebPrjApplication.main          RailwayDatabaseEnvironmentInitializer
      |                              |                                      |
      | env: DATABASE_URL,           | addInitializers(...)                 |
      | PGHOST, PORT, ...            |------------------------------------->|
      |                              | applyIfNeeded(environment)           |
      |                              |   parse postgres:// → jdbc:postgresql://
      |                              |   addFirst MapPropertySource           |
      |                              | SpringApplication.run()              |
      |                              | EnvironmentPostProcessor (META-INF)  |
      |                              |   (same applyIfNeeded — idempotent   |
      |                              |    if JDBC URL already set, skip)    |
      |                              | DataSource auto-config               |
      |                              | JPA ddl-auto=validate (prod)         |
      |                              | AdminAccountSeeder, PayPalStartupLogger
      | GET /health                  |                                      |
```

**Deploy:** `railway.toml` health check `GET /health` → `HealthController`.

---

## 4. Configuration Profiles

### 4.1 File layout

| File | Location | Git | Active when |
|------|----------|-----|-------------|
| `application.properties` | `src/main/resources/` | Yes | Always (base) |
| `application-prod.properties` | `src/main/resources/` | Yes | `spring.profiles.active=prod` |
| `application-local.properties` | **project root** | **No** (gitignored) | `spring.profiles.active=local` (default) |

Default profile from base file:

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

### 4.2 `application.properties` (shared)

- Application name
- Default profile `local` unless `SPRING_PROFILES_ACTIVE` set (Railway sets `prod`)
- JWT expiration default `3600000` ms (1 hour)
- PayPal mode/currency defaults
- Seed flags: catalog on by default; demo users off by default; admin username/email placeholders

**Does not contain:** datasource URL, JWT secret, PayPal secrets (those are profile-specific or env).

### 4.3 `application-prod.properties` (Railway / production)

- `server.port=${PORT:8080}` — Railway injects `PORT`
- Forward headers, secure session cookies, hide error details
- PostgreSQL driver; `ddl-auto` default **validate** (schema must exist)
- Thymeleaf cache **on**
- Demo user seed **disabled**
- PayPal `client-id` / `client-secret` from env `PAYPAL_*`

**Expects env vars:** `APP_JWT_SECRET`, optional `APP_SEED_ADMIN_PASSWORD`, `DATABASE_URL` (via Railway processor), PayPal credentials.

### 4.4 `application-local.properties` pattern (developer machine)

Typical local overrides (your file is gitignored):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/javawebprj
spring.datasource.username=postgres
spring.datasource.password=...

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.thymeleaf.cache=false

app.jwt.secret=<base64 or long secret>
app.seed.users.enabled=true
app.seed.admin.password=...
app.seed.customer.password=...
paypal.client-id=...
paypal.client-secret=...
```

**Why separate file:** Secrets and machine-specific DB never committed; `ddl-auto=update` convenient for development; SQL logging on.

### 4.5 Property → environment variable mapping (Railway)

Spring relaxed binding: `app.jwt.secret` ↔ `APP_JWT_SECRET`, `paypal.client-id` ↔ `PAYPAL_CLIENT_ID`.

| Property | Env var (typical) |
|----------|-------------------|
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE=prod` |
| `app.jwt.secret` | `APP_JWT_SECRET` |
| `app.seed.admin.password` | `APP_SEED_ADMIN_PASSWORD` |
| `paypal.client-id` | `PAYPAL_CLIENT_ID` |
| `paypal.client-secret` | `PAYPAL_CLIENT_SECRET` |
| `paypal.mode` | `PAYPAL_MODE` |
| JDBC URL | `DATABASE_URL` (auto-mapped by Railway support) |

---

## 5. Railway Deployment

### 5.1 `railway.toml`

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
healthcheckPath = "/health"
healthcheckTimeout = 120
restartPolicyType = "ON_FAILURE"
```

Railway builds from `Dockerfile`, waits up to 120s for `GET /health` returning `{"status":"UP"}`.

### 5.2 Database URL resolution (`RailwayDatabaseConfigSupport`)

Runs **before** DataSource creation if `spring.datasource.url` is not already set:

1. Try `DATABASE_URL` or `DATABASE_PUBLIC_URL` (`postgres://` or `postgresql://`)
2. Else build from `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
3. Parse URL → `jdbc:postgresql://host:port/db?sslmode=require` (except `railway.internal` host)
4. Insert `MapPropertySource` named `railwayDatabaseUrl` with **highest precedence** (`addFirst`)

**Unresolved reference detection:** Values containing `${{` (Railway template not linked) are ignored; prod profile logs hints to stderr.

### 5.3 Dual registration (initializer + post-processor)

| Mechanism | Registration |
|-----------|--------------|
| `RailwayDatabaseEnvironmentInitializer` | `JavaWebPrjApplication.main` → `app.addInitializers(...)` |
| `RailwayDatabaseEnvironmentPostProcessor` | `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports` and `META-INF/spring.factories` |

Both call `RailwayDatabaseConfigSupport.applyIfNeeded` — safe to call twice (no-op if JDBC URL already present).

### 5.4 Production safeguards

- `ProductionStartupValidator` (`@Profile("prod")`): JWT secret length ≥ 32, not sample text; admin seed password ≥ 12 if set
- `spring.jpa.hibernate.ddl-auto=validate` — Flyway/manual schema expected
- `SecurityConfig` prod profile: HSTS, CSP allowing PayPal CDN domains

---

## 6. PayPal Integration

### 6.1 Configuration types

| Class | Role |
|-------|------|
| `PayPalProperties` | `@ConfigurationProperties(prefix="paypal")` — clientId, clientSecret, mode, currency; placeholder detection |
| `PayPalConfig` | Creates `PaypalServerSdkClient` bean when id+secret properties exist |
| `PayPalOAuthVerifier` | Static HTTP client_credentials test at startup |
| `PayPalStartupLogger` | Logs config status, SDK presence, OAuth result |
| `PayPalService` | `createOrder`, `captureOrder`, JS SDK URL builder |
| `PayPalCheckoutController` | REST endpoints for browser JS |

### 6.2 Modes

- `paypal.mode=sandbox` (default) → SDK `Environment.SANDBOX`, OAuth to `api-m.sandbox.paypal.com`
- `paypal.mode=live` → production API hosts

Client ID and secret **must be from the same PayPal app** and match mode.

### 6.3 Checkout page wiring

`CheckoutController.checkoutPage` adds to model:

- `paypalEnabled`, `paypalServerReady`, `paypalSdkUrl`, `paypalCurrency`

Thymeleaf loads SDK script from `paypalSdkUrl`; `paypal-checkout.js` calls backend create/capture.

### 6.4 Order lifecycle

1. **Create** — `PayPalService.createOrder(CartSummary)` → PayPal order id
2. **Approve** — User in PayPal UI (browser)
3. **Capture** — `PayPalService.captureOrder(id)` → status must be `COMPLETED`
4. **Persist** — `OrderService.placeOrder(session, PAYPAL, paypalOrderId)` — saves shop order, reduces stock, clears cart

Cash on delivery skips PayPal entirely (`PaymentMethod.COD`).

---

## 7. Templates and Static Assets

### 7.1 Thymeleaf layout

**`templates/layout.html`** — fragments:

| Fragment | Purpose |
|----------|---------|
| `head(title)` | Bootstrap 5.3 CSS, `app.css`, page title |
| `navbar` | Shop links, cart badge `#cart-badge`, sec:authorize login/admin/logout |
| `footer` | Project footer |
| `scripts` | Bootstrap JS + **`cart.js`** (SSE badge) |

Pages use: `th:replace="~{layout :: head(...)}"` pattern.

### 7.2 Templates by area

| Template | Controller / route | Purpose |
|----------|-------------------|---------|
| `index.html` | `GET /` | Home |
| `login.html` | `GET /login` | Form login (posts to same URL) |
| `register.html` | `/register` | User registration |
| `shop/categories.html` | `/shop/categories` | Category list |
| `shop/products.html` | `/shop/categories/{id}/products` | Products in category |
| `shop/product-detail.html` | `/shop/products/{id}` | Add to cart form |
| `cart/cart.html` | `/cart` | Line items, update/remove |
| `checkout/checkout.html` | `/checkout` | COD form + PayPal buttons |
| `checkout/order-confirm.html` | `/checkout/confirm/{id}` | Thank you / order summary |
| `account/orders.html` | `/account/orders` | User order history |
| `admin/dashboard.html` | `/admin` | Admin home |
| `admin/categories.html`, `category-form.html` | CRUD categories | |
| `admin/products.html`, `product-form.html` | CRUD products | |
| `admin/orders.html` | `/admin/orders` | Filterable all orders |
| `admin/login-audit.html` | `/admin/login-audit` | Login audit table |
| `admin/fragments/nav.html` | included in admin pages | Admin sub-nav |
| `error/not-found.html` | `GlobalExceptionHandler` | 404 page |

### 7.3 Static JavaScript

**`static/js/cart.js`**

- Targets `#cart-badge` in navbar
- Opens `EventSource('/cart/sse')`, listens for `cart-count` events
- On SSE error: fallback `fetch('/cart/count')`
- IIFE — no global exports

**`static/js/paypal-checkout.js`**

- Defines `window.initPayPalCheckout()`
- Renders `paypal.Buttons` in `#paypal-button-container`
- `createOrder`: `POST /checkout/paypal/orders`
- `onApprove`: `POST /checkout/paypal/orders/{orderID}/capture` → redirect to `redirectUrl`
- User-facing errors in `#paypal-result-message`

### 7.4 Static CSS

**`static/css/app.css`** — flex column layout so footer stays at bottom; minimal shop styling.

### 7.5 External CDN (prod CSP allows)

- Bootstrap 5.3.3 (css/js) — `cdn.jsdelivr.net`
- PayPal JS SDK — `paypal.com` / sandbox hosts (dynamic URL from server)

---

## 8. Class Reference (All 76 Types)

Convention for each entry: **Purpose** | **Extends/implements** | **Key interactions** | **Key methods** | **Annotations**

---

### 8.1 Root (`hr.algebra.javawebprj`)

#### `JavaWebPrjApplication`

| | |
|---|---|
| **Purpose** | Spring Boot entry point |
| **Extends/implements** | — |
| **Interactions** | Registers `RailwayDatabaseEnvironmentInitializer`; enables `PayPalProperties` |
| **Key methods** | `main(String[] args)` — `SpringApplication` + initializer + `run` |
| **Annotations** | `@SpringBootApplication`, `@EnableConfigurationProperties(PayPalProperties.class)` |

---

### 8.2 `api`

#### `ApiConstants`

| | |
|---|---|
| **Purpose** | Shared REST JSON field names |
| **Extends/implements** | `final` utility class |
| **Interactions** | `RestExceptionHandler`, `PayPalCheckoutController` error bodies |
| **Key members** | `JSON_ERROR = "error"` |
| **Annotations** | none (private constructor) |

---

### 8.3 `config`

#### `AdminAccountSeeder`

| | |
|---|---|
| **Purpose** | Create or fix admin user on startup when password property set |
| **Extends/implements** | `CommandLineRunner` |
| **Interactions** | `UserRepository`, `PasswordEncoder` |
| **Key methods** | `run()` — upsert admin with `ROLE_ADMIN` |
| **Annotations** | `@Component`, `@Order(1)`, `@ConditionalOnProperty("app.seed.admin.password")`, `@Slf4j` |

#### `AsyncConfig`

| | |
|---|---|
| **Purpose** | Enable asynchronous method execution |
| **Extends/implements** | — |
| **Interactions** | `AuditService.@Async` |
| **Key methods** | — |
| **Annotations** | `@Configuration`, `@EnableAsync` |

#### `DataInitializer`

| | |
|---|---|
| **Purpose** | Seed demo catalog and optional demo customer |
| **Extends/implements** | — |
| **Interactions** | `CategoryRepository`, `ProductRepository`, `UserRepository`, `PasswordEncoder` |
| **Key methods** | `@Bean seedCatalog()`, `@Bean seedDemoCustomer()` |
| **Annotations** | `@Configuration`, `@Bean`, `@Order(2/3)`, `@ConditionalOnProperty` |

#### `PayPalConfig`

| | |
|---|---|
| **Purpose** | Construct PayPal Server SDK client bean |
| **Extends/implements** | — |
| **Interactions** | `PayPalProperties` → `PaypalServerSdkClient` |
| **Key methods** | `paypalServerSdkClient()` |
| **Annotations** | `@Configuration`, `@Bean`, `@ConditionalOnProperty` (client-id and client-secret) |

#### `PayPalOAuthVerifier`

| | |
|---|---|
| **Purpose** | Verify credentials via OAuth2 client_credentials (startup diagnostic) |
| **Extends/implements** | `final` utility |
| **Interactions** | Called by `PayPalStartupLogger` |
| **Key methods** | `verify(PayPalProperties)` → `null` if OK, else error message |
| **Annotations** | none |

#### `PayPalProperties`

| | |
|---|---|
| **Purpose** | Type-safe PayPal configuration + placeholder validation |
| **Extends/implements** | — |
| **Interactions** | `PayPalConfig`, `PayPalService`, startup logger |
| **Key methods** | `isConfigured()`, `hasValidClientId()`, setters trim/normalize |
| **Annotations** | `@ConfigurationProperties(prefix = "paypal")`, `@Getter`, `@Setter` |

#### `PayPalStartupLogger`

| | |
|---|---|
| **Purpose** | Log PayPal configuration health at startup |
| **Extends/implements** | `CommandLineRunner` |
| **Interactions** | `PayPalProperties`, `ObjectProvider<PaypalServerSdkClient>`, `PayPalOAuthVerifier` |
| **Key methods** | `run()` |
| **Annotations** | `@Component`, `@Slf4j` |

#### `ProductionStartupValidator`

| | |
|---|---|
| **Purpose** | Fail application start in prod if JWT/admin secrets weak |
| **Extends/implements** | — |
| **Interactions** | Reads `app.jwt.secret`, `app.seed.admin.password` |
| **Key methods** | `validate()` on `ApplicationReadyEvent` |
| **Annotations** | `@Component`, `@Profile("prod")`, `@EventListener` |

#### `RailwayDatabaseConfigSupport`

| | |
|---|---|
| **Purpose** | Parse Railway Postgres env into Spring datasource properties |
| **Extends/implements** | package-private `final` class; record `ParsedPostgres` |
| **Interactions** | Used by initializer and post-processor |
| **Key methods** | `applyIfNeeded`, `parsePostgresUrl`, `resolvePostgresUrl` |
| **Annotations** | none |

#### `RailwayDatabaseEnvironmentInitializer`

| | |
|---|---|
| **Purpose** | Earliest JDBC URL setup from `main()` |
| **Extends/implements** | `ApplicationContextInitializer<ConfigurableApplicationContext>` |
| **Interactions** | `RailwayDatabaseConfigSupport` |
| **Key methods** | `initialize()` |
| **Annotations** | none |

#### `RailwayDatabaseEnvironmentPostProcessor`

| | |
|---|---|
| **Purpose** | Same DB setup via Boot SPI (before beans) |
| **Extends/implements** | `EnvironmentPostProcessor` |
| **Interactions** | `RailwayDatabaseConfigSupport` |
| **Key methods** | `postProcessEnvironment()` |
| **Annotations** | none (registered in META-INF) |

#### `SecurityConfig`

| | |
|---|---|
| **Purpose** | HTTP security: form login, JWT filter, URL rules, prod headers |
| **Extends/implements** | — |
| **Interactions** | `JwtAuthFilter`, `UserDetailsService`, `MvcConstants` |
| **Key methods** | `filterChain()`, `passwordEncoder()`, `authenticationProvider()`, `loginFailureHandler()` |
| **Annotations** | `@Configuration`, `@EnableWebSecurity`, `@Bean` |

#### `UserDataRepairRunner`

| | |
|---|---|
| **Purpose** | Fix legacy role strings and non-BCrypt demo passwords when seeding enabled |
| **Extends/implements** | `CommandLineRunner` |
| **Interactions** | `EntityManager` (native SQL), `UserRepository`, `PasswordEncoder` |
| **Key methods** | `run()` — `fixLegacyRoles`, `fixDemoCustomerPassword`, `warnNonBcryptPasswords` |
| **Annotations** | `@Component`, `@Order(0)`, `@Transactional`, `@ConditionalOnProperty`, `@Slf4j` |

---

### 8.4 `controller`

#### `AccountController`

| | |
|---|---|
| **Purpose** | Logged-in user account pages |
| **Extends/implements** | — |
| **Interactions** | `OrderService` |
| **Key methods** | `orderHistory()` → `account/orders` |
| **Annotations** | `@Controller`, `@RequestMapping("/account")`, `@RequiredArgsConstructor` |

#### `AdminController`

| | |
|---|---|
| **Purpose** | Admin CRUD for categories/products, orders search, login audit |
| **Extends/implements** | — |
| **Interactions** | `CategoryService`, `ProductService`, `LoginAuditService`, `AdminOrderService` |
| **Key methods** | Full CRUD GET/POST for categories and products; `orders()`, `loginAudit()` |
| **Annotations** | `@Controller`, `@RequestMapping("/admin")` |

#### `AuthController`

| | |
|---|---|
| **Purpose** | Login page view and user registration |
| **Extends/implements** | — |
| **Interactions** | `UserService` |
| **Key methods** | `login()`, `registerForm()`, `register()` |
| **Annotations** | `@Controller` |

#### `CartController`

| | |
|---|---|
| **Purpose** | MVC cart pages and JSON count endpoint |
| **Extends/implements** | — |
| **Interactions** | `CartService`, `CartSseService` |
| **Key methods** | `viewCart`, `add`, `updateQuantity`, `removeItem`, `clear`, `itemCount` |
| **Annotations** | `@Controller`, `@RequestMapping("/cart")` |

#### `CartSseController`

| | |
|---|---|
| **Purpose** | Server-Sent Events stream for cart badge |
| **Extends/implements** | — |
| **Interactions** | `CartSseService`, `CartService` |
| **Key methods** | `stream()` → `SseEmitter` |
| **Annotations** | `@RestController`, `@RequestMapping("/cart")` |

#### `CheckoutController`

| | |
|---|---|
| **Purpose** | Checkout page, COD order, order confirmation |
| **Extends/implements** | — |
| **Interactions** | `CartService`, `OrderService`, `PayPalService` |
| **Key methods** | `checkoutPage()`, `cashOnDelivery()`, `confirm()` |
| **Annotations** | `@Controller`, `@RequestMapping("/checkout")` |

#### `HealthController`

| | |
|---|---|
| **Purpose** | Railway/load balancer health probe |
| **Extends/implements** | — |
| **Interactions** | none |
| **Key methods** | `health()` → `{"status":"UP"}` |
| **Annotations** | `@RestController` |

#### `HomeController`

| | |
|---|---|
| **Purpose** | Home page |
| **Extends/implements** | — |
| **Interactions** | none |
| **Key methods** | `home()` → `index` |
| **Annotations** | `@Controller` |

#### `PayPalCheckoutController`

| | |
|---|---|
| **Purpose** | REST API for PayPal JS SDK (create/capture) |
| **Extends/implements** | — |
| **Interactions** | `CartService`, `OrderService`, `PayPalService`, `ApiConstants` |
| **Key methods** | `createPayPalOrder()`, `capturePayPalOrder()` |
| **Annotations** | `@RestController`, `@RequestMapping("/checkout/paypal")` |

#### `ShopController`

| | |
|---|---|
| **Purpose** | Public catalog browsing |
| **Extends/implements** | — |
| **Interactions** | `CategoryService`, `ProductService` |
| **Key methods** | `categories()`, `products()`, `productDetail()` |
| **Annotations** | `@Controller`, `@RequestMapping("/shop")` |

---

### 8.5 `controller.api`

#### `AuthRestController`

| | |
|---|---|
| **Purpose** | Issue JWT after username/password authentication |
| **Extends/implements** | — |
| **Interactions** | `AuthenticationManager`, `UserDetailsService`, `JwtService` |
| **Key methods** | `login(AuthRequest)` |
| **Annotations** | `@RestController`, `@RequestMapping("/api/auth")` |

#### `ProductRestController`

| | |
|---|---|
| **Purpose** | JSON product catalog |
| **Extends/implements** | — |
| **Interactions** | `ProductService` |
| **Key methods** | `list(categoryId?)`, `get(id)` |
| **Annotations** | `@RestController`, `@RequestMapping("/api/products")` |

---

### 8.6 `dto`

#### `AuthRequest`

| | |
|---|---|
| **Purpose** | REST login body |
| **Extends/implements** | — |
| **Interactions** | `AuthRestController` |
| **Key members** | `username`, `password` |
| **Annotations** | `@Getter`, `@Setter` |

#### `AuthResponse`

| | |
|---|---|
| **Purpose** | REST login response with JWT |
| **Extends/implements** | — |
| **Interactions** | `AuthRestController` |
| **Key members** | `token` |
| **Annotations** | `@Getter`, `@AllArgsConstructor` |

#### `CartLineView`

| | |
|---|---|
| **Purpose** | One cart row for UI |
| **Extends/implements** | — |
| **Interactions** | Built in `CartService.toSummary` |
| **Key members** | `itemId`, `productId`, `productName`, `unitPrice`, `quantity`, `stock`, `lineTotal` |
| **Annotations** | `@Getter`, `@Builder` |

#### `CartSummary`

| | |
|---|---|
| **Purpose** | Full cart for cart/checkout pages |
| **Extends/implements** | — |
| **Interactions** | `CartService`, `CheckoutController`, `PayPalService` |
| **Key members** | `lines`, `totalItems`, `totalPrice` |
| **Annotations** | `@Getter`, `@Builder` |

#### `CategoryForm`

| | |
|---|---|
| **Purpose** | Admin category create/edit binding |
| **Extends/implements** | — |
| **Interactions** | `AdminController`, `CategoryService` |
| **Annotations** | `@NotBlank`, `@Size`, `@Getter`, `@Setter` |

#### `OrderFilterForm`

| | |
|---|---|
| **Purpose** | Admin order search criteria |
| **Extends/implements** | — |
| **Interactions** | `AdminController`, `AdminOrderService` |
| **Key members** | `username`, `fromDate`, `toDate` |
| **Annotations** | `@DateTimeFormat`, `@Getter`, `@Setter` |

#### `ProductDto`

| | |
|---|---|
| **Purpose** | Product JSON for REST API |
| **Extends/implements** | — |
| **Interactions** | `ProductRestController`, `ProductService` |
| **Key methods** | `from(Product)` |
| **Annotations** | `@Getter`, `@Builder` |

#### `ProductForm`

| | |
|---|---|
| **Purpose** | Admin product create/edit binding |
| **Extends/implements** | — |
| **Interactions** | `AdminController`, `ProductService` |
| **Annotations** | Jakarta validation, `@Getter`, `@Setter` |

#### `RegisterForm`

| | |
|---|---|
| **Purpose** | User self-registration |
| **Extends/implements** | — |
| **Interactions** | `AuthController`, `UserService` |
| **Key members** | username, email, password, confirmPassword, role default `ROLE_USER` |
| **Annotations** | `@NotBlank`, `@Email`, `@Size`, … |

---

### 8.7 `exception`

#### `GlobalExceptionHandler`

| | |
|---|---|
| **Purpose** | MVC 404 handling |
| **Extends/implements** | — |
| **Interactions** | `ResourceNotFoundException` |
| **Key methods** | `handleNotFound()` → `error/not-found` |
| **Annotations** | `@ControllerAdvice`, `@ExceptionHandler` |

#### `ResourceNotFoundException`

| | |
|---|---|
| **Purpose** | Signal missing entity (domain) |
| **Extends/implements** | `RuntimeException` |
| **Interactions** | Thrown from services; caught by both handlers |
| **Annotations** | none |

#### `RestExceptionHandler`

| | |
|---|---|
| **Purpose** | JSON errors for `controller.api` package |
| **Extends/implements** | — |
| **Interactions** | `ApiConstants.JSON_ERROR` |
| **Key methods** | `notFound`, `badRequest`, `illegalState` |
| **Annotations** | `@RestControllerAdvice(basePackages = "...controller.api")` |

---

### 8.8 `model`

#### `Cart`

| | |
|---|---|
| **Purpose** | Shopping cart (anonymous session or logged-in user) |
| **Extends/implements** | JPA entity |
| **Interactions** | `CartItem`, `User`; `CartRepository` |
| **Key fields** | `sessionId`, `user`, `items` |
| **Annotations** | `@Entity`, `@Table("carts")`, Lombok |

#### `CartItem`

| | |
|---|---|
| **Purpose** | Line in cart |
| **Extends/implements** | JPA entity |
| **Interactions** | `Cart`, `Product` |
| **Annotations** | `@Entity`, `@ManyToOne`, … |

#### `Category`

| | |
|---|---|
| **Purpose** | Product category |
| **Extends/implements** | JPA entity |
| **Interactions** | `Product` (many products per category) |
| **Annotations** | `@Entity`, unique `name` |

#### `LoginAudit`

| | |
|---|---|
| **Purpose** | Record successful login (username, IP, time) |
| **Extends/implements** | JPA entity |
| **Interactions** | `AuditService`, `LoginAuditRepository` |
| **Annotations** | `@Entity` |

#### `Order`

| | |
|---|---|
| **Purpose** | Placed shop order |
| **Extends/implements** | JPA entity |
| **Interactions** | `User`, `OrderItem`, `PaymentMethod` |
| **Key fields** | `orderDate`, `totalAmount`, `paypalOrderId` |
| **Annotations** | `@Entity`, `@Table("orders")` |

#### `OrderItem`

| | |
|---|---|
| **Purpose** | Snapshot line on order (qty + price at purchase) |
| **Extends/implements** | JPA entity |
| **Interactions** | `Order`, `Product` |
| **Annotations** | `@Entity` |

#### `PaymentMethod`

| | |
|---|---|
| **Purpose** | COD vs PayPal enum |
| **Extends/implements** | `enum` |
| **Values** | `COD`, `PAYPAL` |
| **Annotations** | none |

#### `Product`

| | |
|---|---|
| **Purpose** | Sellable item with stock |
| **Extends/implements** | JPA entity |
| **Interactions** | `Category` |
| **Annotations** | `@Entity`, `@ManyToOne` category |

#### `Role`

| | |
|---|---|
| **Purpose** | Spring Security authorities |
| **Extends/implements** | `enum` |
| **Values** | `ROLE_USER`, `ROLE_ADMIN` |
| **Annotations** | none |

#### `User`

| | |
|---|---|
| **Purpose** | Shop account |
| **Extends/implements** | JPA entity |
| **Interactions** | `UserRepository`, `UserDetailsServiceImpl` |
| **Key fields** | `username`, `password` (BCrypt), `email`, `role` |
| **Annotations** | `@Entity`, `@Table("users")`, `@Enumerated(STRING)` |

---

### 8.9 `repository`

All extend `JpaRepository<Entity, Long>` — Spring Data generates implementations.

#### `CartRepository`

| | |
|---|---|
| **Purpose** | Cart persistence with eager item fetch |
| **Key methods** | `findBySessionId`, `findByUser`, `findWithItemsBySessionId`, `findWithItemsByUserId` |
| **Annotations** | `@Query` on fetch methods |

#### `CategoryRepository`

| | |
|---|---|
| **Purpose** | Category CRUD |
| **Key methods** | inherited `findAll`, `save`, … |

#### `LoginAuditRepository`

| | |
|---|---|
| **Purpose** | Login audit persistence |
| **Key methods** | `findAllByOrderByLoginTimeDesc()` |

#### `OrderRepository`

| | |
|---|---|
| **Purpose** | Orders for user and admin search |
| **Key methods** | `findByUserWithItemsOrderByOrderDateDesc`, `findWithItemsById`, `searchAdmin` |

#### `ProductRepository`

| | |
|---|---|
| **Purpose** | Product queries with category fetch |
| **Key methods** | `findByCategoryId`, `findWithCategoryById`, `findAllWithCategory`, `existsByCategoryId` |

#### `UserRepository`

| | |
|---|---|
| **Purpose** | User lookup for auth and registration |
| **Key methods** | `findByUsername`, `findByEmail` |

---

### 8.10 `security`

#### `AuthSuccessListener`

| | |
|---|---|
| **Purpose** | Record login audit on successful authentication |
| **Extends/implements** | `ApplicationListener<AuthenticationSuccessEvent>` |
| **Interactions** | `AuditService`, `RequestContextHelper` |
| **Key methods** | `onApplicationEvent()` |
| **Annotations** | `@Component` |

#### `CartMergeOnLoginListener`

| | |
|---|---|
| **Purpose** | Merge anonymous session cart into user cart after login |
| **Extends/implements** | `ApplicationListener<AuthenticationSuccessEvent>` |
| **Interactions** | `CartService`, `RequestContextHelper` |
| **Key methods** | `onApplicationEvent()` |
| **Annotations** | `@Component` |

#### `JwtAuthFilter`

| | |
|---|---|
| **Purpose** | Authenticate `/api/**` requests via Bearer JWT |
| **Extends/implements** | `OncePerRequestFilter` |
| **Interactions** | `JwtService`, `UserDetailsService` |
| **Key methods** | `doFilterInternal()` |
| **Annotations** | `@Component` |

#### `RequestContextHelper`

| | |
|---|---|
| **Purpose** | Access current request/session/IP outside controllers |
| **Extends/implements** | `final` utility |
| **Key methods** | `currentRequest()`, `currentSession()`, `clientIpAddress()` |
| **Annotations** | none |

#### `UserDetailsServiceImpl`

| | |
|---|---|
| **Purpose** | Load Spring Security `UserDetails` from DB (username or email login) |
| **Extends/implements** | `UserDetailsService` |
| **Interactions** | `UserRepository` |
| **Key methods** | `loadUserByUsername()` |
| **Annotations** | `@Service` |

---

### 8.11 `service`

#### `AdminOrderService`

| | |
|---|---|
| **Purpose** | Admin order search and username list |
| **Interactions** | `OrderRepository.searchAdmin`, `UserRepository` |
| **Key methods** | `search(OrderFilterForm)`, `allUsernames()` |
| **Annotations** | `@Service`, `@Transactional(readOnly = true)` |

#### `AuditService`

| | |
|---|---|
| **Purpose** | Persist login audits without blocking request thread |
| **Interactions** | `LoginAuditRepository` |
| **Key methods** | `recordLoginAsync(username, ip)` |
| **Annotations** | `@Service`, `@Async`, `@Slf4j` |

#### `CartService`

| | |
|---|---|
| **Purpose** | Cart CRUD, summary DTO, merge on login |
| **Interactions** | `CartRepository`, `ProductService`, `UserRepository`, `SecurityContextHolder` |
| **Key methods** | `getCartSummary`, `addProduct`, `updateItemQuantity`, `removeItem`, `clearCart`, `mergeSessionCartOnLogin`, `getTotalItemCount` |
| **Annotations** | `@Service`, `@Transactional` |

#### `CartSseService`

| | |
|---|---|
| **Purpose** | Manage `SseEmitter` per session; broadcast cart count |
| **Interactions** | Called from `CartController`, `CartSseController` |
| **Key methods** | `subscribe`, `publishCount` |
| **Annotations** | `@Service` (in-memory `ConcurrentHashMap`) |

#### `CategoryService`

| | |
|---|---|
| **Purpose** | Category business logic |
| **Interactions** | `CategoryRepository`, `ProductRepository` (delete guard) |
| **Key methods** | `findAll`, `findById`, `save`, `update`, `delete` |
| **Annotations** | `@Service` |

#### `JwtService`

| | |
|---|---|
| **Purpose** | Create and validate JWT (JJWT) |
| **Interactions** | `AuthRestController`, `JwtAuthFilter` |
| **Key methods** | `generateToken`, `extractUsername`, `isTokenValid` |
| **Annotations** | `@Service`, `@Value` constructor params |

#### `LoginAuditService`

| | |
|---|---|
| **Purpose** | Read login audits for admin page |
| **Interactions** | `LoginAuditRepository` |
| **Key methods** | `findAllNewestFirst()` |
| **Annotations** | `@Service` |

#### `OrderService`

| | |
|---|---|
| **Purpose** | Place orders, list user orders, ownership check |
| **Interactions** | `CartService`, `OrderRepository`, `ProductRepository`, `UserRepository` |
| **Key methods** | `placeOrder`, `getOrdersForCurrentUser`, `getOrderForCurrentUser` |
| **Annotations** | `@Service` — decrements stock in same transaction |

#### `PayPalService`

| | |
|---|---|
| **Purpose** | PayPal Orders API wrapper + SDK URL |
| **Interactions** | `PaypalServerSdkClient`, `PayPalProperties` |
| **Key methods** | `createOrder`, `captureOrder`, `buildJsSdkUrl`, `isEnabled`, `isServerReady` |
| **Annotations** | `@Service` |

#### `ProductService`

| | |
|---|---|
| **Purpose** | Product CRUD and DTO mapping |
| **Interactions** | `ProductRepository`, `CategoryService` |
| **Key methods** | `findAllDto`, `findById`, `save`, `update`, `delete`, `findByCategoryId` |
| **Annotations** | `@Service` |

#### `UserService`

| | |
|---|---|
| **Purpose** | Registration and user lookup |
| **Interactions** | `UserRepository`, `PasswordEncoder` |
| **Key methods** | `register(RegisterForm)`, `findByUsername` |
| **Annotations** | `@Service` |

---

### 8.12 `web`

#### `CartModelAdvice`

| | |
|---|---|
| **Purpose** | Expose `cartItemCount` to all Thymeleaf views (navbar badge) |
| **Extends/implements** | — |
| **Interactions** | `CartService` |
| **Key methods** | `@ModelAttribute cartItemCount(HttpSession)` |
| **Annotations** | `@ControllerAdvice`, `@RequiredArgsConstructor` |

#### `MvcConstants`

| | |
|---|---|
| **Purpose** | Centralize view names, redirect URLs, flash attribute keys |
| **Extends/implements** | `final` utility |
| **Interactions** | Controllers, `SecurityConfig` (login path) |
| **Key constants** | `LOGIN_PATH`, `REDIRECT_CART`, `FLASH_SUCCESS`, admin form views |
| **Annotations** | none |

---

## Appendix A — URL / security matrix

| Path pattern | Auth | Controller |
|--------------|------|------------|
| `/`, `/shop/**`, `/cart/**`, `/cart/sse`, `/login`, `/register`, `/css/**`, `/js/**` | Public | Various |
| `/health` | Public | `HealthController` |
| `/admin/**` | `ROLE_ADMIN` | `AdminController` |
| `/checkout/**`, `/account/**` | `USER` or `ADMIN` | Checkout, Account, PayPal REST |
| `/api/auth/**` | Public | `AuthRestController` |
| `/api/**` | Authenticated (JWT or session) | `ProductRestController` |

CSRF disabled globally (JWT API + simplicity); session cookie for MVC.

---

## Appendix B — Database tables (entity → table)

| Entity | Table |
|--------|-------|
| `User` | `users` |
| `Category` | `categories` |
| `Product` | `products` |
| `Cart` | `carts` |
| `CartItem` | `cart_items` |
| `Order` | `orders` |
| `OrderItem` | `order_items` |
| `LoginAudit` | `login_audits` |

---

## Appendix C — Maven dependencies (study focus)

| Dependency | Used for |
|------------|----------|
| `spring-boot-starter-webmvc` | MVC + REST |
| `spring-boot-starter-thymeleaf` | HTML templates |
| `spring-boot-starter-security` | Auth |
| `spring-boot-starter-data-jpa` | PostgreSQL ORM |
| `spring-boot-starter-validation` | Form validation |
| `thymeleaf-extras-springsecurity6` | `sec:*` in templates |
| `jjwt-*` | JWT in `JwtService` |
| `paypal-server-sdk` | Server-side PayPal orders |
| `postgresql` | JDBC driver (runtime) |
| `lombok` | Boilerplate reduction |

---

*Document generated from source snapshot of JavaWebPrj — 76 Java types under `src/main/java/hr/algebra/javawebprj`.*
