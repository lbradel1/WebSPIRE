package starspire;

//import java.io.OutputStream;

/**
 * The debug class is a wrapper for output.
 * It will allow us to print to system.out but will also enable logging. Not only that it will let us
 * disable console output for everything in a central place.
 * @author Patrick Fiaux
 */
public class Debug {
    private static boolean enabled = false;
    private static boolean enable_print = true;
    private static boolean enable_error_print = true;
    private static boolean initialized = false;

//    private static OutputStream printStream;
//    private static OutputStream errorStream;

    /**
     * initialize with System.out and System.err as main streams and enables.
     */
    public static void init() {
        enabled = true;
//        printStream = System.out;
//        printStream = System.err;
        initialized = true;
    }

    /**
     * Helper method allowing the JUnit test to 'reset'
     * the static class between tests.
     */
    protected static void reset() {
        enabled = false;
        enable_print = true;
        enable_error_print = true;
        initialized = false;
//        printStream = null;
//        errorStream = null;
    }

    /**
     * Enables or disables the debug prints/logs all together.
     * @param state true for enabling false for disabling everything.
     */
    public static void setEnabled(boolean state) {
        if (!initialized) {
            init();
        }
        enabled = state;
    }

    /**
     * Returns true if the debug is enabled or not.
     * @return boolean true for enabled false otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Disables or Enables just the system.out.print and system.out.println.
     * @param status true to enable false to disable.
     */
    public static void setEnablePrints(boolean status) {
        enable_print = status;
    }

    /**
     * Tells whether the system.out prints are enabled or disabled.
     * @return true if enabled, false otherwise.
     */
    public static boolean getEnablePrints() {
        return enable_print;
    }

    /**
     * Disables or Enables just the system.err prints
     * @param status true to enable false to disable.
     */
    public static void setEnableErrorPrints(boolean status) {
        enable_error_print = status;
    }

    /**
     * Tells whether the system.err prints are enabled or disabled.
     * @return true if enabled, false otherwise.
     */
    public static boolean getEnableErrorPrints() {
        return enable_error_print;
    }

    /**
     * Prints a string to the active steams and logs.
     * This is equivalent to System.out.print(String s)
     * @param s String to print.
     */
    public static void print(String s) {
        if (enabled) {
            if (enable_print) {
                System.out.print(s);
            }
        }
    }

    /**
     * Prints a string and new line to the active steams and logs.
     * This is equivalent to System.out.println(String s)
     * @param s String to print with a new line
     */
    public static void println(String s) {
        if (enabled) {
            if (enable_print) {
                System.out.println(s);
            }
        }
    }

    /**
     * Prints an error string
     * @param s String to print
     */
    public static void error(String s) {
        if (enabled) {
            if (enable_error_print) {
                System.err.print(s);
            }
        }
    }

    /**
     * Prints an error string with a line end
     * @param s string to print as error
     */
    public static void errorln(String s) {
        if (enabled) {
            if (enable_error_print) {
                System.err.println(s);
            }
        }
    }
}
