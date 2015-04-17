/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starspire;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author thepunksnowman
 */
public class StarSpireUtility {

    public final static FileNameExtensionFilter EXT_FILTER_JIG = new FileNameExtensionFilter("Jigsaw", "jig");
    public final static FileNameExtensionFilter EXT_FILTER_TXT = new FileNameExtensionFilter("Text", "txt");
    public final static FileNameExtensionFilter EXT_FILTER_CSV = new FileNameExtensionFilter("CSV", "csv");
    public final static FileNameExtensionFilter EXT_FILTER_JSON = new FileNameExtensionFilter("JSON", "json");

    /**
     * Tries to use the native file dialog to open a file.
     * @param message Open dialog message.
     * @param pathPreference preference address of the last location to use.
     * @param ext Extension filter to use.
     * @return File if found, null otherwise.
     */
    public static File openFile(String message, String pathPreference, FileNameExtensionFilter ext) {
        Preferences prefs = StarSpireApp.getPrefs();
        String dir = prefs.get(pathPreference, "");
        File file = null;
        if (StarSpireApp.isMacOS()) {
            Frame frame = null;
            FileDialog f = new FileDialog(frame, message, FileDialog.LOAD);
            String fileType = "*." + ext.getExtensions()[0];
            f.setFile(fileType);
            f.setDirectory(dir);
            f.setVisible(true);
            /**
             * Check Result
             */
            String fstring = f.getFile();
            dir = f.getDirectory();
            if (fstring != null) {
                file = new File(dir + fstring);
                prefs.put(pathPreference, dir);
            }

        } else {
            //System.out.println("using non mac file chooser");
            JFileChooser f = new JFileChooser();

            f.setMultiSelectionEnabled(false);
            f.setDialogTitle(message);
            f.setFileFilter(ext);
            if (!dir.equals("")) {
                System.out.println("Setting dir to LOP: " + dir);
                f.setCurrentDirectory(new File(dir));
            }
            int result = f.showOpenDialog(null);

            /**
             * Check Result
             */
            if (result == JFileChooser.APPROVE_OPTION) {
                prefs.put(pathPreference, f.getCurrentDirectory().getAbsolutePath());
                file = f.getSelectedFile();
                System.out.println(file.getAbsoluteFile());
            }
        }
        return file;
    }

    /**
     * Tries to use the native file dialog to open a file. This uses mostly JFileChooser
     * regardless since FileDialog doesn't support multiple files...
     * @param message Open dialog message.
     * @param pathPreference preference address of the last location to use.
     * @param ext Extension filter to use.
     * @return Files if found, null otherwise.
     */
    public static File[] openFiles(String message, String pathPreference, FileNameExtensionFilter ext) {
        Preferences prefs = StarSpireApp.getPrefs();
        String dir = prefs.get(pathPreference, "");
        File[] files = null;
        JFileChooser f = new JFileChooser();

        f.setMultiSelectionEnabled(true);
        f.setDialogTitle(message);
        f.setFileFilter(ext);
        if (!dir.equals("")) {
            System.out.println("Setting dir to LOP: " + dir);
            f.setCurrentDirectory(new File(dir));
        }
        int result = f.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            prefs.put(pathPreference, f.getCurrentDirectory().getAbsolutePath());
            files = f.getSelectedFiles();
        }
        return files;
    }

    /**
     * Tries to use the native file dialog to save a file.
     * @param message save dialog message.
     * @param pathPreference preference address of the last location to use.
     * @param ext Extension filter to use.
     * @return File if found, null otherwise.
     */
    public static File saveFile(String message, String pathPreference, FileNameExtensionFilter ext) {
        Preferences prefs = StarSpireApp.getPrefs();
        String dir = prefs.get(pathPreference, "");
        File file = null;
        if (StarSpireApp.isMacOS()) {
            Frame frame = null;
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            FileDialog f = new FileDialog(frame, message, FileDialog.SAVE);
            String fileType = "*." + ext.getExtensions()[0];
            f.setDirectory(dir);
            f.setVisible(true);
            /**
             * Check Result
             */
            String fstring = f.getFile();
            dir = f.getDirectory();
            if (fstring != null) {
                if (extensionMatches(file, ext)) {
                    file = new File(dir + fstring);
                } else {
                    /* we need to manualy add the extension */
                    file = new File(dir + fstring + "." + ext.getExtensions()[0]);
                }
                prefs.put(pathPreference, dir);
            }

        } else {
            JFileChooser f = new JFileChooser();

            f.setMultiSelectionEnabled(false);
            f.setDialogTitle(message);
            f.setFileFilter(ext);
            if (!dir.equals("")) {
                System.out.println("Setting dir to LOP: " + dir);
                f.setCurrentDirectory(new File(dir));
            }
            int result = f.showSaveDialog(null);

            /**
             * Check Result
             */
            if (result == JFileChooser.APPROVE_OPTION) {
                prefs.put(pathPreference, f.getCurrentDirectory().getAbsolutePath());
                file = f.getSelectedFile();
                if (!extensionMatches(file, ext)) {
                    file = new File(file.getPath() + "." + ext.getExtensions()[0]);
                }
            }
        }

        return file;
    }

    private static boolean extensionMatches(File f, FileNameExtensionFilter ext) {
        if (f != null) {
            if (f.getName().contains(ext.getExtensions()[0])) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
