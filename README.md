# Space Flight Dynamics as a Service (SFDaaS)

Overview
--------
A modern web based implementation of orbit propagation using the open source OreKit library [http://orekit.org], which is "A free low level space dynamics library". SFDaaS has the capability to add output in local or remote memcached servers for later retrieval.

[![Build System](https://img.shields.io/badge/build-Maven-C71A36?logo=apache-maven)](https://maven.apache.org/)
[![Server](https://img.shields.io/badge/server-Netty-00ADD8?logo=netty)](https://netty.io/)
[![OreKit](https://img.shields.io/badge/OreKit-13.1.2-blue)](https://www.orekit.org/)
[![Java](https://img.shields.io/badge/Java-8+-orange?logo=java)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-LGPL--3.0-green)](LGPL-LICENSE.txt)

**Recent Updates (January 2026):**

- ✅ **Migrated to Netty** - Standalone server, no servlet container needed
- ✅ **JSON API** - Modern RESTful JSON responses with diagnostics
- ✅ Updated to OreKit 13.1.2 and Hipparchus 4.0.2
- ✅ Standalone JAR deployment (21MB with all dependencies)
- ✅ Task automation with Taskfile.yaml
- ✅ Comprehensive documentation

---

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Features](#features)
- [Installation](#installation)
- [Building](#building)
- [Deployment](#deployment)
- [API Usage](#api-usage)
- [Configuration](#configuration)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Migration from Tomcat](#migration-from-tomcat)
- [License](#license)

---

## Overview

SFDaaS provides a RESTful web service for satellite orbit propagation. It performs numerical integration of satellite trajectories using OreKit's high-precision orbital mechanics models, with optional result caching via Memcached.

**Key Capabilities:**
- Numerical orbit propagation in J2000 Earth-centered frame
- Forward and backward time propagation
- Optional Memcached caching for performance
- RESTful JSON API with comprehensive diagnostics
- ISO-8601 epoch format support
- Standalone deployment (no application server required)
- In-memory session management

---

## Quick Start

### Prerequisites

- **Java 8+** - [Download](https://openjdk.org/)
- **Maven** - [Install](https://maven.apache.org/download.cgi)
- **Task** (optional, recommended) - [Install](https://taskfile.dev/installation/)

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd SFDaaS

# Install Task (optional but recommended)
make install-task

# Or manually:
# macOS: brew install go-task/tap/go-task
# Linux: sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin

```

### Build and Run

#### Option A: Using Task (Recommended)

```bash
# Build and run standalone server
task run

```

Then open: http://localhost:8080/SFDaaS/orekit/propagate/usage

#### Option B: Using Maven Directly

```bash
# Build standalone JAR
mvn clean package

# Run standalone server
java -Dserver.port=8080 -Dserver.contextPath=/SFDaaS -Dorekit.data.path=./data -jar target/SFDaaS-jar-with-dependencies.jar

```

### Test the API

```bash
# Get usage documentation (JSON)
curl "http://localhost:8080/SFDaaS/orekit/propagate/usage" | jq .

# Perform propagation
curl "http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-28T13:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]" | jq .

```

---

## Features

- **Standalone Server**: Netty-based, no servlet container required
- **JSON API**: Modern RESTful responses with comprehensive diagnostics
- **Orbit Propagation**: Numerical integration using classical Runge-Kutta integrator
- **Time Flexibility**: Propagate forwards or backwards in time
- **Reference Frame**: J2000 Earth-centered inertial frame
- **Epoch Format**: ISO-8601 standard (UTC timezone)
- **Caching**: Optional Memcached integration for performance
- **Session Management**: In-memory HTTP session support
- **Diagnostics**: Detailed timing, caching, session, and request information
- **Modern Build System**: Maven with Task automation

---

## Installation

### 1. Install Prerequisites

**Java Development Kit (JDK) 8+**

```bash
# Check version
java -version

# Install (if needed)
# macOS: brew install openjdk@11
# Ubuntu: sudo apt-get install openjdk-11-jdk
# RHEL: sudo yum install java-11-openjdk-devel

```

**Apache Maven**

```bash
# Check version
mvn --version

# Install (if needed)
# macOS: brew install maven
# Ubuntu: sudo apt-get install maven
# RHEL: sudo yum install maven

```

**Task (Optional but Recommended)**

```bash
# Using the provided Makefile
make install-task

# Or manually:
# macOS
brew install go-task/tap/go-task

# Linux
sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin

```

### 2. Clone Repository

```bash
git clone <repository-url>
cd SFDaaS

```

### 3. Validate Setup

```bash
# With Task
task validate

# Or manually
java -version && mvn --version

```

---

## Building

### Build Standalone JAR

```bash
# With Task (recommended)
task build

# With Maven
mvn clean package

# Output: target/SFDaaS-jar-with-dependencies.jar (21 MB)

```

The build creates a fat JAR containing all dependencies including Netty, Gson, OreKit, Hipparchus, and Spymemcached.

### Compile Only (No Packaging)

```bash
# With Task
task compile

# With Maven
mvn clean compile

```

### Verify Build

```bash
# With Task
task verify

# Or manually
ls -lh target/SFDaaS-jar-with-dependencies.jar
jar tf target/SFDaaS-jar-with-dependencies.jar | head -20

```

---

## Deployment

### Standalone Netty Server (Production Ready)

**Foreground Mode:**

```bash
# With Task
task run

# With Maven
java -Dserver.port=8080 -Dserver.contextPath=/SFDaaS -Dorekit.data.path=./data -jar target/SFDaaS-jar-with-dependencies.jar

```

**Background Mode:**

```bash
# With Task
task run     # Start in background
task status  # Check status
task ps      # Show running processes
task logs    # View logs (tail -f server.log)
task stop    # Stop server

```

Application available at: http://localhost:8080/SFDaaS/

**Advantages of Standalone Deployment:**
- No servlet container required
- Faster startup (3-4 seconds)
- Lower memory footprint
- Single JAR deployment
- Modern microservice architecture
- Async/non-blocking I/O with Netty

---

## API Usage

### Endpoints

- **Usage Documentation**: `/SFDaaS/orekit/propagate/usage` (returns JSON)
- **Propagation Service**: `/SFDaaS/orekit/propagate` (returns JSON)

### Response Format

All responses are in JSON format with the following structure:

```json
{
  "status": "success",
  "data": {
    "apriori": { "t0": "...", "r0": "[...]", "v0": "[...]" },
    "aposteriori": { "tf": "...", "rf": "[...]", "vf": "[...]" }
  },
  "diagnostics": {
    "timing": { "propagationTimeMs": 123, "totalTimeMs": 456 },
    "caching": { "enabled": false, "hit": false },
    "session": { "id": "...", "creationTime": 1234567890 },
    "request": { "method": "GET", "uri": "...", "headers": {...} },
    "orekit": { "version": "13.1.2", "dataPath": "./data" }
  }
}
```

### Basic Propagation

**Required Parameters:**

- `t0` - Initial epoch (format: `YYYY-MM-DDTHH:MM:SS.SSS`, UTC timezone)
- `tf` - Final epoch (same format)
- `r0` - Initial position vector `[x,y,z]` in meters (J2000 Earth-centered frame)
- `v0` - Initial velocity vector `[vx,vy,vz]` in meters/second

**Example Request:**

```bash
curl "http://localhost:8080/SFDaaS/orekit/propagate?\
t0=2010-05-28T12:00:00.000&\
tf=2010-05-28T13:00:00.000&\
r0=[3198022.67,2901879.73,5142928.95]&\
v0=[-6129.640631,4489.647187,1284.511245]"

```

**Example Response (JSON):**

```json
{
  "status": "success",
  "data": {
    "apriori": {
      "t0": "2010-05-28T12:00:00.000",
      "r0": "[3198022.67,2901879.73,5142928.95]",
      "v0": "[-6129.640631,4489.647187,1284.511245]"
    },
    "aposteriori": {
      "tf": "2010-05-28T13:00:00.000",
      "rf": "[-6174454.063243,2474544.324750,-976156.9807064387]",
      "vf": "[-991.274325,-4808.930607,-5927.623934582873]"
    }
  },
  "diagnostics": {
    "timing": {
      "propagationTimeMs": 45,
      "totalTimeMs": 67
    },
    "session": {
      "id": "abc123...",
      "creationTime": 1736629200000
    }
  }
}
```

### With Memcached Caching

**Additional Parameters:**
- `cf=1` - Enable caching flag
- `ca=127.0.0.1:11211` - Memcached server address and port
- `ct=60` - Cache expiration time in seconds (default: 60)
- `ck={KEY}` - Custom cache key (optional)

**Example Request:**

```bash
curl "http://localhost:8080/SFDaaS/orekit/propagate?\
cf=1&\
ca=127.0.0.1:11211&\
ct=60&\
t0=2010-05-28T12:00:00.000&\
tf=2010-05-29T12:00:00.000&\
r0=[3198022.67,2901879.73,5142928.95]&\
v0=[-6129.640631,4489.647187,1284.511245]"
```

### Session Management

**Optional Parameters:**
- `sf=1` - Use session values
- `st=1800` - Session timeout in seconds (default: 1800)

The server automatically manages HTTP sessions using cookies (JSESSIONID). Session information is included in the diagnostics section of responses.

### Browser Access

Open in browser:

```
http://localhost:8080/SFDaaS/orekit/propagate/usage
```

The usage endpoint returns a JSON document describing all available parameters and providing example URLs.

### Assumptions

1. **Epochs**: UTC timezone (ISO-8601 format)
2. **Units**: Position in meters, velocity in meters/second
3. **Reference Frame**: J2000 Earth-centered inertial
4. **Time Direction**: Can propagate forwards or backwards

---

## Configuration

### OreKit Data Path

The application requires OreKit data files (UTC-TAI tables, etc.). Default location: `./data/`

**To customize:**

```bash
# With Task (set environment variable)
task run DATA_PATH=/custom/path/to/data

# Or directly with Java
java -Dorekit.data.path=/custom/path/to/data -jar target/SFDaaS-jar-with-dependencies.jar

```

### Port Configuration

**Change default port (8080):**

```bash
# With Task using SERVER_PORT environment variable
SERVER_PORT=9999 task run

# Or directly with Java
java -Dserver.port=9999 -Dserver.contextPath=/SFDaaS -jar target/SFDaaS-jar-with-dependencies.jar

```

### Context Path

**Change default context path (/SFDaaS):**

```bash
# With Java system property
java -Dserver.contextPath=/myapp -jar target/SFDaaS-jar-with-dependencies.jar

```

### Memcached Setup (Optional)

To enable caching features:

```bash
# Install Memcached
# macOS
brew install memcached
brew services start memcached

# Ubuntu/Debian
sudo apt-get install memcached
sudo systemctl start memcached

# RHEL/CentOS
sudo yum install memcached
sudo systemctl start memcached

# Or run manually
memcached -d -m 64 -p 11211

```

---

## Development

### Common Tasks

**With Task Runner:**

| Task | Command |
|------|---------|
| Show all tasks | `task --list` |
| Setup & validate | `task setup` |
| Clean build | `task build` |
| Compile only | `task compile` |
| Run server | `task run` |
| Stop background server | `task stop` |
| Check status | `task status` |
| Show running processes | `task ps` |
| View logs | `task logs` |
| View dependencies | `task deps` |
| Verify JAR | `task verify` |
| Get help | `task help` |

**With Maven:**

| Task | Command |
|------|---------|
| Clean build | `mvn clean package` |
| Compile only | `mvn clean compile` |
| View dependencies | `mvn dependency:tree` |

### Development Workflow

**Daily Development:**

```bash
# 1. Start server in background
task run

# 2. Make code changes...

# 3. Rebuild and restart
task stop
task build
task run

# 4. View logs if needed
task logs

# 5. Stop when done
task stop

```

### Project Structure

```
SFDaaS/
├── pom.xml                          # Maven build configuration (JAR packaging)
├── Taskfile.yaml                    # Task automation
├── Makefile                         # Redirects to Task with install helper
├── README.md                        # This file
├── NETTY-MIGRATION.md               # Netty migration documentation
├── Usage.html                       # Original usage documentation
│
├── src/
│   └── org/spaceflightdynamics/    # Application code
│       ├── propagation/
│       │   └── Propagator.java     # Core propagation logic
│       ├── netty/                   # Netty server implementation
│       │   ├── NettyServer.java    # Main server class
│       │   ├── HttpRequestHandler.java
│       │   ├── HttpSession.java
│       │   ├── SessionManager.java
│       │   ├── RouteHandler.java
│       │   └── JsonResponseBuilder.java
│       └── utils/                   # Utility classes
│
├── data/                            # OreKit data files (UTC-TAI tables)
├── WebContent/                      # Static resources (legacy)
│
└── target/                          # Maven build output
    ├── SFDaaS.jar                   # Standard JAR (4.6 MB)
    └── SFDaaS-jar-with-dependencies.jar  # Fat JAR (21 MB)
```

### Making Changes

**Code Updates:**

```bash
# Edit source files in src/org/spaceflightdynamics/
# Rebuild
task build

# Test locally
task run

```

**Updating Dependencies:**

Edit [pom.xml](pom.xml) and rebuild:

```bash
# Check for updates
task deps-update

# Rebuild
task build

```

---

## Troubleshooting

### Build Issues

**Problem: "cannot find symbol" errors**

```bash
# Clean and rebuild
task clean
task build

# Or with Maven
mvn clean package

```

**Problem: Java version issues**

```bash
# Check Java version (need 8+)
java -version

# If using wrong version, set JAVA_HOME
export JAVA_HOME=/path/to/jdk-11

```

### Server Issues

**Problem: Port 8080 already in use**

```bash
# Find process using port 8080
lsof -ti:8080

# Kill it
lsof -ti:8080 | xargs kill -9

# Or use different port
SERVER_PORT=8081 task run

```

**Problem: Server won't start**

```bash
# Check status
task status

# View logs
task logs

# Or check directly
tail -f server.log

# Stop and restart
task stop
task run

```

**Problem: Server running but not responding**

```bash
# Check if process is running
task ps

# Check logs for errors
task logs

# Verify port is listening
lsof -i:8080

```

### API Issues

**Problem: 404 Not Found at root URL**

This is expected. The application is only mapped to:

- `/SFDaaS/orekit/propagate/usage`
- `/SFDaaS/orekit/propagate`

**Problem: "OreKit data path not found"**

```bash
# Verify data directory exists
ls -la data/

# Set data path explicitly
task run DATA_PATH=$(pwd)/data

# Or check system property
java -Dorekit.data.path=./data -jar target/SFDaaS-jar-with-dependencies.jar

```

**Problem: Propagation returns errors**

```bash
# Check logs
task logs

# Verify OreKit data is loaded
# Look for "OreKit Configuration" messages in server.log
```

### Development Issues

**Problem: Changes not reflected**

```bash
# Clean build
task clean
task build

# Stop and restart server
task stop
task run
```

**Problem: Task command not found**

```bash
# Install Task
#   See: https://taskfile.dev/installation/
make install-task

```

### Validation

**Validate entire setup:**

```bash
# With Task
task validate

# Shows:
# ✓ Java installation
# ✓ Maven installation
# ✓ Data directory
# ✓ Project configuration
```

---

## Dependencies

The project uses Maven for dependency management with the following libraries:

| Library | Version | Purpose |
|---------|---------|---------|
| **Netty** | 4.1.104.Final | Async HTTP server framework |
| **Gson** | 2.10.1 | JSON serialization/deserialization |
| **OreKit** | 13.1.2 | Space flight dynamics library |
| **Hipparchus** | 4.0.2 | Mathematical library (OreKit dependency) |
| **Spymemcached** | 2.12.3 | Memcached client for caching |

**Migration Note:** The project was migrated from Tomcat servlet-based architecture to Netty standalone server in January 2026. See [NETTY-MIGRATION.md](NETTY-MIGRATION.md) for details.

### View Dependency Tree

```bash
# With Task
task deps

# With Maven
mvn dependency:tree

```

### Check for Updates

```bash
# With Task
task deps-update

# With Maven
mvn versions:display-dependency-updates

```

---

## Migration from Tomcat

SFDaaS was migrated from a Tomcat servlet-based architecture to a standalone Netty server in January 2026.

**Key Changes:**
- **Packaging**: WAR → Standalone JAR (21MB)
- **Server**: Tomcat → Netty (async/non-blocking I/O)
- **API**: HTML/plain text → JSON responses
- **Session**: Servlet sessions → In-memory session management
- **Deployment**: No servlet container required

**Benefits:**
- Faster startup (3-4 seconds vs 8-10 seconds)
- Lower memory footprint
- Modern microservice architecture
- Better performance with async I/O
- Simpler deployment model

For complete migration details, see **[NETTY-MIGRATION.md](NETTY-MIGRATION.md)**.

---

## License

**SFDaaS** is licensed under the **LGPL License version 3.0** by Haisam K. Ido.
See [LGPL-LICENSE.txt](LGPL-LICENSE.txt) for details.

**OreKit** ([www.orekit.org](https://www.orekit.org/)) is licensed by CS Communication & Systèmes under the **Apache License Version 2.0**.

**Hipparchus** ([hipparchus.org](https://hipparchus.org/)) is licensed by the Hipparchus project under the **Apache License Version 2.0**.

**Spymemcached** is licensed under the **MIT License**.

**Netty** ([netty.io](https://netty.io/)) is licensed by The Netty Project under the **Apache License Version 2.0**.

**Gson** is licensed by Google under the **Apache License Version 2.0**.

---

## Resources

- **OreKit Documentation**: https://www.orekit.org/
- **Hipparchus Documentation**: https://hipparchus.org/
- **Netty Documentation**: https://netty.io/
- **Apache Maven**: https://maven.apache.org/
- **Task**: https://taskfile.dev/
- **Gson**: https://github.com/google/gson

---

## Support

For issues, questions, or contributions:

1. Check this README
2. See [NETTY-MIGRATION.md](NETTY-MIGRATION.md) for migration information
3. See [Usage.html](Usage.html) for original API documentation
4. Review logs: `task logs`
5. Validate setup: `task validate`

---

**Built with OreKit 13.1.2 • Netty 4.1.104 • Maven • Task • ❤️**

**Last Updated:** January 11, 2026
**Status:** ✅ Production Ready
