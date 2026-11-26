# Blasphemeows

A [libGDX](https://libgdx.com/) action game with a Spring Boot backend for accounts, scores, and a leaderboard.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `backend`: Spring Boot API (JWT auth, score history, public leaderboard). See [backend/README.md](backend/README.md).

## Online scores (game ↔ backend)

1. Start Postgres + API (from repo root: `docker compose up -d db`, then `cd backend && gradlew.bat bootRun`).
2. Run the game: `gradlew.bat lwjgl3:run`.
3. In-game keys:
   - **F7** register / **F6** login (demo user `player1` / `password`)
   - Defeat enemies to accumulate score (shown on the HUD)
   - Score uploads automatically on death, or press **F9** to submit now
   - **F8** fetch top leaderboard entries into the combat log

The client talks to `http://localhost:8080` by default. Override with `-Dblasphemeows.apiBaseUrl=https://your-host`.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
