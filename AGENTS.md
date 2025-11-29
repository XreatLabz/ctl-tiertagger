# AGENTS.md

## Build and Commit Workflow

1. Always run `./gradlew build` before committing any changes
2. If the build succeeds, commit and push the changes
3. If the build fails, fix the errors before committing

## Discord Webhook

The project is configured to automatically upload the built JAR to Discord after every successful build. The webhook URL is stored in `.env` (not committed to git).
