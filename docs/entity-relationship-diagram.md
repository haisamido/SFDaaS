# SFDaaS Entity Relationship Diagram

## System Architecture Overview

This diagram shows the relationships between all entities in the Space Flight Dynamics as a Service (SFDaaS) system.

```mermaid
erDiagram
    %% ============================================================================
    %% WEB/API LAYER - HTTP Request Processing
    %% ============================================================================

    NettyServer ||--o{ HttpRequestHandler : "creates per-channel"
    NettyServer ||--|| SessionManager : "owns"

    HttpRequestHandler ||--|| SessionManager : "uses"
    HttpRequestHandler ||--o{ HttpSession : "accesses via cookie"
    HttpRequestHandler ||--|| RouteHandler : "delegates business logic"
    HttpRequestHandler ||--|| JsonResponseBuilder : "formats responses"

    SessionManager ||--o{ HttpSession : "manages lifecycle"

    RouteHandler ||--|| Propagator : "instantiates"
    RouteHandler ||--o{ HttpSession : "reads/updates"
    RouteHandler ||--|| JsonResponseBuilder : "uses"
    RouteHandler ||--o{ MemcachedClient : "optional caching"

    %% ============================================================================
    %% CORE DOMAIN LAYER - Orbit Propagation
    %% ============================================================================

    Propagator ||--|| PropagatorType : "references"
    Propagator ||--|| FrameType : "references"
    Propagator ||--|| OrbitType : "references"
    Propagator ||--|| TimeScale : "references"
    Propagator ||--|| IntegratorFactory : "uses"
    Propagator ||--|| FrameFactory : "uses"
    Propagator ||--|| OreKitLibrary : "integrates"

    IntegratorFactory ||--|| PropagatorType : "reads"
    IntegratorFactory ||--|| HipparchusLibrary : "creates integrators from"

    FrameFactory ||--|| FrameType : "reads"
    FrameFactory ||--|| OreKitLibrary : "creates frames from"

    %% ============================================================================
    %% EXTERNAL DEPENDENCIES
    %% ============================================================================

    JsonResponseBuilder ||--|| GsonLibrary : "uses"

    %% ============================================================================
    %% ENTITY DEFINITIONS
    %% ============================================================================

    NettyServer {
        int port
        string contextPath
        EventLoopGroup bossGroup
        EventLoopGroup workerGroup
        SessionManager sessionManager
    }

    HttpRequestHandler {
        SessionManager sessionManager
        string contextPath
        void channelRead0()
        void sendJsonResponse()
        void serveStaticFile()
        HttpSession getOrCreateSession()
    }

    SessionManager {
        Map sessions
        ScheduledExecutorService cleanupExecutor
        HttpSession createSession()
        HttpSession getSession()
        void cleanupExpiredSessions()
    }

    HttpSession {
        string sessionId
        long creationTime
        long lastAccessedTime
        int maxInactiveInterval
        Map attributes
        string getId()
        Object getAttribute()
        void setAttribute()
        void updateLastAccessedTime()
        boolean isExpired()
    }

    RouteHandler {
        string handleUsage()
        string handlePropagate()
        string handle404()
        string formatCentralBody()
    }

    JsonResponseBuilder {
        string buildPropagationResponse()
        string buildUsageResponse()
        string buildErrorResponse()
        string buildMissingParametersError()
    }

    Propagator {
        double stepSize
        HashMap parms
        NumericalPropagator numericalPropagator
        FrameType frameType
        TimeScale timeScale
        void initialize()
        HashMap propagate()
        double getMuForCentralBody()
        TimeScale getTimeScale()
    }

    PropagatorType {
        string RUNGE_KUTTA
        string DORMAND_PRINCE
        string ADAMS_BASHFORTH
        string ADAMS_MOULTON
        PropagatorType fromKey()
    }

    FrameType {
        string EME2000
        string GCRF
        string ITRF
        string TEME
        string MOD
        string TOD
        FrameType fromKey()
    }

    OrbitType {
        string CARTESIAN
        string KEPLERIAN
        string CIRCULAR
        string EQUINOCTIAL
        OrbitType fromKey()
    }

    TimeScale {
        string UTC
        string TAI
        TimeScale fromKey()
        string getKey()
        string getDisplayName()
    }

    IntegratorFactory {
        AbstractIntegrator createIntegrator()
    }

    FrameFactory {
        Frame createFrame()
    }

    MemcachedClient {
        Object get()
        void set()
    }

    OreKitLibrary {
        NumericalPropagator NumericalPropagator
        CartesianOrbit CartesianOrbit
        Frame Frame
        AbsoluteDate AbsoluteDate
        CelestialBodyFactory CelestialBodyFactory
        SpacecraftState SpacecraftState
    }

    HipparchusLibrary {
        AbstractIntegrator AbstractIntegrator
        ClassicalRungeKuttaIntegrator ClassicalRungeKuttaIntegrator
        DormandPrince853Integrator DormandPrince853Integrator
        AdamsBashforthIntegrator AdamsBashforthIntegrator
        AdamsMoultonIntegrator AdamsMoultonIntegrator
        Vector3D Vector3D
    }

    GsonLibrary {
        Gson Gson
        JsonObject JsonObject
    }
```

## Request Flow Diagram

```mermaid
sequenceDiagram
    participant Client
    participant NettyServer
    participant HttpRequestHandler
    participant SessionManager
    participant HttpSession
    participant RouteHandler
    participant Propagator
    participant OreKit
    participant JsonResponseBuilder
    participant Memcached

    Client->>NettyServer: HTTP Request
    NettyServer->>HttpRequestHandler: channelRead0(request)

    HttpRequestHandler->>SessionManager: getSession(JSESSIONID)
    alt Session exists
        SessionManager->>HttpSession: retrieve existing
        HttpSession->>SessionManager: return session
    else No session
        SessionManager->>HttpSession: createSession()
        HttpSession->>SessionManager: return new session
    end
    SessionManager->>HttpRequestHandler: HttpSession

    alt Static file request
        HttpRequestHandler->>Client: Serve HTML/CSS/JS
    else API request (/orekit/propagate)
        HttpRequestHandler->>RouteHandler: handlePropagate(request, session, params)

        alt Caching enabled (cf=1)
            RouteHandler->>Memcached: get(cacheKey)
            alt Cache hit
                Memcached->>RouteHandler: cached result
            else Cache miss
                RouteHandler->>Propagator: new Propagator(r0, v0, t0, tf, ..., timeScale)
                Propagator->>OreKit: initialize(integrator, frame, orbit, timeScale)
                Propagator->>OreKit: propagate(timeScale)
                OreKit->>Propagator: final state (rf, vf, tf)
                Propagator->>RouteHandler: HashMap result
                RouteHandler->>Memcached: set(cacheKey, result, ttl)
            end
        else No caching
            RouteHandler->>Propagator: new Propagator(r0, v0, t0, tf, ..., timeScale)
            Propagator->>OreKit: initialize(integrator, frame, orbit, timeScale)
            Propagator->>OreKit: propagate(timeScale)
            OreKit->>Propagator: final state (rf, vf, tf)
            Propagator->>RouteHandler: HashMap result
        end

        RouteHandler->>JsonResponseBuilder: buildPropagationResponse(apriori, aposteriori, diagnostics)
        JsonResponseBuilder->>RouteHandler: JSON string
        RouteHandler->>HttpRequestHandler: JSON response
        HttpRequestHandler->>Client: HTTP Response (JSON)
    end
```

## Component Dependency Diagram

```mermaid
graph TB
    subgraph "HTTP Layer"
        NettyServer[NettyServer<br/>Port: 8080<br/>Context: /SFDaaS]
        HttpRequestHandler[HttpRequestHandler<br/>Request Routing<br/>Cookie Handling]
        SessionManager[SessionManager<br/>Session Lifecycle<br/>Cleanup Task: 60s]
        HttpSession[HttpSession<br/>UUID-based ID<br/>Timeout: 1800s]
    end

    subgraph "Business Logic Layer"
        RouteHandler[RouteHandler<br/>/orekit/propagate<br/>/orekit/propagate/usage]
        JsonResponseBuilder[JsonResponseBuilder<br/>JSON Formatting<br/>API Documentation]
    end

    subgraph "Propagation Domain Layer"
        Propagator[Propagator<br/>initialize<br/>propagate]
        PropagatorType[PropagatorType Enum<br/>RungeKutta<br/>DormandPrince<br/>AdamsBashforth<br/>AdamsMoulton]
        FrameType[FrameType Enum<br/>EME2000<br/>GCRF<br/>ITRF<br/>TEME<br/>MOD<br/>TOD]
        OrbitType[OrbitType Enum<br/>Cartesian<br/>Keplerian<br/>Circular<br/>Equinoctial]
        TimeScale[TimeScale Enum<br/>UTC<br/>TAI]
    end

    subgraph "Factory Layer"
        IntegratorFactory[IntegratorFactory<br/>Creates ODE Integrators<br/>Tolerances: 10m, 0.01m/s]
        FrameFactory[FrameFactory<br/>Creates Reference Frames<br/>IERS 2010 Conventions]
    end

    subgraph "External Libraries"
        OreKit[OreKit 13.1.2<br/>NumericalPropagator<br/>CartesianOrbit<br/>CelestialBodyFactory]
        Hipparchus[Hipparchus 4.0.2<br/>AbstractIntegrator<br/>Vector3D]
        Gson[Gson 2.10.1<br/>JSON Serialization]
        Memcached[Spy Memcached 2.12.3<br/>Optional Caching]
        Netty[Netty 4.1.104<br/>Async HTTP Server]
    end

    NettyServer --> HttpRequestHandler
    NettyServer --> SessionManager
    NettyServer --> Netty

    HttpRequestHandler --> RouteHandler
    HttpRequestHandler --> SessionManager
    HttpRequestHandler --> HttpSession
    HttpRequestHandler --> JsonResponseBuilder

    SessionManager --> HttpSession

    RouteHandler --> Propagator
    RouteHandler --> HttpSession
    RouteHandler --> JsonResponseBuilder
    RouteHandler --> Memcached

    JsonResponseBuilder --> Gson

    Propagator --> PropagatorType
    Propagator --> FrameType
    Propagator --> OrbitType
    Propagator --> TimeScale
    Propagator --> IntegratorFactory
    Propagator --> FrameFactory
    Propagator --> OreKit

    IntegratorFactory --> PropagatorType
    IntegratorFactory --> Hipparchus

    FrameFactory --> FrameType
    FrameFactory --> OreKit

    style NettyServer fill:#e1f5ff
    style Propagator fill:#fff4e1
    style OreKit fill:#e8f5e9
    style RouteHandler fill:#f3e5f5
```

## Data Flow Diagram

```mermaid
flowchart LR
    subgraph Input["Input Parameters"]
        r0["Initial Position r0<br/>[x, y, z] meters"]
        v0["Initial Velocity v0<br/>[vx, vy, vz] m/s"]
        t0["Initial Epoch t0<br/>ISO 8601 format"]
        tf["Final Epoch tf<br/>ISO 8601 format"]
        timeScale["Time Scale<br/>utc (default), tai"]
        propagatorType["Propagator Type<br/>rungekutta, dormandprince, etc."]
        stepSize["Step Size<br/>seconds (default: 60)"]
        frameType["Reference Frame<br/>eme2000, gcrf, itrf, teme, etc."]
        centralBody["Central Body<br/>earth, sun, moon, etc.<br/>or custom mu value"]
        orbitType["Orbit Type<br/>cartesian (default)"]
        forceModels["Force Models<br/>(future)"]
        caching["Caching Options<br/>cf, ca, ct, ck"]
        session["Session Options<br/>sf, st"]
    end

    subgraph Processing["Processing Pipeline"]
        validation["Parameter Validation<br/>Required: t0, r0, v0, tf"]
        sessionMgmt["Session Management<br/>Get/Create Session<br/>Update Timeout"]
        cacheCheck["Cache Lookup<br/>Memcached (optional)"]
        propagation["Orbit Propagation<br/>OreKit NumericalPropagator"]
        cacheStore["Cache Storage<br/>Store result with TTL"]
        diagnostics["Diagnostics Assembly<br/>Timing, Session, Request, System"]
        jsonFormat["JSON Response Builder<br/>apriori + aposteriori + diagnostics"]
    end

    subgraph Output["Output Data"]
        apriori["Apriori State<br/>t0, r0, v0<br/>frame, centralBody, timeScale<br/>propagator, stepSize"]
        aposteriori["Aposteriori State<br/>tf, rf, vf"]
        diagTiming["Timing Diagnostics<br/>Propagation duration<br/>Total runtime"]
        diagCaching["Caching Diagnostics<br/>Hit/miss, servers, TTL"]
        diagSession["Session Diagnostics<br/>ID, creation, expiry"]
        diagRequest["Request Diagnostics<br/>Method, URI, headers"]
        diagSystem["System Diagnostics<br/>User, directories"]
        diagOreKit["OreKit Diagnostics<br/>Version, data path"]
    end

    r0 --> validation
    v0 --> validation
    t0 --> validation
    tf --> validation
    timeScale --> validation
    propagatorType --> validation
    stepSize --> validation
    frameType --> validation
    centralBody --> validation
    orbitType --> validation
    forceModels --> validation

    caching --> sessionMgmt
    session --> sessionMgmt

    validation --> sessionMgmt
    sessionMgmt --> cacheCheck

    cacheCheck -->|Cache miss| propagation
    cacheCheck -->|Cache hit| diagnostics

    propagation --> cacheStore
    cacheStore --> diagnostics

    diagnostics --> jsonFormat

    jsonFormat --> apriori
    jsonFormat --> aposteriori
    jsonFormat --> diagTiming
    jsonFormat --> diagCaching
    jsonFormat --> diagSession
    jsonFormat --> diagRequest
    jsonFormat --> diagSystem
    jsonFormat --> diagOreKit
```

## Key Relationships Summary

| Source Entity | Relationship | Target Entity | Cardinality |
|--------------|--------------|---------------|-------------|
| NettyServer | owns | SessionManager | 1:1 |
| NettyServer | creates | HttpRequestHandler | 1:N (per channel) |
| SessionManager | manages | HttpSession | 1:N |
| HttpRequestHandler | uses | SessionManager | N:1 |
| HttpRequestHandler | accesses | HttpSession | N:N |
| HttpRequestHandler | delegates to | RouteHandler | N:1 |
| HttpRequestHandler | formats via | JsonResponseBuilder | N:1 |
| RouteHandler | instantiates | Propagator | N:N |
| RouteHandler | reads/updates | HttpSession | N:N |
| RouteHandler | uses | JsonResponseBuilder | N:1 |
| RouteHandler | optionally uses | MemcachedClient | N:N |
| Propagator | references | PropagatorType | N:1 |
| Propagator | references | FrameType | N:1 |
| Propagator | references | OrbitType | N:1 |
| Propagator | references | TimeScale | N:1 |
| Propagator | uses | IntegratorFactory | N:1 |
| Propagator | uses | FrameFactory | N:1 |
| Propagator | integrates with | OreKitLibrary | N:1 |
| IntegratorFactory | reads | PropagatorType | N:N |
| IntegratorFactory | creates from | HipparchusLibrary | N:1 |
| FrameFactory | reads | FrameType | N:N |
| FrameFactory | creates from | OreKitLibrary | N:1 |
| JsonResponseBuilder | uses | GsonLibrary | N:1 |

## Entity Attributes Detail

### Core Propagation Entities

**Propagator**
- `stepSize: double` - Integration step size in seconds (default: 60)
- `parms: HashMap<String,String>` - Initial state parameters
- `numericalPropagator: NumericalPropagator` - OreKit propagator instance
- `frameType: FrameType` - Reference frame enumeration

**PropagatorType Enum Values**
- `RUNGE_KUTTA` - Classical 4th order fixed-step
- `DORMAND_PRINCE` - 8(5,3) adaptive-step (default)
- `ADAMS_BASHFORTH` - Multi-step variable-step
- `ADAMS_MOULTON` - Multi-step variable-step

**FrameType Enum Values**
- `EME2000` - Earth Mean Equator 2000 (Inertial, default)
- `GCRF` - Geocentric Celestial Reference Frame (IAU 2000)
- `ITRF` - International Terrestrial Reference Frame (Earth-fixed)
- `TEME` - True Equator Mean Equinox (TLE/SGP4 compatible)
- `MOD` - Mean of Date
- `TOD` - True of Date

**OrbitType Enum Values**
- `CARTESIAN` - Position/Velocity vectors (currently implemented)
- `KEPLERIAN` - Classical orbital elements
- `CIRCULAR` - Modified elements for near-circular orbits
- `EQUINOCTIAL` - Non-singular elements

**TimeScale Enum Values**
- `UTC` - Coordinated Universal Time with leap seconds (default)
- `TAI` - International Atomic Time - continuous atomic time without leap seconds

### Web/API Layer Entities

**NettyServer**
- `port: int` - HTTP server port (default: 8080)
- `contextPath: String` - Application context path (default: /SFDaaS)
- `sessionManager: SessionManager` - Session lifecycle manager
- `bossGroup: EventLoopGroup` - Connection acceptor
- `workerGroup: EventLoopGroup` - I/O handler

**HttpSession**
- `sessionId: String` - UUID-based unique identifier
- `creationTime: long` - Session creation timestamp (ms)
- `lastAccessedTime: long` - Last request timestamp (ms)
- `maxInactiveInterval: int` - Session timeout (default: 1800s)
- `attributes: Map<String,Object>` - Session data storage

**SessionManager**
- `sessions: Map<String,HttpSession>` - Active sessions (ConcurrentHashMap)
- `cleanupExecutor: ScheduledExecutorService` - Background cleanup (60s interval)

### API Parameters

**Required Propagation Parameters**

- `t0` - Initial epoch (ISO 8601 format)
- `r0` - Initial position vector [x,y,z] in meters
- `v0` - Initial velocity vector [vx,vy,vz] in m/s
- `tf` - Final epoch (ISO 8601 format)

**Optional Propagation Parameters**

- `propagator` - Integration method (default: dormandprince)
- `stepSize` - Integration step size in seconds (default: 60)
- `frame` - Reference frame (default: eme2000)
- `centralBody` - Central body or custom mu value (default: earth)
- `timeScale` - Time scale for epochs (default: utc)
- `orbitType` - Orbit representation (default: cartesian)
- `forceModels` - Perturbation models (future feature)

**Caching Parameters**

- `cf` - Cache flag (0=disabled, 1=enabled)
- `ca` - Cache server addresses (comma-separated)
- `ct` - Cache TTL in seconds (default: 60)
- `ck` - Custom cache key prefix

**Session Parameters**

- `sf` - Session flag (0=disable, 1=enable)
- `st` - Session timeout in seconds

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| HTTP Server | Netty | 4.1.104.Final | Async non-blocking HTTP server |
| Session Management | Custom (ConcurrentHashMap) | N/A | In-memory session storage |
| Orbit Propagation | OreKit | 13.1.2 | Space flight dynamics library |
| Numerical Integration | Hipparchus | 4.0.2 | Mathematical library (OreKit dependency) |
| JSON Processing | Gson | 2.10.1 | JSON serialization/deserialization |
| Caching | Spy Memcached | 2.12.3 | Optional distributed caching |
| Build Tool | Maven | 3.x | Dependency management and build |
| Task Runner | Task | 3.x | Development workflow automation |

## File Locations

| Entity | File Path |
|--------|----------|
| NettyServer | `sfdaas-api/src/org/sfdaas/api/netty/NettyServer.java` |
| HttpRequestHandler | `sfdaas-api/src/org/sfdaas/api/netty/HttpRequestHandler.java` |
| SessionManager | `sfdaas-api/src/org/sfdaas/api/netty/SessionManager.java` |
| HttpSession | `sfdaas-api/src/org/sfdaas/api/netty/HttpSession.java` |
| RouteHandler | `sfdaas-api/src/org/sfdaas/api/netty/RouteHandler.java` |
| JsonResponseBuilder | `sfdaas-api/src/org/sfdaas/api/netty/JsonResponseBuilder.java` |
| Propagator | `sfdaas-core/src/org/sfdaas/propagation/Propagator.java` |
| PropagatorType | `sfdaas-core/src/org/sfdaas/propagation/PropagatorType.java` |
| FrameType | `sfdaas-core/src/org/sfdaas/propagation/FrameType.java` |
| OrbitType | `sfdaas-core/src/org/sfdaas/propagation/OrbitType.java` |
| TimeScale | `sfdaas-core/src/org/sfdaas/propagation/TimeScale.java` |
| IntegratorFactory | `sfdaas-core/src/org/sfdaas/propagation/IntegratorFactory.java` |
| FrameFactory | `sfdaas-core/src/org/sfdaas/propagation/FrameFactory.java` |
| UI | `sfdaas-web/src/index.html` |

---

**Generated:** 2026-01-13
**SFDaaS Version:** 1.0.0
**OreKit Version:** 13.1.2
