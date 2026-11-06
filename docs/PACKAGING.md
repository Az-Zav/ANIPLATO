# Packaging ANIPLATO as a Windows program

`scripts\package-exe.ps1` builds a folder with `ANIPLATO.exe` in it. The folder carries its own Java, so the computer that runs
it needs no Java, Maven or MySQL. Zip the folder `target\installer\ANIPLATO` and give it out.

## Build (the person making the package)

Needs a JDK 17 or newer with `jpackage` on PATH (this project builds with JDK 25) and Maven. Close any running copy of the app
first (a running copy locks the old build).

    .\scripts\package-exe.ps1

It runs `mvn -Pdist package`, which leaves `db.properties` out of the program (it holds the password), then `jpackage`, then
drops a fill-in-the-blanks `db.properties.example` beside `ANIPLATO.exe`. The first run takes several minutes.

## Run (everyone else)

1. Unzip the folder anywhere and keep its contents together.
2. Make the settings file `db.properties` in **one** of these places (the first one found is used):
   - `%APPDATA%\ANIPLATO\db.properties` (paste that into Explorer's address bar; survives replacing the program folder), or
   - the program folder, next to `ANIPLATO.exe` (copy `db.properties.example` and rename it).
3. Fill in the lines you were sent: `url` (host and port), `user` and `password`. Leave `ssl.ca=ca.pem`.
4. Put the Aiven `ca.pem` in the same folder as `db.properties`. A file name with no folder is looked for beside
   `db.properties`; an absolute path also works, written with forward slashes (`C:/Users/me/ca.pem`).
5. Optional, for the help assistant: make your own free key at https://aistudio.google.com/app/apikey and paste it after
   `ai.gemini.api-key=`. Without it the assistant says it is not set up and the rest of the app works.
6. Double-click `ANIPLATO.exe`. Settings are read at start, so close and reopen the program after changing the file.

If the settings file is missing, the sign-in screen says where to put it. A wrong password or no internet shows
"Cannot reach the database" instead of a crash.

### What goes in the file

| Key | What it is | Who gives it to you |
|---|---|---|
| `url`, `user`, `password` | the shared online database login | the project owner, privately |
| `ssl.ca` | `ca.pem`, the database's certificate, in the same folder | the project owner |
| `session.timezone`, `pool.size`, `pool.minIdle` | already right in the example; leave them | |
| `mail.*` | optional email (registration results, temporary passwords) | your own account |
| `ai.gemini.api-key` | optional, for the help assistant | you, free |

Never put the password or the key in a file that is shared or committed, and give each person their own login where you can.

## Notes

- The package is an app folder, not an installer. `jpackage --type exe` (a Setup.exe) needs the WiX Toolset installed; add it
  later if an installer is wanted.
- Windows may show a "Windows protected your PC" prompt the first time, because the program is not code-signed. Choose
  More info, then Run anyway.
- Photos and documents are fetched from the database and cached in the user's temp folder (`aniplato-cache`).
- The assistant needs internet. If it is busy or the key is missing, it says so and the rest of the app is unaffected.
