package xyz.nulldev.androidcompat.replace.java.text;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Desktop target for references rewritten by {@code BytecodeEditor}.
 *
 * Android extensions cannot keep references to {@code java.text.SimpleDateFormat}
 * in converted bytecode, so the converter relocates them into this package. The
 * desktop implementation can safely delegate to the JDK implementation.
 */
public class SimpleDateFormat extends java.text.SimpleDateFormat {
    private static final long serialVersionUID = 1L;

    public SimpleDateFormat() {
        super();
    }

    public SimpleDateFormat(String pattern) {
        super(pattern);
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        super(pattern, locale);
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        super(pattern, formatSymbols);
    }
}
