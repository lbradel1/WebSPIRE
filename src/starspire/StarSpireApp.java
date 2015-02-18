package starspire;

import starspire.controllers.EntityExtractorWrapper;
import starspire.controllers.StarSpireController;
import java.util.logging.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import org.json.JSONException;
import java.util.prefs.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.*;

/**
 * This is the application main class and luncher. It sets up the main frame
 * and lunches the rest.
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class StarSpireApp extends JFrame implements ComponentListener {

    /**
     * Set up logging
     */
    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());

    /*
     * Constants
     */
    private static final String DEFAULT_TITLE = "StarSPIRE";
    private static final boolean IMPORT_ON_NEW = true;

    /*
     * Main variables
     */
    private static boolean MacOS = false;
    private static ArrayList<StarSpireController> openProjects = new ArrayList<StarSpireController>();
    private static Preferences prefs = Preferences.userNodeForPackage(StarSpireApp.class);
    private StarSpireController currentProject;

    /*
     * Menus
     */
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu viewMenu;
    private JMenu documentsMenu;
    private JMenu entitiesMenu;
    private JMenu functionsMenu;
    private JMenu layoutMenu;
    private JMenu windowMenu;
    private JMenu multiscaleMenu;
    /*
     * Menu Items
     */
    private JMenuItem nodeView;
    private JMenuItem docView;
    private JMenuItem entView;
    private JMenuItem databaseView;
    /*
     * Application Actions
     */
    private Action projectNew;
    private Action projectMSSI;
    private Action projectMSSIoff;
    private Action projectOpen;
    private Action projectSave;
    private Action projectSaveAs;
    private Action projectClose;
    private Action appQuit;

    /**
     * Returns os boolean when the app is running.
     * @return true if we're on mac os.
     */
    public static boolean isMacOS() {
        return MacOS;
    }

    /**
     * Returns a copy of the preference object.
     * @return prefs...
     */
    public static Preferences getPrefs() {
        return prefs;
    }

    /**
     * Default constructor
     */
    public StarSpireApp() {
        /*
         * Load Preferences
         */
        addComponentListener(this);

        /*
         * Set debug options
         */
        setupLogging();

        /*
         * Detect Mac OS X to do mac things later
         */
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
            MacOS = true;
        }

        // Get the look and feel for this OS
        initLookAndFeel();

        // Menubar
        initMenuBar();

        // set Handling of window closed
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });

        //Set layout
        getContentPane().setLayout(new BorderLayout());

        //pack
        pack();

        //look up location and size from preferences
        initPositionAndSizePref();

        /**
         * Now preload the entity parser
         */
        SwingWorker worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                System.out.println(Thread.currentThread().getName() + " setting up entity parser...");
                EntityExtractorWrapper.setup();
                System.out.println("done setting up entity parser!");

                return null;
            }
        };
        worker.execute();
    }

    /**
     * This sets up logging options for the app
     */
    private void setupLogging() {
        /**
         * Use this to limit logging level.
         */
        logger.setLevel(Level.INFO);
        try {
            // Create a file handler that write log record to a file
            FileHandler handler = new FileHandler("forceSPIRE.log");
            // Add to the desired logger
            logger.addHandler(handler);
        } catch (IOException e) {
            System.err.println("Could not add FileHandler to logger");
        }
    }

    /**
     * This helper sets up the accelerators for menu items.
     * It gets the key from the abstractAction value KeyEventCode,
     * which has to be set. If it's not it wont add an accelerator.
     * ie action.putValue("KeyEventCode", KeyEvent.VK_A) would set it to a.
     *
     * This by default sets it to use the Command key modifier if on MacOS and
     * CTRL on other platforms.
     *
     * @param item
     */
    private void setAccelerator(JMenuItem item, int keyEventCode) {
        if (MacOS) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyEventCode, Event.META_MASK));
        } else {
            item.setAccelerator(KeyStroke.getKeyStroke(keyEventCode, Event.CTRL_MASK));
        }
    }

    /**
     * This helper initiates the look and feel and sets title.
     * It tries to get the native look and feel for most OSes.
     * Note: on MacOS it also tries to use the Mac menu bar and sets the
     * title to that there too.
     */
    private void initLookAndFeel() {
        setTitle(DEFAULT_TITLE);
        if (MacOS) {
            try {
                Class<?> appc = Class.forName("com.apple.eawt.Application");
                Object app = appc.newInstance();
                Class paramTypes[] = new Class[]{java.awt.Image.class};
                Image dockIcon = new ImageIcon(getClass().getResource("/starspire/images/Death-Star-icon-48.png")).getImage();
                Object argumentList[] = {dockIcon};
                Method setDockIconImage = appc.getMethod("setDockIconImage", paramTypes);
                setDockIconImage.invoke(app, argumentList);


            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        } else {
            this.setIconImage(new ImageIcon(getClass().getResource("/starspire/images/Death-Star-icon-48.png")).getImage());
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (UnsupportedLookAndFeelException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This helper initializes the main menu bar. It will create and add all the menus.
     * It will also add all the actions that can be added at the time. The rest will have to be set later.
     */
    private void initMenuBar() {
        menuBar = new JMenuBar();

        initFileMenu();
        menuBar.add(fileMenu);

        initViewMenu();
        menuBar.add(viewMenu);

        initDocMenu();
        menuBar.add(documentsMenu);

        initEntityMenu();
        menuBar.add(entitiesMenu);

        initFunctionsMenu();
        menuBar.add(functionsMenu);

        initLayoutMenu();
        menuBar.add(layoutMenu);
        //todo way to load layout actions based on current document?

        initWindowMenu();
        menuBar.add(windowMenu);
        
        initWindowMenu();
        menuBar.add(multiscaleMenu);

        menuBar.setBorderPainted(true);



        //set menubar
        setJMenuBar(menuBar);
    }

    /**
     * Initialize the file menu,
     * add all the menu actions that are mostly project independent.
     */
    private void initFileMenu() {
        fileMenu = new JMenu("File");

        /*
         * New
         */
        projectNew = new AbstractAction("New") {

            public void actionPerformed(ActionEvent e) {
                newProject();
                //todo after creating document ask if we wanna import data
                //then ask to generate entities
            }
        };
        JMenuItem newItem = new JMenuItem(projectNew);
        setAccelerator(newItem, KeyEvent.VK_N);
        fileMenu.add(newItem);
        
        projectMSSI = new AbstractAction("Turn MSSI On") {
            public void actionPerformed(ActionEvent e) {
                currentProject.setMSSI(true);
            }
        };
        projectMSSI.setEnabled(false);
        JMenuItem mssiItem = new JMenuItem(projectMSSI);
        setAccelerator(mssiItem, KeyEvent.VK_Y);
        fileMenu.add(mssiItem);
        
        projectMSSIoff = new AbstractAction("Turn MSSI Off") {
            public void actionPerformed(ActionEvent e) {
                currentProject.setMSSI(false);
            }
        };
        projectMSSIoff.setEnabled(false);
        JMenuItem mssiOffItem = new JMenuItem(projectMSSIoff);
        setAccelerator(mssiOffItem, KeyEvent.VK_U);
        fileMenu.add(mssiOffItem);

        //_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
        /*
         * new from folder
         * todo use doc import for this... canncel this?
         */
//        JMenuItem newFromFolder = new JMenuItem("New from Folder");
//        newFromFolder.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                newFromFolder();
//            }
//        });
//        fileMenu.add(newFromFolder);
        //_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _

        /*
         * Open
         */
        projectOpen = new AbstractAction("Open") {

            public void actionPerformed(ActionEvent e) {
                open();
            }
        };
        JMenuItem openItem = new JMenuItem(projectOpen);
        setAccelerator(openItem, KeyEvent.VK_O);
        fileMenu.add(openItem);

        /*
         * Save
         */
        projectSave = new AbstractAction("Save") {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        };
        projectSave.setEnabled(false);
        JMenuItem saveItem = new JMenuItem(projectSave);
        setAccelerator(saveItem, KeyEvent.VK_S);
        fileMenu.add(saveItem);

        /*
         * Save As
         */
        projectSaveAs = new AbstractAction("Save As...") {

            public void actionPerformed(ActionEvent e) {
                saveAs();
            }
        };
        projectSaveAs.setEnabled(false);
        JMenuItem saveAsItem = new JMenuItem(projectSaveAs);
        fileMenu.add(saveAsItem);

        /*
         * Close
         */
        projectClose = new AbstractAction("Close") {

            public void actionPerformed(ActionEvent e) {
                closeProject();
            }
        };
        projectClose.setEnabled(false);
        JMenuItem closeItem = new JMenuItem(projectClose);
        setAccelerator(closeItem, KeyEvent.VK_W);
        fileMenu.add(closeItem);

        /*
         * Quit
         */
        appQuit = new AbstractAction("Quit " + DEFAULT_TITLE) {

            public void actionPerformed(ActionEvent e) {
                exitApp();
            }
        };
        JMenuItem quitItem = new JMenuItem(appQuit);
        setAccelerator(quitItem, KeyEvent.VK_Q);
        fileMenu.add(quitItem);
    }

    /**
     * This helper does some checks and then exits.
     * If a project is open and has unsaved changes, ask
     * to confirm losing the application.
     */
    private void exitApp() {
        if (currentProject != null && currentProject.needSave()) {
            int reply = JOptionPane.showConfirmDialog(null, "Are you sure you want to close the application? Unsaved changes will be lost",
                    "Close and lose unsaved changes?", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                setVisible(false);
                dispose();
                System.exit(0);
            }
        } else {
            setVisible(false);
            dispose();
            System.exit(0);
        }
    }

    /**
     * This initializes the view menu
     */
    private void initViewMenu() {
        viewMenu = new JMenu("Views");
        
        multiscaleMenu = new JMenu("Multi-Scale Actions");
        databaseView = new JMenuItem("Database-level query");
        multiscaleMenu.add(databaseView);
        //TODO: add action to execute query for new docs
        

        nodeView = new JMenuItem("NodeView");
        nodeView.setEnabled(false);
        viewMenu.add(nodeView);

        docView = new JMenuItem();
        docView.setAction(new AbstractAction("Document Viewer") {

            public void actionPerformed(ActionEvent ae) {
                currentProject.openDocumentViewer();
            }
        });
        docView.setEnabled(false);
        viewMenu.add(docView);

        entView = new JMenuItem();
        entView.setAction(new AbstractAction("Entity Viewer") {

            public void actionPerformed(ActionEvent ae) {
                currentProject.openEntityViewer();
            }
        });
        entView.setEnabled(false);
        viewMenu.add(entView);


    }

    /**
     * Initialize document menu
     */
    private void initDocMenu() {
        documentsMenu = new JMenu("Documents");
    }

    /**
     * Initialize Entity menu
     */
    private void initEntityMenu() {
        entitiesMenu = new JMenu("Entities");
        //to do add items here?

        /*
         * Import Entities from File
         * This uses the file from CPA's exported entity thing right now, format:
         * "entity string", TYPE
         * "entity2 string", TYPE
         * entity_string, TYPE
         */
//        JMenuItem importFromFile = new JMenuItem("Import from File...");
//        importFromFile.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                importFromFile();
//            }
//        });
//        entitiesMenu.add(importFromFile);
    }

    /**
     * This function initializes and menu and if possible tries to add it's
     * components (might have to wait until a document is loaded.
     */
    private void initFunctionsMenu() {
        functionsMenu = new JMenu("Functions");
    }

    /**
     * This initiates the layout menu. There's nothing done here really. Its only when a document is opened
     * that it will load the layout's actions.
     */
    private void initLayoutMenu() {
        layoutMenu = new JMenu("Layout");
    }

    /**
     * This initializes the window menu.
     * It will contain a link to all the open frames.
     */
    private void initWindowMenu() {
        windowMenu = new JMenu("Window");
        //to do generate actions to switch windows to front
        //especially main and search...
        //also look into http://lists.apple.com/archives/java-dev/2003/oct/msg01310.html to save time
    }

    /**
     * This help loads the windows size and location from the preferences before
     * the window is shown.
     */
    private void initPositionAndSizePref() {
        //debug print
//        System.out.println("Opening at:" + prefs.getInt("window_location_x", 0)
//                + ", " + prefs.getInt("window_location_y", 0));
//        System.out.println("Opening with:" + prefs.getInt("window_size_width", DEFAULT_WIDTH)
//                + ", " + prefs.getInt("window_size_height", DEFAULT_HEIGHT));
//        setLocation(prefs.getInt("window_location_x", 0), prefs.getInt("window_location_y", 0));
//        //Open last size or default
        //setSize(prefs.getInt("window_size_width", DEFAULT_WIDTH)
        //	, prefs.getInt("window_size_height", DEFAULT_HEIGHT));

        /**
         * Handle multi monitor setups???
         */
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Rectangle virtualBounds = new Rectangle();

        int width = gs[0].getDisplayMode().getWidth();
        int height = gs[0].getDisplayMode().getHeight();

        if (gs.length > 1) {
            Rectangle rect = ge.getMaximumWindowBounds();

//            for (int j = 0; j < gs.length; j++) {
//                GraphicsDevice gd = gs[j];
//                GraphicsConfiguration[] gc = gd.getConfigurations();
//                for (int i = 0; i < gc.length; i++) {
//                    virtualBounds = virtualBounds.union(gc[i].getBounds());
//                }
//            }
            width = rect.width;
            height = rect.height;
        }

        setLocation(0, 0);
        System.out.println("starting StarSPIRE with resolution: " + width + ", " + height);
        setSize(width, height);
    }

    /**
     * This function reloads all the menu.
     * This is used when the current document changes and the menus must be generated
     * based on the new current document's actions.
     */
    private void reloadAllMenus() {
        if (currentProject != null) {
            reloadMenu(functionsMenu, currentProject.getFunctionActions());
            reloadMenu(entitiesMenu, currentProject.getEntitiesActions());
            reloadMenu(documentsMenu, currentProject.getDocumentsActions());
            reloadMenu(layoutMenu, currentProject.getLayoutActions());
        } else {
            functionsMenu.removeAll();
            entitiesMenu.removeAll();
            documentsMenu.removeAll();
            layoutMenu.removeAll();
        }
    }

    /**
     * This code reloads a menu by creating a new menu items based
     * on the given abstract actions.
     * @param menu Menu to reload
     * @param arr array of actions to use as items in the menu
     */
    private void reloadMenu(JMenuItem menu, ArrayList<AbstractAction> arr) {
        menu.removeAll(); //flush the old ones
        if (currentProject != null) {
            for (AbstractAction a : arr) {
                JMenuItem item = new JMenuItem(a);
                menu.add(item);
                //System.out.println("adding menu item: " + item.toString());
                Object key = a.getValue("KeyEventCode");
                if (key != null && (Integer) key != KeyEvent.VK_UNDEFINED) {
                    setAccelerator(item, (Integer) key);
                }
            }
        }
    }

    /**
     * Helper to close currently open document.
     *
     * Checks for unsaved changes first and if needed saves project.
     */
    private void closeProject() {
        if (currentProject.needSave()) {
            int reply = JOptionPane.showConfirmDialog(null, "Do you want to save this document before closing it?",
                    "Save before closing?", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                currentProject.save();
            }
        }
        currentProject.close();

        getContentPane().remove(currentProject.getGraphView());
        //getContentPane().remove(currentDoc.getActionToolBar());
        openProjects.remove(currentProject);
        nodeView.setAction(null);
        nodeView.setEnabled(false);
        docView.setEnabled(false);
        entView.setEnabled(false);
        currentProject = null;
        reloadAllMenus();
        validate();
        repaint();
        //disable project related actions
        projectClose.setEnabled(false);
        projectSave.setEnabled(false);
        projectSaveAs.setEnabled(false);
        //empty layout menu
        layoutMenu.removeAll();
    }

    /**
     * New file Helper
     * Starts a new document
     */
    private void newProject() {
        if (currentProject != null) {
            closeProject();
        }
        /*
         * Start new Document
         */
        StarSpireController theNewProject = new StarSpireController(getSize().width, getSize().height);
        setCurrentProject(theNewProject);
        /*
         * Start the layout AFTER all the loading is done.
         */
        currentProject.startLayout();

        if (IMPORT_ON_NEW) {
            //TODO ask to import data here if so can pop up file dialog,
            //and take in file...
        }

    }

    /**
     * Open Helper.
     * Shows file open dialog and if user picks valid file try to open it.
     */
    private void open() {
        if (currentProject != null) {
            closeProject();
        }

        File file = StarSpireUtility.openFile("Open Drawing", "last_open_path", StarSpireUtility.EXT_FILTER_JSON);
        if (file != null) {
            System.out.println("Open target:" + file.getAbsolutePath());
            try {
                StarSpireController openProject = new StarSpireController(file);
                setCurrentProject(openProject);
                openProjects.add(currentProject);
                /*
                 * Start the layout AFTER all the loading is done.
                 */
                currentProject.startLayout();
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error File Not Found",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.WARNING, "File Not Found", ex);
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error JSON failed to open file",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.WARNING, "JSON Format Error", ex);
            }
        }
    }

    /**
     * This helper sets the current document it's used by new document and by open document.
     * @param project ForceSpire project to set as current.
     */
    private void setCurrentProject(StarSpireController project) {
        currentProject = project;
        openProjects.add(currentProject);

        reloadAllMenus();

        //enable views
        nodeView.setAction(currentProject.getNodeViewAction());
        nodeView.setEnabled(true);
        docView.setEnabled(true);
        entView.setEnabled(true);

        //enable project related actions
        projectClose.setEnabled(true);
        projectSave.setEnabled(true);
        projectSaveAs.setEnabled(true);
        projectMSSI.setEnabled(true);
        projectMSSIoff.setEnabled(true);

        //get preferred size and try to accomodate it
        Insets i = getInsets();
        Dimension d = currentProject.getPreferredSize();
        System.out.println("Insets: " + i + ", Preffered Dimensions: " + d);
        setSize(d.width + i.left + i.right, d.height + i.bottom + i.top);

        //Add the graph after the view has been resized
        getContentPane().add(currentProject.getGraphView(), BorderLayout.CENTER);

        
        validate();
    }

    /**
     * Save Helper
     * If document's file set, save now.
     * Otherwise perform saveAs to get a file.
     */
    private void save() {
        if (currentProject.canSave()) {
            currentProject.save();
        } else {
            saveAs();
        }
    }

    /**
     * Save file helper. Checks the extension entered, or makes it .json if none given
     * @param File f the File to check
     * @return File f the File with the correct .json extension
     */
    private File checkExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        if (ext == null) {
            f = new File(f.getPath() + ".json");
        }
        return f;
    }

    /**
     * Save as helper
     * Shows file chooser and let user select file
     * If file exists ask if overwrite wanted.
     * Save current document.
     */
    private void saveAs() {
        File file = StarSpireUtility.saveFile("Save Project As", "last_save_path", StarSpireUtility.EXT_FILTER_JSON);
        if (file != null) {
            file = checkExtension(file);
            /*
             * Check if file exist if so ask for overwrite
             */
            if (file.exists()) {
                int yes = JOptionPane.showConfirmDialog(null, "Are you sure you want to overwrite this file?",
                        "Overwrite File?", JOptionPane.YES_NO_OPTION);
                if (yes != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            /* save if everything is in order */
            currentProject.saveAs(file);
        }

    }

    /**
     * Main part starts application no specific behavior, may be load last open file
     * @param args currently unused
     */
    public static void main(String args[]) {
        //Create the object
        StarSpireApp forceSpireApp = new StarSpireApp();
        //get the show going
        forceSpireApp.setVisible(true);
    }

    /**
     * Used to set preferences. Window Size preference will be updated.
     * System will try to open application of save size on next run.
     * @param e Event Details
     */
    public void componentResized(ComponentEvent e) {
        Dimension d = getSize();
        prefs.putInt("window_size_width", d.width);
        prefs.putInt("window_size_height", d.height);
    }

    /**
     * Used to set preferences. Window Location preference will be updated.
     * This way we can open in the same location next time.
     * @param e Event Details
     */
    public void componentMoved(ComponentEvent e) {
        Point p = getLocation();
        prefs.putInt("window_location_x", p.x);
        prefs.putInt("window_location_y", p.y);
    }

    /**
     * Currently unused. Does nothing.
     * @param e Unused
     */
    public void componentShown(ComponentEvent e) {
    }

    /**
     * Currently unused. Does nothing.
     * @param e Unused
     */
    public void componentHidden(ComponentEvent e) {
    }
}
