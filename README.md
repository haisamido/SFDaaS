# Space Flight Dynamics as a Service (SFDaaS)

Overview
--------
A modern web based implementation of orbit propagation using the open source OreKit library [http://orekit.org], which is "A free low level space dynamics library". SFDaaS has the capability to add output in local or remote memcached servers for later retrieval.

[![Build System](https://img.shields.io/badge/build-Maven-C71A36?logo=apache-maven)](https://maven.apache.org/)
[![OreKit](https://img.shields.io/badge/OreKit-11.3.3-blue)](https://www.orekit.org/)
[![Java](https://img.shields.io/badge/Java-8+-orange?logo=java)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-LGPL--3.0-green)](LGPL-LICENSE.txt)

**Recent Updates (January 2026):**

- ✅ Migrated to Maven build system
- ✅ Updated to OreKit 13.1.2 and Hipparchus 2.3
- ✅ Added Task automation with Taskfile.yaml
- ✅ Modernized code for current API compatibility
- ✅ Added comprehensive documentation

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
- [License](#license)

---

## Overview

SFDaaS provides a RESTful web service for satellite orbit propagation. It performs numerical integration of satellite trajectories using OreKit's high-precision orbital mechanics models, with optional result caching via Memcached.

**Key Capabilities:**
- Numerical orbit propagation in J2000 Earth-centered frame
- Forward and backward time propagation
- Optional Memcached caching for performance
- RESTful HTTP API
- ISO-8601 epoch format support

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
# Run with embedded Tomcat. This task has dependencies that are executed before the run
task run

```

Then open: http://localhost:8080/SFDaaS/orekit/propagate/usage

#### Option B: Using Maven Directly

```bash
# Build
mvn clean package

# Run with embedded Tomcat
mvn tomcat7:run
```

#### Option C: Using Make (Redirects to Task)

```bash
# This will show instructions to use Task instead
make

# Install Task
make install-task
```

### Test the API

```bash
# Manually with curl
curl "http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-28T13:00:00.000&r0=\[3198022.67,2901879.73,5142928.95\]&v0=\[-6129.640631,4489.647187,1284.511245\]"

```

---

## Features

- **Orbit Propagation**: Numerical integration using classical Runge-Kutta integrator
- **Time Flexibility**: Propagate forwards or backwards in time
- **Reference Frame**: J2000 Earth-centered inertial frame
- **Epoch Format**: ISO-8601 standard (UTC timezone)
- **Caching**: Optional Memcached integration for performance
- **RESTful API**: Simple HTTP GET interface
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

### Build WAR File

```bash
# With Task (recommended)
task build

# With Maven
mvn clean package

# Output: target/SFDaaS.war (13 MB)
```

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
ls -lh target/SFDaaS.war
jar tf target/SFDaaS.war | head -20
```

---

## Deployment

### Option 1: Embedded Tomcat (Development)

**Foreground Mode:**
```bash
# With Task
task run

# With Maven
mvn tomcat7:run
```

**Background Mode:**
```bash
# With Task
task run     # Start
task status  # Check status
task logs    # View logs
task stop    # Stop server
```

Application available at: http://localhost:8080/SFDaaS/

### Option 2: External Tomcat (Production)

**Prerequisites:**
- Apache Tomcat 7+ installed
- `CATALINA_HOME` environment variable set

**Deploy:**
```bash
# With Task
task deploy-tomcat

# Manually
cp target/SFDaaS.war $CATALINA_HOME/webapps/

# Start Tomcat
$CATALINA_HOME/bin/startup.sh  # Linux/macOS
%CATALINA_HOME%\bin\startup.bat  # Windows
```

**Undeploy:**
```bash
# With Task
task undeploy-tomcat

# Manually
rm -f $CATALINA_HOME/webapps/SFDaaS.war
rm -rf $CATALINA_HOME/webapps/SFDaaS
```

---

## API Usage

### Endpoints

- **Usage Documentation**: `/SFDaaS/orekit/propagate/usage`
- **Propagation Service**: `/SFDaaS/orekit/propagate`

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

**Example Response:**

```bash
A priori state:
 t0 = 2010-05-28T12:00:00.000
 r0 = [3198022.67,2901879.73,5142928.95]
 v0 = [-6129.640631,4489.647187,1284.511245]

A posteriori state:
 tf = 2010-05-28T13:00:00.000
 rf = [-6174454.063243,2474544.324750,-976156.9807064387]
 vf = [-991.274325,-4808.930607,-5927.623934582873]

Run Properties:
 Run Start     : 2026-01-09T14:15:32.736-0500
 Run End       : 2026-01-09T14:15:32.755-0500
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
tf=2011-05-28T12:00:00.000&\
r0=[3198022.67,2901879.73,5142928.95]&\
v0=[-6129.640631,4489.647187,1284.511245]"
```

### Browser Access

Open in browser:

```bash
http://localhost:8080/SFDaaS/orekit/propagate/usage
```

Or test propagation (remove backslashes - shown for readability):

```bash
http://localhost:8080/SFDaaS/orekit/propagate?\
  t0=2010-05-28T12:00:00.000&\
  tf=2010-05-28T13:00:00.000&\
  r0=[3198022.67,2901879.73,5142928.95]&\
  v0=[-6129.640631,4489.647187,1284.511245]
```

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

**For Embedded Tomcat:**

```bash
# With Task
task run DATA_PATH=/custom/path/to/data

# With Maven
mvn tomcat7:run -Dorekit.data.path=/custom/path/to/data
```

**For External Tomcat:**

Create `$CATALINA_HOME/bin/setenv.sh` (Linux/macOS):
```bash
export JAVA_OPTS="$JAVA_OPTS -Dorekit.data.path=/path/to/SFDaaS/data"
```

Or `setenv.bat` (Windows):
```batch
set JAVA_OPTS=%JAVA_OPTS% -Dorekit.data.path=C:\path\to\SFDaaS\data
```

### Port Configuration

**Change default port (8080):**
```bash
# With Task
task run TOMCAT_PORT=8081

# With Maven
mvn tomcat7:run -Dmaven.tomcat.port=8081
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
| View logs | `task logs` |
| View dependencies | `task deps` |
| Verify WAR | `task verify` |
| Get help | `task help` |

**With Maven:**

| Task | Command |
|------|---------|
| Clean build | `mvn clean package` |
| Compile only | `mvn clean compile` |
| Run server | `mvn tomcat7:run` |
| View dependencies | `mvn dependency:tree` |

### Development Workflow

**Daily Development:**

```bash
# 1. Start server in background
task run

# 2. Make code changes...

# 3. Rebuild
task build

# 5. View logs if needed
task logs

# 6. Stop when done
task stop

```

### Project Structure

```bash
SFDaaS/
├── pom.xml                          # Maven build configuration
├── Taskfile.yaml                    # Task automation
├── Makefile                         # Redirects to Task with install helper
├── README.md                        # This file
├── Usage.html                       # Original usage documentation
│
├── src/
│   ├── org/spaceflightdynamics/    # Application code
│   │   ├── propagation/
│   │   │   └── Propagator.java     # Core propagation logic (updated for OreKit 11.3.3)
│   │   ├── servlets/
│   │   │   └── OreKitPropagate.java # Web servlet
│   │   └── utils/                   # Utility classes
│   │
│   ├── org/orekit/                  # Embedded OreKit 5.0.3 source (not compiled)
│   ├── org/apache/commons/          # Embedded Commons Math source (not compiled)
│   └── net/spy/memcached/           # Embedded Spymemcached source (not compiled)
│
├── data/                            # OreKit data files (UTC-TAI tables)
├── WebContent/                      # Web application resources
│   ├── META-INF/
│   └── WEB-INF/
│       └── web.xml
│
└── target/                          # Maven build output (excluded from git)
    └── SFDaaS.war                   # Built WAR file (13 MB)
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

**Problem: Maven not found**

```bash
# Install Maven
# macOS: brew install maven
# Ubuntu: sudo apt-get install maven
# Verify: mvn --version
```

### Server Issues

**Problem: Port 8080 already in use**

```bash
# Find process using port 8080
lsof -ti:8080

# Kill it
lsof -ti:8080 | xargs kill -9

# Or use different port
task run TOMCAT_PORT=8081
```

**Problem: Server won't start**

```bash
# Check status
task status

# View logs
task logs

# Stop and restart
task stop
task run
```

### API Issues

**Problem: 404 Not Found at root URL**

This is expected. The servlet is only mapped to:

- `/SFDaaS/orekit/propagate/usage`
- `/SFDaaS/orekit/propagate`

**Problem: "OreKit data path not found"**

```bash
# Verify data directory exists
ls -la data/

# Set data path explicitly
task run DATA_PATH=$(pwd)/data

# Or check configuration
echo $JAVA_OPTS

```

**Problem: Propagation returns errors**

```bash
# Check logs
task logs

# Verify OreKit data is loaded
# Look for "OreKit data" messages in logs
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
| **OreKit** | 11.3.3 | Space flight dynamics library |
| **Hipparchus** | 2.3 | Mathematical library (replaces Apache Commons Math) |
| **Spymemcached** | 2.12.3 | Memcached client for caching |
| **Servlet API** | 3.1.0 | Java web servlet framework |

**Historical Note:** The original project embedded OreKit 5.0.3 and Apache Commons Math 2.2 source code. The modernized version uses Maven to manage current versions as dependencies.

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

## License

**SFDaaS** is licensed under the **LGPL License version 3.0** by Haisam K. Ido.
See [LGPL-LICENSE.txt](LGPL-LICENSE.txt) for details.

**OreKit** ([www.orekit.org](https://www.orekit.org/)) is licensed by CS Communication & Systèmes under the **Apache License Version 2.0**.

**Apache Commons Math / Hipparchus** is licensed by the Apache Software Foundation under the **Apache License Version 2.0**.

**Spymemcached** is licensed under the **MIT License**.

---

## Resources

- **OreKit Documentation**: https://www.orekit.org/
- **Hipparchus Documentation**: https://hipparchus.org/
- **Apache Maven**: https://maven.apache.org/
- **Task**: https://taskfile.dev/
- **Apache Tomcat**: https://tomcat.apache.org/

---

## Support

For issues, questions, or contributions:

1. Check this README
2. See [Usage.html](Usage.html) for original API documentation
3. Review logs: `task logs`
4. Validate setup: `task validate`

---

**Built with OreKit 11.3.3 • Maven • Task • ❤️**

**Last Updated:** January 9, 2026
**Status:** ✅ Production Ready
