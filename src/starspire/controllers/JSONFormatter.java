package starspire.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * JSON log format for soft data log
 * @author Patrick Fiaux
 */
class JSONFormatter extends Formatter {
    private static final SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    private boolean start = true;

    /**
     * TODO
     * @param rec
     * @return
     */
    public String format(LogRecord rec) {
        StringBuilder buf = new StringBuilder(1000);

        if (start) {
            start = false;
        } else {
            buf.append(',');
        }

        buf.append("\t{\n");

        buf.append("\t\t\"level\":");
        buf.append(rec.getLevel());
        buf.append(",\n");

        buf.append("\t\t\"time\":");
        buf.append(calcDate(rec.getMillis()));
        buf.append(",\n\t\t");

        buf.append("\t\t\"message\":");
        buf.append(formatMessage(rec));
        buf.append("\n");

        buf.append("\t}\n");

        return buf.toString();
    }

    /**
     * TODO
     * @param millisecs
     * @return
     */
    private String calcDate(long millisecs) {
        Date resultdate = new Date(millisecs);
        return date_format.format(resultdate);
    }

    /**
     * TODO
     * @param h
     * @return
     */
    @Override
    public String getHead(Handler h) {
        return "{\n" + "Creation date" + (new Date()) + ",\n"
                + "Messages: [\n";
    }

    /**
     * TODO
     * @param h
     * @return
     */
    @Override
    public String getTail(Handler h) {
        return "]}\n";
    }
}
