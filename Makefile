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

install-task:
	@echo "Installing Task..."
	@echo ""
	@echo "Installing Task for Linux..."; \
	sh -c "$$(curl --location https://taskfile.dev/install.sh)" -- -d -b ~/.local/bin; \
	echo ""; \
	echo "Task installed to ~/.local/bin/task"; \
	echo "Make sure ~/.local/bin is in your PATH";
	@echo ""
	@echo "Task installation complete!"
	@echo "Run 'task --version' to verify installation"
	@echo "Run 'task --list' to see available tasks"
