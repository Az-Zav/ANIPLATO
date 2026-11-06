# Packaging ANIPLATO as a Windows program

`scripts\package-exe.ps1` builds a folder with `ANIPLATO.exe` in it. The folder carries its own Java, so the computer that runs
it needs no Java, Maven or MySQL. Zip the folder `target\installer\ANIPLATO` and give it out.

## Build (the person making the package)

Needs a JDK 17 or newer with `jpackage` on PATH (this project builds with JDK 25) and Maven.

    .\scripts\package-exe.ps1

It runs `mvn -Pdist package`, which leaves `db.properties` out of the program (it holds the password), then `jpackage`.
The first run takes several minutes.

## Run (everyone else)

1. Unzip the folder anywhere and keep its contents together.
2. Create the settings file `%APPDATA%\ANIPLATO\db.properties` (paste that into Explorer's address bar). Copy the lines from
   `db.properties.online.example` and fill in the host, port, user and password you were sent.
3. Put the Aiven `ca.pem` in the same folder and set `ssl.ca=ca.pem` (a name with no folder is looked for beside
   `db.properties`).
4. Double-click `ANIPLATO.exe`.

If the settings file is missing, the sign-in screen says where to put it. A wrong password or no internet shows
"Cannot reach the database" instead of a crash.

## Notes

- The package is an app folder, not an installer. `jpackage --type exe` (a Setup.exe) needs the WiX Toolset installed; add it
  later if an installer is wanted.
- Windows may show a "Windows protected your PC" prompt the first time, because the program is not code-signed. Choose
  More info, then Run anyway.
- Photos and documents are fetched from the database and cached in the user's temp folder (`aniplato-cache`).
