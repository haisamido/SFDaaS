# SFDaaS Makefile
# This project uses Task (https://taskfile.dev) for build automation
# Please use 'task' instead of 'make' for all build operations

.PHONY: help install-task all build run clean test

# Default target - show message about using Task
help:
	@echo ""
	@echo "=========================================="
	@echo "  SFDaaS Build System"
	@echo "=========================================="
	@echo ""
	@echo "This project uses Task for build automation."
	@echo "Please install Task and use 'task' commands instead of 'make'."
	@echo ""
	@echo "To install Task:"
	@echo "  make install-task    # Install Task using the appropriate method"
	@echo ""
	@echo "After installing Task, use these commands:"
	@echo "  task --list          # Show all available tasks"
	@echo "  task setup           # Initial setup and validation"
	@echo "  task build           # Build the project"
	@echo "  task run             # Run with embedded Tomcat"
	@echo "  task help            # Show detailed help"
	@echo ""
	@echo "For more information:"
	@echo "  - See QUICKSTART.md for quick start guide"
	@echo "  - See BUILD.md for detailed build instructions"
	@echo "  - Visit https://taskfile.dev for Task documentation"
	@echo ""

# Install Task based on the operating system
install-task:
	@echo "Installing Task..."
	@echo ""
	@if command -v brew >/dev/null 2>&1; then \
		echo "Detected Homebrew, installing Task..."; \
		brew install go-task/tap/go-task; \
	elif [ "$$(uname)" = "Linux" ] && command -v sh >/dev/null 2>&1; then \
		echo "Installing Task for Linux..."; \
		sh -c "$$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin; \
		echo ""; \
		echo "Task installed to ~/.local/bin/task"; \
		echo "Make sure ~/.local/bin is in your PATH"; \
	elif [ "$$(uname)" = "Darwin" ]; then \
		echo "Installing Task for macOS..."; \
		sh -c "$$(curl --location https://taskfile.dev/install.sh)" -- -d -b /usr/local/bin; \
	else \
		echo "Unable to detect package manager."; \
		echo "Please install Task manually from https://taskfile.dev/installation/"; \
		exit 1; \
	fi
	@echo ""
	@echo "Task installation complete!"
	@echo "Run 'task --version' to verify installation"
	@echo "Run 'task --list' to see available tasks"

# Catch-all targets that redirect to Task
all build run clean test compile package deploy:
	@echo ""
	@echo "ERROR: This project uses Task, not Make."
	@echo ""
	@echo "Please install Task first:"
	@echo "  make install-task"
	@echo ""
	@echo "Then use Task commands:"
	@echo "  task $@"
	@echo ""
	@echo "For all available tasks, run: task --list"
	@echo ""
	@exit 1
