package starspire;

/**
 * This class is the luncher. It starts the application form here,
 * outside the swing thread.
 * 
 * This allows doing MacOSX things that can't be done in the swing thread like
 * setting the application name and other such things...
 * @author Patrick Fiaux, Alex Endert
 */
public class StarSpireLauncher {

    private static final String DEFAULT_TITLE = "StarSPIRE";

    /**
     * Starts the application.
     * @param args unused right now
     */
    public static void main(String args[]) {

        /*
         * If mac do special things...
         */
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            //Set Menubar title
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", DEFAULT_TITLE);
            //Use the screen menu bar
            System.setProperty("apple.laf.useScreenMenuBar", "true");      
        }

        /*
         * start the main app
         */
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                StarSpireApp forceSpireApp = new StarSpireApp();
                //get the show going
                forceSpireApp.setVisible(true);
            }
        });

    }
}
