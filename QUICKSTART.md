# SFDaaS Quick Start Guide

## What We've Accomplished

The Space Flight Dynamics as a Service (SFDaaS) project has been successfully modernized and is now ready to compile and deploy!

### Changes Made

1. **Created Maven Build System** ([pom.xml](pom.xml))
   - Modern dependency management
   - Updated to OreKit 11.3.3 and Hipparchus (successor to Apache Commons Math)
   - Automatic dependency resolution for all required libraries

2. **Fixed Code Compatibility Issues**
   - Updated [Propagator.java](src/org/spaceflightdynamics/propagation/Propagator.java:7) to use Hipparchus instead of Apache Commons Math
   - Made OreKit data path configurable via system property
   - Removed deprecated PropagationException

3. **Created Documentation**
   - Comprehensive build guide ([BUILD.md](BUILD.md))
   - This quick start guide

## Quick Start: Build and Run

### Option A: Using Task Runner (Recommended)

If you don't have [Task](https://taskfile.dev/) installed yet:

```bash
# Install Task using the Makefile
make install-task

# Or install manually:
# macOS: brew install go-task/tap/go-task
# Linux: sh -c "$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin
```

Once Task is installed:

```bash
# Show all available tasks
task --list

# Quick setup and validation
task setup

# Build the application
task build

# Run with embedded Tomcat
task run
```

**Note:** If you run `make` commands, you'll be redirected to use `task` instead.

### Option B: Using Maven Directly

### 1. Build the WAR File

```bash
mvn clean package
```

✅ **Success!** WAR file created at: `target/SFDaaS.war` (13 MB)

### 2. Deploy to Tomcat

#### Option A: Using Embedded Tomcat (Quick Test)

```bash
mvn tomcat7:run
```

Then open: http://localhost:8080/SFDaaS/orekit/propagate/usage

#### Option B: Deploy to Your Tomcat Server

1. Copy the WAR file:
   ```bash
   cp target/SFDaaS.war $CATALINA_HOME/webapps/
   ```

2. Start Tomcat:
   ```bash
   $CATALINA_HOME/bin/startup.sh  # Linux/macOS
   # OR
   %CATALINA_HOME%\bin\startup.bat  # Windows
   ```

3. Access the application:
   - Usage page: http://localhost:8080/SFDaaS/orekit/propagate/usage
   - Test propagation: http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]

### 3. Verify It Works

Test with curl:

```bash
curl "http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-28T13:00:00.000&r0=\[3198022.67,2901879.73,5142928.95\]&v0=\[-6129.640631,4489.647187,1284.511245\]"
```

Expected output should include:
```
A priori state:
 t0 = 2010-05-28T12:00:00.000
 r0 = [3198022.67,2901879.73,5142928.95]
 v0 = [-6129.640631,4489.647187,1284.511245]

A posteriori state:
 tf = 2010-05-28T13:00:00.000
 rf = [final coordinates]
 vf = [final velocities]
```

## Configuration

### OreKit Data Path

By default, the application looks for OreKit data in the `data/` directory. To customize:

**For Tomcat**, create `$CATALINA_HOME/bin/setenv.sh`:
```bash
export JAVA_OPTS="$JAVA_OPTS -Dorekit.data.path=/path/to/SFDaaS/data"
```

**For embedded Tomcat**:
```bash
mvn tomcat7:run -Dorekit.data.path=/path/to/SFDaaS/data
```

## Project Structure

```
SFDaaS/
├── pom.xml                                    # Maven configuration
├── BUILD.md                                   # Detailed build instructions
├── QUICKSTART.md                              # This file
├── Usage.html                                 # Original usage documentation
├── src/org/spaceflightdynamics/
│   ├── propagation/Propagator.java           # Core propagation logic
│   ├── servlets/OreKitPropagate.java         # Web servlet
│   └── utils/                                 # Utility classes
├── data/                                      # OreKit data files (UTC-TAI tables)
├── WebContent/                                # Web application resources
└── target/
    └── SFDaaS.war                            # Built WAR file (13 MB)
```

## Common Commands

### With Task Runner (Recommended)

| Task | Command |
|------|---------|
| Setup & validate | `task setup` |
| Clean build | `task build` |
| Compile only | `task compile` |
| Run with embedded Tomcat | `task run` |
| Run in background | `task run-background` |
| Stop background server | `task stop` |
| Test API | `task test-api` |
| Check status | `task status` |
| Deploy to external Tomcat | `task deploy-tomcat` |
| View dependencies | `task deps` |
| Show all tasks | `task --list` |
| Get help | `task help` |

**See [TASKFILE_README.md](TASKFILE_README.md) for complete task documentation.**

To install Task: `brew install go-task/tap/go-task` (macOS) or visit https://taskfile.dev/installation/

### With Maven Directly

| Task | Command |
|------|---------|
| Clean build | `mvn clean package` |
| Compile only | `mvn clean compile` |
| Run with embedded Tomcat | `mvn tomcat7:run` |
| Deploy to external Tomcat | `cp target/SFDaaS.war $CATALINA_HOME/webapps/` |
| View dependencies | `mvn dependency:tree` |

## API Usage

### Basic Propagation

**Endpoint:** `/SFDaaS/orekit/propagate`

**Parameters:**
- `t0` - Initial epoch (format: `YYYY-MM-DDTHH:MM:SS.SSS`, UTC timezone)
- `tf` - Final epoch (same format)
- `r0` - Initial position vector `[x,y,z]` in meters (J2000 Earth-centered frame)
- `v0` - Initial velocity vector `[vx,vy,vz]` in meters/second

**Example:**
```
http://localhost:8080/SFDaaS/orekit/propagate?
  t0=2010-05-28T12:00:00.000&
  tf=2011-05-28T12:00:00.000&
  r0=[3198022.67,2901879.73,5142928.95]&
  v0=[-6129.640631,4489.647187,1284.511245]
```

### With Memcached (Optional)

**Additional Parameters:**
- `cf=1` - Enable caching
- `ca=127.0.0.1:11211` - Memcached server address and port
- `ct=60` - Cache expiration time in seconds (default: 60)
- `ck={KEY}` - Custom cache key (optional)

**Example:**
```
http://localhost:8080/SFDaaS/orekit/propagate?
  cf=1&
  ca=127.0.0.1:11211&
  ct=60&
  t0=2010-05-28T12:00:00.000&
  tf=2011-05-28T12:00:00.000&
  r0=[3198022.67,2901879.73,5142928.95]&
  v0=[-6129.640631,4489.647187,1284.511245]
```

## Dependencies

The project now uses Maven to manage these dependencies:

| Library | Version | Purpose |
|---------|---------|---------|
| OreKit | 11.3.3 | Space flight dynamics library |
| Hipparchus | 2.3 | Mathematical library (replaces Apache Commons Math) |
| Spymemcached | 2.12.3 | Memcached client |
| Servlet API | 3.1.0 | Java web servlets |

All dependencies are automatically downloaded by Maven during the build process.

## Troubleshooting

### Build fails with "cannot find symbol"
Make sure you're using Java 8 or higher:
```bash
java -version
```

### "OreKit data path not found" error
Ensure the `data/` directory exists and contains OreKit data files, or set the `orekit.data.path` system property.

### Port 8080 already in use
Either stop the process using port 8080, or use a different port:
```bash
mvn tomcat7:run -Dmaven.tomcat.port=8081
```

## Next Steps

- See [BUILD.md](BUILD.md) for detailed build instructions and deployment options
- Check [Usage.html](Usage.html) for original usage examples
- Customize OreKit data path for your environment
- Set up memcached for caching capabilities
- Review the code in [src/org/spaceflightdynamics/](src/org/spaceflightdynamics/)

## Need Help?

- **Build Issues:** See [BUILD.md](BUILD.md) troubleshooting section
- **OreKit Documentation:** https://www.orekit.org/
- **Maven Documentation:** https://maven.apache.org/guides/

---

**Built successfully on:** January 9, 2026
**WAR file location:** `target/SFDaaS.war` (13 MB)
**Ready to deploy!** 🚀
