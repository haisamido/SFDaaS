# SFDaaS Documentation

This directory contains detailed documentation for the SFDaaS (Space Flight Dynamics as a Service) project.

## Available Documentation

### [NETTY-MIGRATION.md](NETTY-MIGRATION.md)

Complete documentation of the migration from Tomcat servlet-based architecture to standalone Netty server (January 2026).

**Contents:**

- Migration overview and timeline
- Architectural changes (WAR → JAR, Servlet → Netty)
- API changes (HTML → JSON responses)
- Session management updates
- Static file serving implementation
- Performance comparisons
- Breaking changes and compatibility
- Deployment guide
- Troubleshooting

---

## Other Documentation Locations

### Testing Documentation

Comprehensive testing guides and scripts are located in the `tests/` directory:
- **[tests/README.md](../tests/README.md)** - Testing suite overview and script documentation
- **[tests/STRESS_TESTING.md](../tests/STRESS_TESTING.md)** - Detailed stress testing guide with 10+ testing approaches

### Main Documentation
- **[README.md](../README.md)** - Main project documentation with quick start, API usage, and deployment
- **API Documentation** - Available at runtime: `http://localhost:8080/SFDaaS/orekit/propagate/usage`

### Deployment Documentation
- **[deployments/README.md](../deployments/README.md)** - Deployment configurations and containerization

---

## Documentation Structure

```
SFDaaS/
├── README.md                    # Main project documentation
├── docs/                        # Detailed documentation (this directory)
│   ├── README.md                # This file
│   └── NETTY-MIGRATION.md       # Tomcat to Netty migration guide
├── tests/                       # Testing documentation and scripts
│   ├── README.md                # Testing suite documentation
│   └── STRESS_TESTING.md        # Comprehensive stress testing guide
└── deployments/                 # Deployment documentation
    └── README.md                # Deployment configurations
```

---

## Contributing Documentation

When adding new documentation:

1. **Create markdown files** in this directory for detailed guides
2. **Update this README** to include the new documentation
3. **Link from main README.md** if it's a major feature or guide
4. **Use clear section headings** and table of contents for long documents
5. **Include code examples** where applicable
6. **Keep documents focused** - one topic per document

---

## Documentation Standards

- Use GitHub-flavored Markdown
- Include a table of contents for documents > 200 lines
- Use code blocks with language specification
- Include examples and usage patterns
- Keep line length reasonable (80-120 characters)
- Use relative links for internal documentation
- Include date/version information for time-sensitive docs

---

## Quick Links

### For Users
- [Main README](../README.md) - Start here
- [API Usage](../README.md#api-usage) - How to use the API
- [Quick Start](../README.md#quick-start) - Get started in 5 minutes

### For Developers
- [Development Guide](../README.md#development) - Development workflow
- [Testing Suite](../tests/README.md) - Run tests and stress tests
- [Migration Guide](NETTY-MIGRATION.md) - Understand the architecture

### For Operators
- [Deployment Guide](../README.md#deployment) - Deploy SFDaaS
- [Troubleshooting](../README.md#troubleshooting) - Common issues and solutions
- [Stress Testing](../tests/STRESS_TESTING.md) - Performance testing

---

## License

All documentation is part of the SFDaaS project and is licensed under LGPL-3.0.
