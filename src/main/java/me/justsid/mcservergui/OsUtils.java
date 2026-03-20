package me.justsid.mcservergui;

/**
 * Plattform-Hilfsmethoden. Kapselt OS-Erkennung an einem einzigen Ort,
 * damit kein Modul den System.getProperty("os.name")-Ausdruck dupliziert.
 */
public final class OsUtils {

    public static final boolean IS_WINDOWS;
    public static final boolean IS_MAC;
    /** Dateiname des Java-Executables auf diesem Betriebssystem. */
    public static final String JAVA_EXE;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        IS_WINDOWS = osName.contains("win");
        IS_MAC     = osName.contains("mac");
        JAVA_EXE   = IS_WINDOWS ? "java.exe" : "java";
    }

    private OsUtils() {}
}
