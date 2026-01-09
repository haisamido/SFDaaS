# Building and Deploying SFDaaS

This guide explains how to build and deploy the Space Flight Dynamics as a Service (SFDaaS) application.

## Prerequisites

1. **Java Development Kit (JDK) 8 or higher**
   - Check your version: `java -version`
   - Install if needed: [OpenJDK](https://openjdk.org/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)

2. **Apache Maven**
   - Check your version: `mvn -version`
   - Install if needed:
     - macOS: `brew install maven`
     - Linux: `sudo apt-get install maven` or `sudo yum install maven`
     - Windows: Download from [Apache Maven](https://maven.apache.org/download.cgi)

3. **Apache Tomcat 7.x or higher** (for deployment)
   - Download from: [Apache Tomcat](https://tomcat.apache.org/download-90.cgi)
   - Extract to a directory (e.g., `/opt/tomcat` or `C:\tomcat`)

## Project Structure

```
SFDaaS/
├── src/                          # Java source files
│   ├── org/spaceflightdynamics/  # Main application code
│   ├── org/orekit/               # OreKit library source
│   └── net/spy/memcached/        # Spymemcached library source
├── data/                         # OreKit data files (UTC-TAI tables, etc.)
├── WebContent/                   # Web application resources
│   └── META-INF/
├── Usage.html                    # Usage documentation
├── pom.xml                       # Maven build configuration
└── BUILD.md                      # This file
```

## Building the Application

### Option 1: Build WAR file with Maven (Recommended)

1. **Clean and build the project:**
   ```bash
   mvn clean package
   ```

   This will:
   - Compile all Java source files
   - Package everything into a WAR file
   - Create `target/SFDaaS.war`

2. **The WAR file will be located at:**
   ```
   target/SFDaaS.war
   ```

### Option 2: Run with Embedded Tomcat (for testing)

Maven can run the application with an embedded Tomcat server:

```bash
mvn clean tomcat7:run
```

The application will be available at: `http://localhost:8080/SFDaaS/orekit/propagate/usage`

**Note:** This uses the `tomcat7-maven-plugin` and is suitable for development/testing only.

## Deployment to Tomcat

### Method 1: Manual WAR Deployment

1. **Build the WAR file:**
   ```bash
   mvn clean package
   ```

2. **Stop Tomcat** (if running):
   ```bash
   # Linux/macOS
   $CATALINA_HOME/bin/shutdown.sh

   # Windows
   %CATALINA_HOME%\bin\shutdown.bat
   ```

3. **Copy the WAR file to Tomcat's webapps directory:**
   ```bash
   cp target/SFDaaS.war $CATALINA_HOME/webapps/
   ```

4. **Start Tomcat:**
   ```bash
   # Linux/macOS
   $CATALINA_HOME/bin/startup.sh

   # Windows
   %CATALINA_HOME%\bin\startup.bat
   ```

5. **Access the application:**
   - Usage page: `http://localhost:8080/SFDaaS/orekit/propagate/usage`
   - Test propagation: `http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]`

### Method 2: Tomcat Manager Deployment

If you have Tomcat Manager configured:

```bash
mvn tomcat7:deploy
```

Or to redeploy an existing application:

```bash
mvn tomcat7:redeploy
```

**Note:** This requires Tomcat Manager credentials configured in your Maven `settings.xml`.

## Configuration

### OreKit Data Path

The application needs access to OreKit data files (UTC-TAI tables, etc.). By default, it looks for a `data/` directory in the working directory.

You can override this by setting a system property:

1. **For Tomcat, edit `$CATALINA_HOME/bin/setenv.sh` (Linux/macOS) or `setenv.bat` (Windows):**

   ```bash
   # Linux/macOS (setenv.sh)
   export JAVA_OPTS="$JAVA_OPTS -Dorekit.data.path=/path/to/SFDaaS/data"
   ```

   ```batch
   rem Windows (setenv.bat)
   set JAVA_OPTS=%JAVA_OPTS% -Dorekit.data.path=C:\path\to\SFDaaS\data
   ```

2. **For embedded Tomcat (development):**
   ```bash
   mvn tomcat7:run -Dorekit.data.path=/path/to/SFDaaS/data
   ```

### Memcached (Optional)

To use caching features, you need a memcached server running:

```bash
# Install memcached
# macOS
brew install memcached

# Linux
sudo apt-get install memcached

# Start memcached
memcached -d -m 64 -p 11211
```

## Testing the Application

### 1. View Usage Documentation

Open in browser:
```
http://localhost:8080/SFDaaS/orekit/propagate/usage
```

### 2. Test Basic Propagation

```bash
curl "http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=\[3198022.67,2901879.73,5142928.95\]&v0=\[-6129.640631,4489.647187,1284.511245\]"
```

Expected output should include:
```
A priori state:
 t0 = 2010-05-28T12:00:00.000
 r0 = [3198022.67,2901879.73,5142928.95]
 v0 = [-6129.640631,4489.647187,1284.511245]

A posteriori state:
 tf = 2011-05-28T12:00:00.000
 rf = [final x, y, z coordinates]
 vf = [final vx, vy, vz velocities]
```

### 3. Test with Memcached

```bash
curl "http://localhost:8080/SFDaaS/orekit/propagate?cf=1&ca=127.0.0.1:11211&t0=2010-05-28T12:00:00.000&tf=2011-05-28T12:00:00.000&r0=\[3198022.67,2901879.73,5142928.95\]&v0=\[-6129.640631,4489.647187,1284.511245\]"
```

## Troubleshooting

### Issue: "ClassNotFoundException: org.orekit..."

**Solution:** Ensure Maven successfully downloaded all dependencies. Run:
```bash
mvn clean install -U
```

### Issue: "OreKit data path not found"

**Solution:**
1. Verify the `data/` directory exists and contains OreKit data files
2. Check the data path configuration (see Configuration section)
3. Set the `orekit.data.path` system property explicitly

### Issue: Compilation errors with OreKit version

**Solution:** The project originally used OreKit 5.0.3 (very old). The `pom.xml` uses OreKit 11.3.3 which has API changes. You may need to:

1. Update the code to use the new OreKit API, or
2. Install old OreKit 5.0.3 manually:
   ```bash
   # Download and install old version manually to local Maven repo
   mvn install:install-file -Dfile=orekit-5.0.3.jar \
     -DgroupId=org.orekit -DartifactId=orekit -Dversion=5.0.3 \
     -Dpackaging=jar
   ```

   Then update `pom.xml` to use version 5.0.3

### Issue: Port 8080 already in use

**Solution:** Either:
1. Stop the process using port 8080
2. Use a different port: `mvn tomcat7:run -Dmaven.tomcat.port=8081`

## Development Workflow

1. **Make code changes**
2. **Rebuild:** `mvn clean package`
3. **Test locally:** `mvn tomcat7:run`
4. **Deploy to Tomcat:** Copy `target/SFDaaS.war` to Tomcat's `webapps/`

## Additional Resources

- **OreKit Documentation:** https://www.orekit.org/
- **Apache Tomcat:** https://tomcat.apache.org/
- **Maven WAR Plugin:** https://maven.apache.org/plugins/maven-war-plugin/
- **Original Usage:** See [Usage.html](Usage.html)
