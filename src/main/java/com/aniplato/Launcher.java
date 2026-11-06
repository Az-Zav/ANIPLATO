package com.aniplato;

/**
 * The entry point of the packaged app (ANIPLATO.exe). JavaFX refuses to start from a class that itself extends
 * Application when its libraries are not on the module path, so the launcher is a plain class that hands over to Main.
 */
public final class Launcher {

    private Launcher() { }

    public static void main(String[] args) {
        Main.main(args);
    }
}
