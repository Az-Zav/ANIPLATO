# Packaging ANIPLATO as a Windows program

`scripts\package-exe.ps1` builds a folder with `ANIPLATO.exe` in it. The folder carries its own Java, so the computer that runs
it needs no Java or Maven. Zip the folder `target\installer\ANIPLATO` and publish it (for example as a GitHub Release).

The package holds **no database login and no key**. Each person brings their own MySQL, and the program's Setup screen
(Spec 6.8) asks for it on first run. See [SELF-HOSTING.md](SELF-HOSTING.md) for what the person does.

## Build (the person making the package)

Needs a JDK 17 or newer with `jpackage` on PATH (this project builds with JDK 25) and Maven. Close any running copy of the app
first (a running copy locks the old build).

    .\scripts\package-exe.ps1

It runs `mvn -Pdist package`, which leaves `db.properties` out of the program, then `jpackage`, then adds to the folder:

| File | For |
|---|---|
| `SETUP.txt` | the plain-words guide a person reads first (from `packaging\SETUP.txt`) |
| `db.properties.example` | a provider-neutral settings template for the by-hand route (from `packaging\db.properties.template`) |
| `sql\schema.sql`, `sql\seed.sql` | the by-hand route: tables only, and one admin. The Setup screen needs neither: the schema is also inside the program. |

The first run takes several minutes. Check the result before publishing:

- the jar has no `db.properties` (`unzip -l target\installer\ANIPLATO\app\aniplato-*.jar | findstr db.properties` shows only the
  two `.example` files),
- `ANIPLATO.exe` with no settings opens the Setup screen, and with a `db.properties` beside it opens Sign In,
- the folder has no `db.properties`, no `ca.pem` and no key.

## How a new user gets running

1. Unzip, double-click `ANIPLATO.exe` (Windows may warn: More info, then Run anyway).
2. The Setup screen asks for their MySQL (host, port, database, user, password, optional certificate), tests it, and creates the
   tables and their admin account, or connects to a database that is already set up. It never deletes or replaces data.
3. It saves `%APPDATA%\ANIPLATO\db.properties`. The program looks for settings there first, then beside the exe, then in the
   working folder; a `-Daniplato.<key>` system property overrides any of them.
4. Optional keys (the help assistant, email) go on the Setup screen or into that file.

## Notes

- The package is an app folder, not an installer. `jpackage --type exe` (a Setup.exe) needs the WiX Toolset installed; add it
  later if an installer is wanted.
- Windows may show a "Windows protected your PC" prompt the first time, because the program is not code-signed.
- Photos and documents are stored in the database and cached in the user's temp folder (`aniplato-cache`).
- The assistant needs internet and the user's own key. Without them it says it is not set up and the rest of the app is unaffected.
- A shared team database is still possible: give each person their own database user and have them choose **Connect** on the Setup
  screen. Do not put a team login in a public download.
