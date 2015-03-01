package starspire.controllers;

import starspire.views.DocumentViewer;
import starspire.views.EntityViewer;
import starspire.views.NodeView;
import starspire.views.GraphView;
import starspire.models.Entity;
import starspire.models.Edge;
import starspire.models.GraphListener;
import starspire.models.Node;
import starspire.models.DocumentLink;
import starspire.models.Document;
import starspire.models.DataListener;
import starspire.models.WeightedElasticLayout;
import starspire.models.DataModel;
import starspire.models.GraphModel;
import starspire.models.SearchNode;
import starspire.models.Search;
import starspire.models.GraphLayout;
import starspire.models.DocumentNode;
import starspire.StarSpireApp;
import starspire.StarSpireUtility;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import starspire.webscraper.BingHandler;

/**
 * The ForceSpireController basically represents a project/document when it's open
 * it holds both the data and graph controllers as well as their models.
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class StarSpireController implements ComponentListener, DataListener {
    /*
     * Constants
     */

    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());
    private final static int JSON_FORMAT_INDENT = 2;
    private final static int DEFAULT_WIDTH = 900;
    private final static int DEFAULT_HEIGHT = 650;
    private final static int DEFAULT_RTF_STRING_CAPACITY = 128;
    private final static int SELECTION_TOLERENCE = 5;
    private final static int OVERLAP_TOLERENCE = -15;
    private final static double PRUNING_THRESHOLD = .02;
    private final static double SEARCH_THRESHOLD = .001;
    private final static double REG_THRESHOLD = .002;
    private final static int MAX_TO_ADD = 8; //documents to retrieve max to add to workspace
    /**
     * This is used to update the weight of an edge.
     * i.e. a note or something is added and an edge should be created but already
     * exists, it's up weighted by adding this. Why adding? good question..
     */
    private final static double EDGE_UP_WEIGHT_ADDER = 0.1;

    private static enum EntityUpdate {

        HIGHLIGHT, LINK, SEARCH, ANNOTATE, SYSTEM
    };
    /*
     * Models
     */
    private DataModel data;
    private GraphModel graph;
    private GraphLayout graphLayout;

    /*
     * Views
     */
    private GraphView mainPanel;
    private JToolBar tools;
    private DocumentViewer docViewer;
    private EntityViewer entViewer;
    private SoftDataLogger softdata = null;
    private SearchNode currentSearchNode;
    /*
     * Actions
     */
    private NodeViewAction nodeAction;
    private ArrayList<AbstractAction> functions;
    private ArrayList<AbstractAction> documents;
    private ArrayList<AbstractAction> entities;

    /*
     * Local Variables
     */
    private java.util.Random r = new java.util.Random(3); //seeded for testing
    private int needSave, width, height;
    private boolean linking;
    private File saveLocation;
    private boolean autoSave = false;    //when true, program will automatically save
    private boolean mssiOn = true;

    /**
     * This private class is used to do the highlight background work...
     * if we don't do it this way it can freeze the gui...
     */
    private class HighlightWorker extends SwingWorker<Void, Integer> {

        private int start, end;
        private Document doc;
        //add enum for type

        public HighlightWorker(Document d, int s, int e) {
            start = s;
            end = e;
            doc = d;
        }

        @Override
        protected Void doInBackground() {
            System.out.println(Thread.currentThread().getName()
                    + " generating entities for highlight.");
            //get the entities of the highlighted text
            String textHighlighted = doc.getContent(start, end);
            Parser localParser = new Parser();
            ArrayList<String> terms = localParser.parseString(textHighlighted);

            ArrayList<String> extractedEntites = EntityExtractorWrapper.extractEntities(textHighlighted);

            //these are the extracted entities
            for (String s : extractedEntites) {
                if (hasEntity(s)) {
                    //this entity exists, upweight it
                    //done in the loop below
                } else {
                    //this entity does not exist, create it
                    addEntity(s, true);
                    //upweighting done in the loop below
                    //increaseEntityStrength(data.getEntity(s), EntityUpdate.HIGHLIGHT);
                }
            }

            //these are the terms that are highlighted, upweight them
            ArrayList<Entity> entsToUpweight = new ArrayList<Entity>();
            for (String s : terms) {
                if (hasEntity(s)) {
                    //this is an entity, it has been highlighted, upweight it
                    //increaseEntityStrength(data.getEntity(s), DataListener.HIGHLIGHT);
                    entsToUpweight.add(data.getEntity(s));
                }
            }
            if(mssiOn) {
                retrieveDocuments(entsToUpweight, DataListener.HIGHLIGHT);
            }

            //add the highlight to the Document, and fire the event
            data.addDocumentHighlight(doc, start, end);
            graphLayout.start();
            return null;
        }
    }

    /**
     * NodeViewAction
     * This action will serve to open/close the node view.
     */
    public class NodeViewAction extends AbstractAction {

        private NodeView view = null;
        private StarSpireController controller;

        /**
         * Default constructor, gives label and image
         * @param name Name of this item
         * @param fsc Controller so the action can do stuff
         */
        public NodeViewAction(String name, StarSpireController fsc) {
            super(name);
            controller = fsc;
            view = null;
        }

        /**
         * Open the view if it's not open already. If it is... close it?
         * @param e info about where the event comes from (unused right now)
         */
        public void actionPerformed(ActionEvent e) {
            if (view == null) {
                view = new NodeView(controller);
                //view.setLocationRelativeTo(mainPanel);
                view.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosing(WindowEvent e) {
                        closeNodeView();
                    }
                });
            }
        }

        /**
         * This closes the node view.
         */
        public void closeNodeView() {
            if (view != null) {
                view.setVisible(false);
                graph.removeListener(view);
                view.dispose();
                view = null;
            }
        }
    }

    /**
     * This action allows the application to bind the function to start a user
     * data (soft data) log.
     */
    public class LogUserDataAction extends AbstractAction {

        /**
         * Constructor
         * @param name name of action
         * @param ico icon icon for the action.
         * @param KeyEventCode key to use as keyboard shortcut.
         */
        public LogUserDataAction(String name, Icon ico, int KeyEventCode) {
            super(name, ico);
            putValue("KeyEventCode", KeyEventCode);
        }

        /**
         * Action performed, toggle linking state.
         * @param e event details
         */
        public void actionPerformed(ActionEvent e) {
            File file = StarSpireUtility.saveFile("Select soft data log file", "last_log_save_path", StarSpireUtility.EXT_FILTER_CSV);
            if (file != null) {
                System.out.println("starting soft data log file...");
                startSoftDataLog(file);
            }
        }
    }

    /**
     * This is a function that starts the process to link 2 nodes.
     */
    public class LinkNodesAction extends AbstractAction {

        /**
         * Constructor
         * @param name name of action
         * @param ico icon icon for the action.
         * @param KeyEventCode key to use as keyboard shortcut.
         */
        public LinkNodesAction(String name, Icon ico, int KeyEventCode) {
            super(name, ico);
            putValue("KeyEventCode", KeyEventCode);
        }

        /**
         * Action performed, toggle linking state.
         * @param e event details
         */
        public void actionPerformed(ActionEvent e) {
            toggleLinking();
            System.out.println("linking is " + linking);

        }
    }

    /**
     * This is a function that handles the action for
     * adding documents to the workspace
     */
    public class ImportDocumentsAction extends AbstractAction {

        /**
         * Constructor
         */
        public ImportDocumentsAction(String name, Icon ico, int KeyEventCode) {
            super(name);
            putValue("KeyEventCode", KeyEventCode);
        }

        /**
         * Action performed
         * Import the documents, do not parse/add entities
         */
        public void actionPerformed(ActionEvent e) {
            System.out.println("adding documents to workspace...");

            /*
             * Take all .txt documents, add them to workspace
             */
            File[] files = StarSpireUtility.openFiles("Select .txt files to parse", "last_import_path", StarSpireUtility.EXT_FILTER_TXT);
            if (files != null) {
                System.out.print("Starting Text Import...");
                addDocumentsFromFile(files);
            }

        }
    }

    /**
     * This is an action for the Application to bind to the jigsaw import to a button
     */
    public class ImportJigsawAction extends AbstractAction {

        /**
         * Constructor
         */
        public ImportJigsawAction(String name, Icon ico, int KeyEventCode) {
            super(name);
            putValue("KeyEventCode", KeyEventCode);
        }

        /**
         * Action performed
         * Import the documents, do not parse/add entities
         */
        public void actionPerformed(ActionEvent e) {
            System.out.println("adding Jigsaw file to workspace...");
            /*
             * Select the .jig file to parse it
             */
            File file = StarSpireUtility.openFile("Select .jig file to parse", "last_jig_import_path", StarSpireUtility.EXT_FILTER_JIG);
            if (file != null) {
                System.out.print("Starting Jigsaw Import...");
                addDocumentsFromJigsaw(file);
                //calculate TF-IDF values
                //calculateTFIDF();
                //printTFIDF();
                //hideAllDocuments();
            }
        }
        
        
    }
    
    private void calculateTFIDF() {
        Iterator<Document> TFdocs = data.hiddenDocsIterator();
        int allEntityTerms = 0;
        int numberOfDocs = 0;
        while(TFdocs.hasNext()) {
            Document d = TFdocs.next();
            String content = d.getContent().toLowerCase();
            Iterator<Entity> docEnts = d.getEntityIterator();
            int docEntCount = 0;
            while(docEnts.hasNext()) {
                int entCount = 0;
                Entity e = docEnts.next();
                String entName = e.getName().toLowerCase();
                Pattern p = Pattern.compile(entName.toLowerCase());
                 Matcher m = p.matcher(content);
                 while(m.find()) {
                     docEntCount++;
                     entCount++;
                     allEntityTerms++;
                 }
                 e.setEntityCount(entCount);
                 //System.out.println(entCount);
            }
            Iterator<Entity> ents = d.getEntityIterator();
            while(ents.hasNext()) {
                Entity e = ents.next();
                double entCount = (double) e.getEntityCount();
                double TFval = entCount / (double) docEntCount;
                //System.out.println(TFval);
                e.addTFdoc(TFval, d);
            }
            numberOfDocs++;
            
        }
        
        
        Iterator<Entity> ents = data.entityIterator();
        int totalTerms = data.getEntityCount();
        
        while(ents.hasNext()) {
            Entity ent = ents.next();
            String entName = ent.getName().toLowerCase();
            int count = 0;
            Iterator<Document> docs = data.hiddenDocsIterator();
            while(docs.hasNext()) {
                Document doc = docs.next();
                String content = doc.getContent().toLowerCase();
                //System.out.println("Doc content: " + content);
                
                //int instances = content.split(entName).length;
                //count += instances;
                //allEntityTerms += instances;
                
                Pattern p = Pattern.compile(entName.toLowerCase());
                 Matcher m = p.matcher(content);
                 while(m.find()) {
                 count++;
                 allEntityTerms++;
                 
                 }                 
            }
            double IDF = (double) Math.log(numberOfDocs / (double) count);
            //System.out.println(IDF);
            ent.setIDF(IDF);
            //double TF = ent.getTF();
            //ent.setTFIDF(TF * IDF);

        }
        
    }
    
    private void printTFIDF() {
        try {
            String fn = "TFIDF_values.txt";
            //Print initial binary values for SVD purposes
            PrintWriter pw = new PrintWriter(fn);
            Iterator<Entity> ents = data.entityIterator();
            //pw.println("Entity Name" + "\t" + "TFIDF" + "\t" + "count" + "\t" + "IDF");
            pw.print("Entity Name" + "\t");
            Iterator<Document> docsNames = data.hiddenDocsIterator();
            while(docsNames.hasNext()) {
                Document d = docsNames.next();
                pw.print(d.getName() + "\t");
            }
            pw.println();
            
            while(ents.hasNext()) {
                Entity ent = ents.next();
                pw.print(ent.getName() + "\t");
                Iterator<Document> docs = data.hiddenDocsIterator();
                while(docs.hasNext()) {
                    Document d = docs.next();
                    if(d.hasEntity(ent)) {
                        //System.out.println(ent.getTFIDFdoc(d));
                        pw.print(ent.getTFIDFdoc(d) + "\t");
                    }
                    else {
                        pw.print("\t");
                    }
                }
                pw.println();
                //pw.println(ent.getName() + "\t" + ent.getTFIDF() + "\t" + ent.getEntityCount() + "\t" + ent.getIDF());
                
                //System.out.println(ent.getName() + "\t" + ent.getTFIDFdoc(null));

            }        
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StarSpireController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        

    }
    
    
    private void hideAllDocuments() {
        ArrayList<Document> docsToHide = new ArrayList<Document>();
        Iterator<Document> docs = data.documentIterator();
        while(docs.hasNext()) {
            Document d = docs.next();
            docsToHide.add(d);
        }
        
        for(int i = 0; i < docsToHide.size(); i++) {
            data.removeDocument(docsToHide.get(i));
        }
        
    }

    /**
     * This is a function that handles the action for parsing the
     * current documents for entities
     */
    public class GenerateEntitiesAction extends AbstractAction {

        /**
         * Constructor
         */
        public GenerateEntitiesAction(String name, Icon ico, int KeyEventCode) {
            super(name);
            putValue("KeyEventCode", KeyEventCode);
        }

        /**
         * Action performed
         * parse/add entities
         */
        public void actionPerformed(ActionEvent e) {
            System.out.println("generating entities...");
            generateNewEntities();
        }
    }

    /**
     * This is a function that handles the action for adding entities from a file
     * NOTE: this will only work given the format that comes from CPA's tool
     */
    public class ImportEntitiesFromFileAction extends AbstractAction {

        /**
         * Constructor
         */
        public ImportEntitiesFromFileAction(String name, Icon ico, int KeyEventCode) {
            super(name);
            if (KeyEventCode >= 0) {
                putValue("KeyEventCode", KeyEventCode);
            }
        }

        /**
         * Action performed
         * parse/add entities from file
         */
        public void actionPerformed(ActionEvent e) {
            File file = StarSpireUtility.openFile("Select .json file to parse", "last_ent_import_path", StarSpireUtility.EXT_FILTER_TXT);
            if (file != null) {
                System.out.println("adding entities from file...");
                importEntitiesFromFile(file);
            }

        }
    }

    public class SearchAction extends AbstractAction {

        /**
         * Constructor
         */
        public SearchAction(String name, Icon ico, int KeyEventCode) {
            super(name);
            if (KeyEventCode >= 0) {
                putValue("KeyEventCode", KeyEventCode);
            }
        }

        /**
         * Open a new Search by creating an empty search node
         * @param e item clicked
         */
        public void actionPerformed(ActionEvent e) {
            Search s = null;
            //graph.addNode(GraphModel.Position.CENTER, s);
            PointerInfo a = MouseInfo.getPointerInfo();
            Point point = new Point(a.getLocation());
            SwingUtilities.convertPointFromScreen(point, mainPanel);
            graph.addNode(point,s);
        }
    }

    /**
     * Default constructor makes a new document.
     */
    public StarSpireController() {
        /*
         * Run default set up
         */
        try {
            setup(null, null, null, DEFAULT_WIDTH, DEFAULT_HEIGHT, 50);
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * New project with a set starting size.
     * @param width width to start with
     * @param height height to start with.
     */
    public StarSpireController(int width, int height) {
        /*
         * Run default set up
         */
        try {
            setup(null, null, null, width, height, 50);
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load file constructor, this constructor loads it's data from a file.
     * Fails if file doesn't exist of if not in correct format or something
     *
     * @param f File to open as a ForceSpire JSON save
     * @throws FileNotFoundException The given file was not found
     * @throws JSONException JSON exception... try again?
     */
    public StarSpireController(File f) throws FileNotFoundException, JSONException {
        JSONObject forceSpireDoc, forceSpire, jsonData, jsonGraph, jsonLayout;
        int w, h;

        System.out.println("Loading a ForceSpire file...");

        forceSpireDoc = load(f);

        forceSpire = forceSpireDoc.getJSONObject("ForceSpire");
        System.out.print("\tproject data...");
        jsonData = forceSpire.getJSONObject("DataModel");
        System.out.println("done");
        System.out.print("\tgraph data...");
        jsonGraph = forceSpire.getJSONObject("GraphModel");
        System.out.println("done");
        System.out.print("\tlayout manager...");
        jsonLayout = forceSpire.getJSONObject("GraphLayout");
        System.out.println("done");
        try {
            w = forceSpire.getInt("Width");
            h = forceSpire.getInt("Height");
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Error loading JSON size", e);
            System.err.println("Width and Height missing from saved file!"
                    + " Reverting to default values");
            w = DEFAULT_WIDTH;
            h = DEFAULT_HEIGHT;
        }
        System.out.println("\tLoaded Size from file " + w + " by " + h);

        setup(jsonGraph, jsonData, jsonLayout, w, h, 0);
        saveLocation = f; //make sure we keep the file
        System.out.println("...done loading.");
    }

    /**
     * This makes sure all constructors work consistently.
     * @param jsonGraph Optional graph data
     * @param jsonData Optional data data
     * @param jsonLayout Optional graph
     * @param nwidth size of layout
     * @param nheight size of layout
     * @throws JSONException Oops looks like json file wasn't in the correct format
     */
    private void setup(JSONObject jsonGraph, JSONObject jsonData, JSONObject jsonLayout, int nwidth, int nheight, int dcount) throws JSONException {
        /*
         * Set up the stuff
         */
        linking = false;
        height = nheight;
        width = nwidth;
        System.out.println("New Controller Preffered size " + width + " by " + height);

        /*
         * Set up models
         * only load data from json if not null.
         */
        if (jsonData == null && jsonGraph == null) {
            data = new DataModel(dcount);
            graph = new GraphModel();
        } else {
            data = new DataModel(jsonData);
            graph = new GraphModel(jsonGraph, data);
        }

        /*
         * Set up Controllers
         */
        data.addDataListener(this);

        /*
         * UI: Graph view and toolbar
         */
        mainPanel = new GraphView(this);
        mainPanel.addComponentListener(this);

        nodeAction = new NodeViewAction("Show Node View", this);

        functions = new ArrayList<AbstractAction>();
        functions.add(
                new LinkNodesAction("Link Nodes", null, KeyEvent.VK_L));
        functions.add(
                new LogUserDataAction("Log Soft Data", null, KeyEvent.VK_UNDEFINED));
        functions.add(
                new SearchAction("Search", null, KeyEvent.VK_F));

        documents = new ArrayList<AbstractAction>();
        documents.add(
                new ImportDocumentsAction("Import Documents", null, KeyEvent.VK_UNDEFINED));
        documents.add(
                new ImportJigsawAction("Import Jigsaw File", null, KeyEvent.VK_I));

        entities = new ArrayList<AbstractAction>();
        entities.add(
                new GenerateEntitiesAction("Generate Entities", null, KeyEvent.VK_G));
        entities.add(
                new ImportEntitiesFromFileAction("Import From File", null, KeyEvent.VK_UNDEFINED));


        /*
         * Set up Layout
         */
        if (jsonLayout == null) {
            //use default layout
            graphLayout = new WeightedElasticLayout(width, height);
        } else {
            //java reflection load correct layout class based on the saved file
            //if we had multiple layouts...
            graphLayout = new WeightedElasticLayout(width, height, jsonLayout);
        }
        graphLayout.setForceSpireController(this);
        graph.setGraphSize(new Dimension(width, height));

        currentSearchNode = null;

        saveLocation = null;
        needSave = 0;


    }

    /**
     * This method loads a JSON file into a JSON object. It's really a helper
     * for the open file constructor.
     * @param f JSON save file to open.
     * @return JSONObject containing file data for the whole document/project
     * @throws FileNotFoundException The given file was not found
     * @throws JSONException JSON exception... try again?
     */
    private JSONObject load(File f) throws FileNotFoundException, JSONException {
        FileReader reader = new FileReader(f);
        JSONTokener tokener = new JSONTokener(reader);
        JSONObject forceSpireDoc = new JSONObject(tokener);
        needSave = 0; // no need to save we just loaded.

        return forceSpireDoc;
    }

    /**
     * Finds if the file can be saved.
     * If no file was set it can't save.
     * @return True if a file is set. False if no file is set.
     */
    public boolean canSave() {
        return saveLocation != null;
    }

    /**
     * This will tell if the document was changed since the last save.
     * Useful to ask if you wanna save when closing.
     * @return True if there was a change since the last save, false otherwise
     */
    public boolean needSave() {
        return needSave != 0;
    }

    /**
     * If canSave() is true (just to double check)
     * It will save to the file save location.
     */
    public void save() {
        /*
         * Save the file
         */
        if (canSave()) {
            System.out.print("Saving...");
            try {
                //get the json
                JSONObject forceSpireRoot = getJSONObject();

                //save to file
                FileWriter writer = new FileWriter(saveLocation);
                writer.write(forceSpireRoot.toString(JSON_FORMAT_INDENT));
                writer.close();

                needSave = 0;

                //print it out for debug
                //System.out.println(forceSpireRoot.toString(JSON_FORMAT_INDENT));
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error building JSON save",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, "Error building JSON save", ex);
                Logger.getLogger(StarSpireApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error writing file to disk",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, "Error writing file to disk", ex);
            }
        }
        /*
         * flush the log buffer if it's around
         */
        if (softdata != null) {
            softdata.flush();
        }
        //System.out.println("done.");
    }

    /**
     * Save as works when save doesn't, it takes in a file and sets it as the
     * new current file location to use for save.
     * @param f File of where to save the data. Warning will do no check for
     * existence and such here, could overwrite data!
     */
    public void saveAs(File f) {
        if (f != null) {
            saveLocation = f;
            save();
        }
    }

    public void doAutoSave() {
        if (canSave() && autoSave && (needSave > 100)) {
            System.out.print("AutoSaving...");
            try {
                //get the json
                JSONObject forceSpireRoot = getJSONObject();

                //get the time and date
                //Calendar cal = Calendar.getInstance();
                //Date now = calendar.getTime();

                //save to file
                FileWriter writer = new FileWriter(saveLocation);
                writer.write(forceSpireRoot.toString(JSON_FORMAT_INDENT));
                writer.close();

                needSave = 0;

                //print it out for debug
                //System.out.println(forceSpireRoot.toString(JSON_FORMAT_INDENT));
            } catch (JSONException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error building JSON save",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, "Error building JSON save", ex);
                Logger.getLogger(StarSpireApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, ex, "Error writing file to disk",
                        JOptionPane.ERROR_MESSAGE);
                logger.log(Level.SEVERE, "Error writing file to disk", ex);
            }
        }
        /*
         * flush the log buffer if it's around
         */
        if (softdata != null) {
            softdata.flush();
        }
        //System.out.println("done.");
    }

    /**
     * Add a highlight to a document
     * @param document document to highlight
     * @param start start position of the highlight
     * @param end end position
     */
    public void addDocumentHighlight(Document d, int start, int end) {
        /*
        graphLayout.stop();
        SwingWorker worker = new HighlightWorker(d, start, end);
        worker.execute();
         *
         */

//        System.out.println(Thread.currentThread().getName()
//                + " generating entities for highlight.");
        graphLayout.stop();
        //get the entities of the highlighted text
        String textHighlighted = d.getContent(start, end);
        //System.out.println("highlighted text: "+textHighlighted);
        Parser localParser = new Parser();
        ArrayList<String> terms = localParser.parseString(textHighlighted);


        /*
        ArrayList<String> extractedEntites = EntityExtractorWrapper.extractEntities(textHighlighted);
        
        //these are the extracted entities
        for (String s : extractedEntites) {
        if (hasEntity(s)) {
        //this entity exists, upweight it
        //done in the loop below
        } else {
        //this entity does not exist, create it
        addEntity(s, true);
        //upweighting done in the loop below
        //increaseEntityStrength(data.getEntity(s), EntityUpdate.HIGHLIGHT);
        }
        }
         * 
         */
        
        ArrayList<Entity> entsToUpweight = new ArrayList<Entity>();
        for(int i = 0; i < terms.size(); i++) {
        	entsToUpweight.add(data.getEntity(terms.get(i)));
        }

        //these are the terms that are highlighted, upweight them
        for (String s : terms) {
            //System.out.println("highlight checking: "+s);
            if (hasEntity(s)) {
                //this is an entity, it has been highlighted, upweight it
                //increaseEntityStrength(data.getEntity(s), DataListener.HIGHLIGHT);
            } else {
                //this term has been highlighted, but is not an entity, let's make it one!
                addEntity(s, true);
                //increaseEntityStrength(data.getEntity(s), DataListener.HIGHLIGHT);
            }
        }
        if(mssiOn) {
            retrieveDocuments(entsToUpweight, DataListener.HIGHLIGHT);
        }

        //add the highlight to the Document, and fire the event
        data.addDocumentHighlight(d, start, end);
        graphLayout.start();
    }

    /**
     * Add an entity to the document model
     * @param s entity to add
     * @param softdata true if adding entity from user data false otherwise.
     */
    public void addEntity(String s, boolean softdata) {
        data.addEntity(s, softdata);
    }

    /**
     * Takes a string and makes a new document for the document model
     * @param content content for new document
     */
    public void addDocument(String content) {
        data.addDocument(content);
    }

    /**
     * Takes 2 strings to make a new document
     * @param content content for new document
     * @param name title for new document
     */
    public void addDocument(String content, String name) {
        data.addDocument(content, name);
    }

    /**
     * This adds the given listener to the graph's listener list.
     * @param gl a graph listener view
     */
    public void addGraphListener(GraphListener gl) {
        graph.addListener(gl);
    }

    /**
     * Adds a listener to the data model
     * @param listener listener to add
     */
    public void addDataListener(DataListener listener) {
        data.addDataListener(listener);
    }

    /**
     * This method closes the current document.
     * It will stop the thread dead this way it can be closed.
     */
    public void close() {
        nodeAction.closeNodeView();
        graphLayout.stop();
        if (softdata != null) {
            softdata.closeLog();
        }
    }

    /**
     * Returns a document iterator over the documents
     * @return an iterator for the documents in this controller
     */
    public Iterator<Document> documentIterator() {
        return data.documentIterator();
    }

    /**
     * Returns an iterator for the entities.
     * @return Entities iterator.
     */
    public Iterator<Entity> entityIterator() {
        return data.entityIterator();
    }

    /**
     * finds an edge that links 2 nodes (regardless of node order)
     * @param n1 first node
     * @param n2 second node
     * @return null if not found, or edge if it exists
     */
    private Edge lookUpEdge(Node n1, Node n2) {
        Iterator<Edge> edges = graph.edgeIterator();
        while (edges.hasNext()) {
            Edge e = edges.next();
            if (e.links(n1, n2)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Helper that finds a node based on a document.
     * @param d Document who's node we're looking up
     * @return Node that contains this document, null if there is no such node.
     */
    public Node findNode(Document d) {
        Iterator<Node> nodes = graph.nodeIterator();
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n instanceof DocumentNode && ((DocumentNode) n).getDocument() == d) {
                return n;
            }
        }
        return null;
    }

    /**
     * Helper that finds a node based on a search.
     * @param s Search who's node we're looking up
     * @return Node that contains this search, null if there is no such node.
     */
    public Node findNode(Search s) {
        Iterator<Node> nodes = graph.nodeIterator();
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n instanceof SearchNode && ((SearchNode) n).getSearch() == s) {
                return n;
            }
        }
        return null;
    }

    /**
     * This function takes in a point in coordinates and returns a node
     * if a node is within the selection tolerance of this point.
     * @param pt location of the point (click)
     * @return Node at location of point or null if no node.
     */
    public Node findByPoint(Point pt) {
        Iterator<Node> it = graph.nodeIterator();
        /* offset is radius plus tolerence */
        Node n;
        while (it.hasNext()) {
            n = it.next();
            int xoffset = n.getWidth() / 2 + SELECTION_TOLERENCE;
            int yoffset = n.getHeight() / 2 + SELECTION_TOLERENCE;

            //int offset = n.getRadius() + SELECTION_TOLERENCE;
            if ((n.getX() + xoffset >= pt.getX() && n.getX() - xoffset <= pt.getX())
                    && (n.getY() <= pt.getY() + yoffset && n.getY() >= pt.getY() - yoffset)) {
                return n;
            }
        }
        return null;
    }

    public Node findOverlappingNode(Node n) {
        Node overlappingNode = null;

        Iterator<Node> it = graph.nodeIterator();
        /* offset is radius plus tolerence */
        ArrayList<Node> overlappingNodes = new ArrayList<Node>();
        Node n2;
        int xoffset = n.getWidth() / 2 + OVERLAP_TOLERENCE;
        int yoffset = n.getHeight() / 2 + OVERLAP_TOLERENCE;
        while (it.hasNext()) {
            n2 = it.next();
            if (n2 != n && n2.isPinned()) {
                int xoffset2 = n2.getWidth() / 2 + OVERLAP_TOLERENCE;
                int yoffset2 = n2.getHeight() / 2 + OVERLAP_TOLERENCE;

                //int offset = n.getRadius() + OVERLAP_TOLERENCE;
                if ((n2.getX() + xoffset2 >= n.getX() - xoffset && n2.getX() - xoffset2 <= n.getX() + xoffset)
                        && (n2.getY() - yoffset2 <= n.getY() + yoffset && n2.getY() + yoffset2 >= n.getY() - yoffset)) {
                    overlappingNodes.add(n2);
                }
            }
        }

        if (overlappingNodes.size() > 1) {
            //System.out.println("Overlapping with more than one node! Choosing the first one!");
            overlappingNode = overlappingNodes.get(0);
        } else if (overlappingNodes.size() > 0) {
            overlappingNode = overlappingNodes.get(0);
        }

        return overlappingNode;
    }

    /**
     * Returns the preferred display size of this controller.
     * @return Dimensions of the preferred size.
     */
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Handles saving the data model to JSON
     * @return JSON representation of the data model
     * @throws JSONException format exception happened.
     */
    public JSONObject getDataModelJSONObject() throws JSONException {
        JSONObject content = new JSONObject();
        /**
         * Build a JSONArray with a JSONObject representation of all the documents.
         */
        JSONArray nodes = new JSONArray();
        content.put("Documents", nodes);

        Iterator<Document> docIt = data.documentIterator();
        Document n;
        while (docIt.hasNext()) {
            n = docIt.next();
            nodes.put(n.getJSONObject());
        }

        /*
         * Build a JSONArray with the entities...
         */
        JSONArray edges = new JSONArray();
        content.put("Entities", edges);

        Iterator<Entity> entIt = data.entityIterator();
        Entity e;
        while (entIt.hasNext()) {
            e = entIt.next();
            edges.put(e.getJSONObject());
        }

        /*
         * Build a JSONArray with the searches...
         */
        JSONArray searches = new JSONArray();
        content.put("Searches", searches);

        Iterator<Search> sIt = data.searchIterator();
        Search s;
        while (sIt.hasNext()) {
            s = sIt.next();
            searches.put(s.getJSONObject());
        }

        return content;
    }

    public JSONObject getGraphModelJSONObject() throws JSONException {
        JSONObject content = new JSONObject();
        /**
         * Build a JSONArray with a JSONObject representation of all the nodes.
         */
        JSONArray nodes = new JSONArray();
        content.put("Nodes", nodes);

        Iterator<Node> nodeIt = graph.nodeIterator();
        Node n;
        while (nodeIt.hasNext()) {
            n = nodeIt.next();
            nodes.put(n.getJSONObject());
        }

        /*
         * Build a JSONArray with the edges...
         */
        JSONArray edges = new JSONArray();
        content.put("Edges", edges);

        Iterator<Edge> edgeIt = graph.edgeIterator();
        Edge e;
        while (edgeIt.hasNext()) {
            e = edgeIt.next();
            edges.put(e.getJSONObject());
        }

        return content;
    }

    /**
     * This returns an RTF representation of the document
     * Entities it contains are underlined.
     *
     * @param plain a string containing text that needs to be converted to RTF
     * @param entities is a list of all the entities to underline if found.
     * @return RTF converted string with documents highlighted
     */
    public String getRTFEnhancedText(String plain, Iterator<Entity> entities, Iterator<Search> searches) {
        //open rtf tag
        StringBuilder rtf = new StringBuilder(DEFAULT_RTF_STRING_CAPACITY);
        rtf.append("{\\rtf1\\ansi\n");
        rtf.append("{\\fonttbl\\f0\\fswiss\\fcharset0 Helvetica;}\n"); /* define font # */
        rtf.append("{\\colortbl;\\red255\\green0\\blue0;\\red252\\green193\\blue91;\\red250\\green219\\blue135;\\red252\\green235\\blue169;\\red255\\green255\\blue255;}\n");
        rtf.append("\\f0\\fs24"); /* set font # and font size */

        String plainWithEntities = plain;
                    Iterator<Search> ss = searches;
        
        double maxEstrength = data.getMaxEntityStrength();

        while(ss.hasNext()) {
            Search s = ss.next();
            //plainWithEntities = plainWithEntities.replaceAll("(?i)" + Pattern.quote(s.getSearchTerm()), Matcher.quoteReplacement("{\\cf1 " + s.getSearchTerm() + "}"));
            //plainWithEntities = plainWithEntities.replaceAll("(?i)" + Pattern.quote(s.getSearchTerm()), "{\\cf1" + "$1 }");
            plainWithEntities = plainWithEntities.replaceAll("(?i)" + "("+Pattern.quote(s.getSearchTerm())+")", Matcher.quoteReplacement("{\\b ") +  "$1}");
         }
        
        
        //add highlights for entities in text.
        while (entities.hasNext()) {
            Entity e = entities.next();
            double strength = e.getStrength();
            //highlight colors start at 3
            String highN = "6";
            if(strength > maxEstrength * .80) {
                highN = "3";
            }
            else if(strength > maxEstrength * .50) {
                highN = "4";
            }
            else if(strength > maxEstrength * .25) {
                highN = "5";
            }
            
            if(strength < 1.0) {
                highN = "6";
            }
            
            plainWithEntities = plainWithEntities.replaceAll(Pattern.quote(e.getName()), Matcher.quoteReplacement("{\\ul\\highlight" + highN + " " + e.getName() + "}"));
        }
        
       /* while(searches.hasNext()) {
            Search s = searches.next();
            Color sc = Color.getHSBColor(s.getHue() / 360.0f, 100.0f, 100.0f);
            plainWithEntities = plainWithEntities.replaceAll(Pattern.quote(s.getSearchTerm()), Matcher.quoteReplacement("{\\uldb " + s.getSearchTerm() + "}"));

            
        }
            */   
        
        // \strike, \strike0 to turn it off

        //add that to the rtf text.
        rtf.append(plainWithEntities);

        //close rtf tag
        rtf.append(" \n\n}");

        return rtf.toString();
    }

    /**
     * Same as get content RTF but for the notes content.
     * Simply calls the helper with the notes string instead of content.
     * @param doc document we wanna get the note for
     * @return formated note with entities underlined.
     */
    public String getNotesRTF(Document doc) {
        return getRTFEnhancedText(doc.getNotes(), doc.iterator(), data.searchIterator());
    }

    /**
     * Returns the RTF formated text for the document.
     * This simply calls the RTF helper with a string and a list of entities from
     * the document.
     * @param doc document to return the content in RTF format for.
     * @return document's content in RTF format with entities underlined.
     */
    public String getDocumentRTF(Document doc) {
        return getRTFEnhancedText(doc.getContent(), doc.iterator(), data.searchIterator());
    }

    /**
     * This returns a JSON object representation.
     * @return a JSONObject representing this document's graph and data.
     * @throws JSONException something went wrong
     */
    public JSONObject getJSONObject() throws JSONException {
        //this is the document root
        JSONObject forceSpireRoot = new JSONObject();
        JSONObject content = new JSONObject();
        //build root
        forceSpireRoot.put("ForceSpire", content);
        //save controller specific data
        content.put("Width", mainPanel.getSize().width);
        content.put("Height", mainPanel.getSize().height);
        //add data model
        content.put("DataModel", getDataModelJSONObject());
        //add graph model
        content.put("GraphModel", getGraphModelJSONObject());
        //add layoutdata
        content.put("GraphLayout", graphLayout.getJSONOjbect());


        return forceSpireRoot;
    }

    /**
     * This populates the entities to upweight and downweight for the DocumentLink.
     * @param docLink
     * @return
     */
    public DocumentLink populateDocumentLink(ArrayList<DocumentNode> nodesToLink) {

        //System.out.println("Linking");
        DocumentLink fullDocLink = new DocumentLink(nodesToLink.get(0), nodesToLink.get(1));

        //get all the documents being linked
        //TODO: This needs to be scalable for >2 documents to be linked
        ArrayList<Document> docs = new ArrayList<Document>();
        for (DocumentNode n : nodesToLink) {
            if (n != null) {
                docs.add(n.getDocument());
            }
            //this.documentModified(n.getDocument(), LINK);
            data.linkDocument(n.getDocument());
        }

        //get all the entities the need to be linked
        //if they match, we are going to recommend upweighting them?!!
        Iterator<Entity> doc1Ents = docs.get(0).iterator();
        //Iterator<Entity> doc2Ents = docs.get(1).iterator();

        ArrayList<Entity> matchingEntities = new ArrayList<Entity>();

        while (doc1Ents.hasNext()) {
            Entity e1 = doc1Ents.next();
            if (docs.get(1).hasEntity(e1)) {
                matchingEntities.add(e1);
                //System.out.println(e1.toString() + " matches");
            }
        }

        fullDocLink.setEntitiesUpweight(matchingEntities);

        //reset the linking state
        linking = false;
        

        return fullDocLink;
    }

    public DocumentLink executeDocumentLink(DocumentLink docLink) {
        DocumentLink finalDocLink = docLink;

        ArrayList<Entity> entsToUpweight = new ArrayList<Entity>();
        if (docLink.isAccepted()) {
            //accept the link
            for (Entity e : docLink.getEntitiesUpweight()) {
                //upweight these entities
                //System.out.println("Inreasing weight of, new strength: " + e.getName() + ", " + (e.getStrength()+2));
                //e.setStrength(e.getStrength() + 2);
                //increaseEntityStrength(e, DataListener.LINK);
                //entityModified(e, DataListener.LINK);
                entsToUpweight.add(e);
            }
        }
        
        if(mssiOn) {
            retrieveDocuments(entsToUpweight, DataListener.LINK);
        }

        finalDocLink.clear();
        return finalDocLink;
    }
    
    public void increaseDocWeight(Document d) {
        double amount = 1;
        increaseDocWeight(d, amount, true);
    }
    
    public void increaseDocWeight(Document d, double amount) {
        increaseDocWeight(d, amount, true);
    }
    
    public void increaseDocWeight(Document d, double amount, boolean retrieve) {
        stopLayout();
        Iterator<Entity> ents = d.getEntityIterator();
        ArrayList<Entity> entsToUpweight = new ArrayList<Entity>();

        while(ents.hasNext()) {
            Entity e = ents.next();
            entsToUpweight.add(e);
            if(!retrieve) {
                increaseEntityStrength(e, DataListener.OTHER);
            }
        }
        
        if(retrieve) {
            retrieveDocuments(entsToUpweight, DataListener.OTHER, amount, retrieve);
        }
        
        data.updateDocument(d, DataListener.INCREASED);

        startLayout();
    }
    
    
    public void decreaseDocWeight(Document d) {
        stopLayout();
        Iterator<Entity> ents = d.getEntityIterator();
        
        while(ents.hasNext()) {
            Entity e = ents.next();
            this.decreaseEntityStrength(e, DataListener.OTHER);
        }
        data.updateDocument(d, DataListener.OTHER);
        
        pruneDocuments();

        mainPanel.tryRefresh();
        startLayout();
    }

    /**
     * A way for the application to get this controller's tool bar.
     * @return Tool bar for this dataset.
     */
    public JToolBar getActionToolBar() {
        return tools;
    }

    /**
     * Forwards the set of actions from the layout of the controller.
     * @return A set of actions.
     */
    public ArrayList<AbstractAction> getLayoutActions() {
        return graphLayout.getActions();
    }

    /**
     * This returns a read-only list of the nodes in the graph.
     * @return ArrayList of nodes that are pinned
     */
    public ArrayList<Node> getGraphPinned() {
        return graph.getPinned();
    }

    /**
     * Returns the selected node(s) in the graph.
     * @return Node which is selected in the graph.
     */
    public Node getGraphSelected() {
        return graph.getSelected();
    }

    /**
     * Returns an iterator for the nodes in the graph.
     * @return node iterator
     */
    public Iterator<Node> getNodeIterator() {
        return graph.nodeIterator();
    }

    /**
     * Return an iterator for the edges in the graph model.
     * @return Edge iterator
     */
    public Iterator<Edge> getEdgeIterator() {
        return graph.edgeIterator();
    }
    
    public boolean isMSSIOn() {
        return mssiOn;
    }

    /**
     * Gets an edge for 2 documents. This will look up the document's corresponding nodes
     * and then find or create an edge between them.
     * Documents MUST be different. And these documents must be contained in
     * the dataset!
     * @param d1 first document
     * @param d2 second document
     * @return Edge for the 2 documents
     */
    public Edge getEdge(Document d1, Document d2) {
        assert (d1 != d2);
        Node n1, n2;
        Edge e;
        n1 = findNode(d1);
        n2 = findNode(d2);
        assert (n1 != null && n2 != null);
        e = lookUpEdge(n1, n2);
        if (e == null) {
            //No edge exists between d1 and d2, create one and return it
            e = graph.addEdge(n1, n2);
        } else {
            /*
             * There is already an edge just up it's weight!
             */
            graph.changeEdgeStrength(e, e.getStrength() + EDGE_UP_WEIGHT_ADDER);
        }
        return e;
    }

    /**
     * Gets an edge for 2 documents. This will look up the document's corresponding nodes
     * and then find or create an edge between them.
     * Documents MUST be different. And these documents must be contained in
     * the dataset!
     * @param d1 first document
     * @param d2 second document
     * @return Edge for the 2 documents
     */
    public Edge getEdge(Search s, Document doc) {
        //assert (s != doc);
        Node n1, n2;
        Edge e;
        n1 = findNode(doc);
        n2 = findNode(s);
        //System.out.println("here, with the search node of: "+n2);
        assert (n1 != null && n2 != null);
        e = lookUpEdge(n1, n2);
        if (e == null) {
            //No edge exists between d1 and d2, create one and return it
            e = graph.addEdge(n1, n2);
            //System.out.println("getEdge is adding an edge between: "+n1+", "+n2);
        } else {
            /*
             * There is already an edge just up it's weight!
             */
            graph.changeEdgeStrength(e, e.getStrength() + EDGE_UP_WEIGHT_ADDER);
        }
        return e;
    }

    /**
     * A way for the application to add the open node view to the menu
     * @return NodeViewAction for this document.
     */
    public NodeViewAction getNodeViewAction() {
        return nodeAction;
    }

    /**
     * Returns this graph's main GraphView panel.
     * @return GraphView panel for this project
     */
    public GraphView getGraphView() {
        return mainPanel;
    }

    public ArrayList<AbstractAction> getFunctionActions() {
        return functions;
    }

    public ArrayList<AbstractAction> getEntitiesActions() {
        return entities;
    }

    public ArrayList<AbstractAction> getDocumentsActions() {
        return documents;
    }

    /**
     * Passes the query on to the graph to move a node.
     * @param current Node to be moved
     * @param newX new x position
     * @param newY new y position
     */
    public void moveNode(Node current, int newX, int newY) {
        graph.moveNode(current, newX, newY);
    }

    /**
     * Checks if the selected node is selected in the graph.
     * In this case a node.
     * @param n Node in question
     * @return true if node is selected
     */
    public boolean isSelected(Node n) {
        return graph.isSelected(n);
    }

    /**
     * This checks if the linking process is underway. Returns true if linking.
     * @return true if linking false other wise.
     */
    public boolean isLinking() {
        return linking;
    }

    /**
     * This is a private helper. It toggles linking on or off but can't be
     * called from outside, it must be accessed through the action.
     */
    protected void toggleLinking() {
        linking = !linking;
    }

    /**
     * This imports entities from a .txt file
     * Format: ent1
     *         ent2
     * ... (one entity per line)
     */
    protected void importEntitiesFromFile(File file) {
        /*
         * Parse the files for entities
         */
        int numOfEntities = 0;
        String line = "";
        String entity = "";
        //String type = "";
        Scanner scanner = null;
        try {
            long start = System.currentTimeMillis();
            System.out.println("importing entities");
            StringBuilder sb = new StringBuilder();
            scanner = new Scanner(new FileInputStream(file));
            Parser parser = new Parser();
            while (scanner.hasNextLine()) {
                //each line contains an entity
                line = scanner.nextLine();
                if (numOfEntities % 10 == 0) {
                    System.out.print(".");
                }

                if ((line.length() <= 1) || (parser.isStopWord(line)) || (hasEntity(line))) {
                    //entity exists in project, do nothing?
                    System.out.println("not adding entity: " + line);
                } else {
                    //entity does not exist in project, add it
                    System.out.println("adding entity: " + line);
                    addEntity(line, false);
                    numOfEntities++;
                }
            }

            long end = System.currentTimeMillis();
            System.out.println("done");
            System.out.println("added " + numOfEntities + " entities.");
            System.out.println("imported entities in: " + (end - start) + " milliseconds.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex, "Import Entity exception!",
                    JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "", ex);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * This starts a logger on the file to save all the interaction data.
     * @param f file to use to start the logger...
     */
    protected void startSoftDataLog(File f) {
        if (softdata == null) {
            softdata = new SoftDataLogger(f, this);
            data.addDataListener(softdata);
            graph.addListener(softdata);
        }
    }

    /**
     * Checks if there is an entity with a certain name.
     * @param name name of the entity to find
     * @return true if it exists false otherwise.
     */
    public boolean hasEntity(String name) {
        return data.hasEntity(name);
    }

    /**
     * Pins a node.
     * @param selected sets the selected node
     * @param b sets state. true for selected false for not selected.
     */
    public void pinNode(Node selected, boolean b) {
        graph.pinNode(selected, b);
    }

    /**
     * Sets a new node as selected.
     * @param selected - node to set a selected
     */
    public void setSelected(Node selected) {
        graph.setSelected(selected);
    }
    
    public void setIsLinking(boolean b) {
        linking = b;
    }

    /**
     * set whether a node is open or not
     * @param selected selected node
     * @param b if true set to open, close otherwise
     */
    public void setNodeOpen(Node selected, boolean b) {
        graph.setNodeOpen(selected, b);
        if(!b) {
            if(selected instanceof DocumentNode) {
                DocumentNode dn = (DocumentNode) selected;
                Iterator<Entity> ents = dn.getDocument().getEntityIterator();
                while(ents.hasNext()) {
                    Entity e = ents.next();
                    this.decreaseEntityStrength(e, DataListener.OTHER);
                }
                
            }
                
                
            
        }
        mainPanel.tryRefresh();
    }
    
    public void adjustNodeSize(Node selected, int amount) {
        int width = selected.getWidth() + amount;
        int height = selected.getHeight() + amount;
        
        width = Math.max(5, width);
        height = Math.max(5, height);
        width = Math.min(50, width);
        height = Math.min(50, height);
        
        Dimension d = new Dimension(width, height);
        
        //System.out.println("changing to size " + width + ", " + height);
        
        boolean upweight = false;
        boolean downweight = false;
        
        //This makes sure that the size has changed
        if(width > selected.getWidth()) {
            upweight = true;
        }
        else if(width < selected.getWidth()) {
            downweight = true;
        }
        
        graph.setNodeClosedSize(selected, d);
        
        if(selected instanceof DocumentNode && Math.abs(amount) > 4) {
            DocumentNode dn = (DocumentNode) selected;
            Document doc = dn.getDocument();
            if(upweight) {
                increaseDocWeight(doc);
                //data.updateDocument(doc, DataListener.OTHER);
            }
            else if(downweight){
                decreaseDocWeight(doc);
                //data.updateDocument(doc, DataListener.OTHER);


            }
        }

        mainPanel.tryRefresh();
        
    }

    /**
     * Update the name of a document
     * @param document Document
     * @param text New document name
     */
    public void setDocumentName(Document document, String text) {
        data.setDocumentName(document, text);
    }
    
    private void retrieveDocuments(ArrayList<Entity> ent, int type) {
        double amount = 1;
        retrieveDocuments(ent, type, amount);
    }
    
    private void retrieveDocuments(ArrayList<Entity> ent, int type, double amount) {
        retrieveDocuments(ent, type, amount, true);
    }    
    private void retrieveDocuments(ArrayList<Entity> ent, int type, double amount, boolean retrieve) {
        
        boolean canPrune = true;
        
        while(canPrune) {
            //see if any documents fall below a threshold and get rid of them
            canPrune = pruneDocuments();
        }


        
        /*String entString = "";
        
        for(Entity e : ent) {
        entString = entString.concat(e.getName());
        }
        
        
        System.out.println("My Entities: ");
        System.out.println(entString);
        
        BingHandler bh = new BingHandler();
        bh.storeArticles(data, bh.getArticles(entString));
        
        generateNewHiddenEntities();*/
        
        
    	int docAddLimit = 10;
        double threshold = data.getTotalStrength() * REG_THRESHOLD;
        if(type == DataListener.SEARCH) {
            docAddLimit = 20;
            threshold = data.getTotalStrength() * SEARCH_THRESHOLD;
        }
        
        
    	ArrayList<Document> docsToAdd = new ArrayList<Document>();
    	Iterator<Document> hiddenDocs = data.hiddenDocsIterator();
    	while (hiddenDocs.hasNext()) {
            Document current = hiddenDocs.next();
            boolean matchFound = false;
            for(int i = 0; i < ent.size(); i++) {
            	if(!matchFound) {
	            	if (current.getContent().toUpperCase().contains(ent.get(i).getName().toUpperCase())) {
	            		matchFound = true;
	            		docsToAdd.add(current);
	            	}
            	}
            }
    	}
    	//System.out.println("Found " + docsToAdd.size() + " potential matches");
    	
    	Document[] docsToSort = new Document[docsToAdd.size()];
        
        for(int i = 0; i < docsToAdd.size(); i++) {
            docsToSort[i] = docsToAdd.get(i);
        }
        
        //note these are reverse sorted
        for(int i = 1; i < docsToSort.length; i++) {
            Document docKey = docsToSort[i];
            
            double totalWeight = docsToSort[i].getTotalEntityStrength();
            
            //double tfidf = ent.getTFIDFdoc(docsToSort[i]);
            int j = i - 1;
            while(j >= 0 && docsToSort[j].getTotalEntityStrength() > totalWeight) {
                docsToSort[j + 1] = docsToSort[j];
                j--;
            }
            docsToSort[j + 1] = docKey;
        }
        
        //System.out.println("results sorted");
        
        //add top n docs to the screen
        if(docAddLimit > docsToSort.length) {
        	docAddLimit = docsToSort.length;
        }
        for(int i = docsToSort.length - 1; i > docsToSort.length - docAddLimit - 1; i--) {
            Document d = docsToSort[i];
            double docStrength = d.getTotalEntityStrength();
			         
            if(docStrength > threshold) {
			            
                data.addDocument(d);
                Iterator<Entity> entities = d.iterator();
                while(entities.hasNext()) {
                    Entity e = entities.next();
                    data.getEntity(e.getName()).addDocument(d);
                    this.entityDocumentAdded(e, d);
                }
            }
            else {
                //System.out.println("doc rejected - not relevant enough");
            }
        }
        
        //System.out.println(docAddLimit + " added to workspace");
        
        if(docAddLimit > 0) {
            //docs were added, update the recency. otherwise don't
            updateRecency();
        }
        
        
        for(Entity e : ent) {
            setEntityStrength(e, (e.getStrength() + 1.0 * amount), type);
        }
          
        docsToAdd.clear();
        //System.out.println("done");
        
        Iterator<Search> searches = data.searchIterator();
        while(searches.hasNext()) {
            Search s = searches.next();
            searchHighlight(s);
        }
        mainPanel.tryRefresh();
    	
    }
    
    private void updateRecency() {
        Iterator<Document> docs = data.documentIterator();
        while(docs.hasNext()) {
            Document d = docs.next();
            d.increaseRecency();
        }
    }
    
    /**
     * Gets rid of a document that falls below a given threshold. This is done one at a time in order to avoid concurrent modification errors
     */
    private boolean pruneDocuments() {
        //remove documents below a certain relevance threshold or document weight?
        Iterator<Document> docs = data.documentIterator();
        double cutoff = data.getTotalStrength() * PRUNING_THRESHOLD;
        boolean removed = false;
        while(docs.hasNext()) {
            Document d = docs.next();
            if(d.getTotalEntityStrength() < cutoff && !findNode(d).isOpen()) {
                Node n = graph.getSelected();
                if(n instanceof DocumentNode) {
                    DocumentNode dn = (DocumentNode) n;
                    if(dn.getDocument().getName().compareTo(d.getName()) != 0) {
                        //System.out.println("Trying to remove document " + d.getName());
                        removed = data.removeDocument(d);
                        if(!removed) {
                            //System.out.println("Could not remove " + d.getName());
                        }
                        return removed;
                        }
                    }
            }
        }
        return removed;
    }
    

    /**
     * This will eventually be the "smart" way to increase the entity strength.
     *   greedy? make the current one the max? normalize?
     * @param ent Entity being updated
     * @param type int that will carry what is being done {highlight, search, annotate}
     */
    private void increaseEntityStrength(Entity ent, int type) {
        //System.out.println("increasing strength of " + ent.getName());

        double maxEntStrength = data.getMaxEntityStrength();

        Iterator<Entity> ents = data.entityIterator();
        Entity currentEnt;
        double maxStrength = -1;

        while (ents.hasNext()) {
            currentEnt = ents.next();
            if (currentEnt.getStrength() > maxStrength) {
                maxStrength = currentEnt.getStrength();
            }
        }

        //set the strength as the highest strength
        //this should be the only place this is done
        //setEntityStrength(ent, (maxStrength + 2));
        setEntityStrength(ent, ent.getStrength() + 1, type);
        //setEntityStrength(ent, (ent.getStrength() + (data.getTotalStrength() / 20)), type);
        
        
    }

    /**
     * This will eventually be the "smart" way to decrease the entity strength.
     *   TODO: what's a smart way to decrease the strength of an entity given the interaction?
     * @param ent Entity being updated
     * @param int type that will carry what is being done {highlight, search, annotate}
     */
    private void decreaseEntityStrength(Entity ent, int type) {

        //this should be the only place this is done
        setEntityStrength(ent, (ent.getStrength() * 0.75), type); //TODO: make this smarter!!
        
    }

    /**
     * manually sets the strength of an entity.
     * @param selectedEntity Entity to update
     * @param value new strength
     * @param int type the type of modification that has caused this change
     */
    public void setEntityStrength(Entity selectedEntity, double value, int type) {
        double strengthChange = value - selectedEntity.getStrength();
        //System.out.println("strengthChange is: " + strengthChange);
        data.setEntityStrength(selectedEntity, value, type);

        if (strengthChange >= 0) {
            //update all other entities to ensure same totalStrength
            //System.out.println("About to update remaining entities");
            data.updateAllStrength(selectedEntity, strengthChange, type);
            
        } else {
            //TODO what should happen here, all entities gain a little strength?
        }
    }

    /**
     * Rename an entity manually
     * @param selectedEntity Entity to rename
     * @param value new name.
     */
    public void setEntityName(Entity selectedEntity, String value) {
        data.setEntityName(selectedEntity, value);
    }

    /**
     * Sets the weight of a node manually.
     * @param displayedNode node to update
     * @param value new weight
     */
    public void setNodeWeight(Node displayedNode, double value) {
        graph.setNodeWeight(displayedNode, value);
    }

    /**
     * Request a node resize.
     */
    public void setNodeSize(Node n, Dimension d) {
        graph.setNodeSize(n, d);
    }

    /**
     * Set or update the note for a document.
     * @param document Document to update note for.
     * @param text New note
     */
    public void setNote(Document document, String text) {
        graphLayout.stop();
        data.setNote(document, text);
        mainPanel.tryRefresh();
        graphLayout.start();
    }

    /**
     * Opens the document viewer.
     * @return a ref to the doc viewer
     */
    public DocumentViewer openDocumentViewer() {
        if (docViewer == null) {
            docViewer = new DocumentViewer(this);
            docViewer.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    closeDocumentViewer();
                }
            });
        }
        return docViewer;
    }

    /**
     * Closes the document viewer
     * either the doc viewer calls this through it's closing event or
     * something else could call it.
     */
    public void closeDocumentViewer() {
        if (docViewer != null) {
            docViewer.setVisible(false);
            data.removeDataListener(docViewer);
            docViewer.dispose();
            docViewer = null;
        }
    }

    /**
     * Opens the document viewer.
     * @return a ref to the doc viewer
     */
    public EntityViewer openEntityViewer() {
        if (entViewer == null) {
            entViewer = new EntityViewer(this);
            entViewer.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    closeEntityViewer();
                }
            });
        }
        return entViewer;
    }

    /**
     * Closes the document viewer
     * either the doc viewer calls this through it's closing event or
     * something else could call it.
     */
    public void closeEntityViewer() {
        if (entViewer != null) {
            entViewer.setVisible(false);
            data.removeDataListener(entViewer);
            entViewer.dispose();
            entViewer = null;
        }
    }

    /**
     * Parse the list of documents for entities that are currently in the model.
     * NOTE: This will not create new entities, only parse documents that have been
     * added to the model for entities that are currently in the model.
     */
    private void parseDocumentsForCurrentEntities() {
        //System.out.print("parsing new docuemnts for entities");
        Iterator<Document> docs = data.documentIterator();
        int doccount = 1;
        Parser parser = new Parser();
        while (docs.hasNext()) {
            Document doc = docs.next();
            //System.out.println("("+doccount+")");
            doccount++;
            //Parse each document for entities
            String toParse = doc.getContent();
            ArrayList<String> stringList = parser.parseString(toParse);

            for (String s : stringList) {
                if (data.hasEntity(s)) {
                    //adds Entity to list in Doc, Doc to list in Entity
                    //System.out.println("linking with entity: " + s);
                    data.link(data.getEntity(s), doc);
                }
            }
        }
        //System.out.println("done.");
    }

    /**
     * This takes a jigsaw file and parses the xml. It adds all the documents
     * and entities to the current model.
     * Note: this pauses the layout.
     * @param file Jigsaw xml file
     */
    private void addDocumentsFromJigsaw(File file) {
        /*
         * stop the layout to avoid concurrent problems
         */
        graphLayout.stop();

        /*
         * Read the info from the .jig files
         */
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            JigsawSAXHandler handler = new JigsawSAXHandler(data);
            InputStream inputStream = new FileInputStream(file);
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            InputSource is = new InputSource(reader);

            //NOTE: ome jigsaw files are not UTF-8 even tho they say so.
            is.setEncoding("UTF-8");

            //System.out.print("Parsing documents");
            saxParser.parse(is, handler);
            //System.out.println("done");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex, "Jigsaw file import exception!",
                    JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "", ex);
            //System.err.print(ex.getStackTrace());
        }
        System.out.println(data.getDocumentCount() + " documents in StarSPIRE");
        System.out.println(data.getHiddenDocCount() + " documents loaded");
        System.out.println(data.getEntityCount() + " entities in StarSPIRE");
        try {
            String fn = file.getName().substring(0, 3).toUpperCase();
            //Print initial binary values for SVD purposes
            PrintWriter pw = new PrintWriter(fn + "_initial_values.txt");
            pw.print("\t");
            Iterator<Document> di = data.documentIterator();
            for(int i = 0; i < data.getDocumentCount(); i++) {
                Document d = di.next();
                pw.print(d.getName() + "\t");
            }
            System.out.println();

            Iterator<Entity> ei = data.entityIterator();
            for(int i = 0; i < data.getEntityCount(); i++) {
                Entity e = ei.next();
                pw.print(e.getName() + "\t");

                Iterator<Document> dit = data.documentIterator();
                for(int j = 0; j < data.getDocumentCount(); j++) {
                    Document d = dit.next();
                    if(d.hasEntity(e)){
                        pw.print(1 + "\t");
                    }
                    else {
                        pw.print(0 + "\t");
                    }
                }
                pw.println();
            }
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(StarSpireController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        /*
         * Start the layout again AFTER all the loading is done.
         */
        
        
        
        //calculates initial weighting and coloring (emphasis right now is docs with more entities)
        this.data.newDocsAdded();
        


        startLayout();
    }

    /**
     * Imports Files into the dataset. Doesn't generate entities at the time.
     * TODO: should ask user if new documents should be parsed for potentially new
     * entities, or if only current entities should be applied to new docs
     * @param files array of files to load up.
     */
    private void addDocumentsFromFile(File[] files) {
        /*
         * stop the layout to avoid concurrent problems
         */
        graphLayout.stop();

        try {
            /*
             * Read the info from the .txt files
             */
            int numOfDocs = 0;
            /*
             * Add a Document for each file
             */
            long start = System.currentTimeMillis();
            System.out.print("loading documents");
            for (File file : files) {
                StringBuilder sb = new StringBuilder();
                Scanner scanner = new Scanner(new FileInputStream(file));
                try {
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                    }
                } finally {
                    scanner.close();
                }
                data.addDocument(sb.toString(), file.getName());
                if (numOfDocs % 1000 == 0) {
                    System.out.print(".");
                }
                //System.out.println("Created Document: " + file.getName());
                numOfDocs++;
            }
            long end = System.currentTimeMillis();
            System.out.println("done");
            System.out.println("Loaded documents in: " + (end - start) + " milliseconds.");
            System.out.println("Imported " + numOfDocs + " files.");
            /*
             * Parse each Document for Entities
             */
            parseDocumentsForCurrentEntities();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex, "File Folder import exception!",
                    JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "File Folder import exception", ex);
            //System.err.print(ex);
        }

        /*
         * Start the layout again AFTER all the loading is done.
         */
        startLayout();

    }

    /**
     * This generates entities using CPA's entity extractor when a new set
     * text documents are imported in to ForceSPIRE.
     */
    private void generateNewEntities() {
        if (this != null) {
            graphLayout.stop();

            long start = System.currentTimeMillis();
            System.out.print("Thread " + Thread.currentThread().getName()
                    + "Generating entities");
            Iterator<Document> docs = data.documentIterator();
            int doccount = 0;
            int entcount = 0;
            //TODO initial size should be some kind of config variable
            //HashSet<String> uniqueKeys = new HashSet<String>(60000);
            Parser parser = new Parser();
            String toParse = "";
            while (docs.hasNext()) {
                Document doc = docs.next();
                //System.out.println("("+doccount+")");
                doccount++;
                //Parse each document for entities
                toParse += doc.getContent() + "\n\n";
            }

            ArrayList<String> stringList = EntityExtractorWrapper.extractEntities(toParse);
            System.out.println("Number of entities found: " + stringList.size());

            for (String s : stringList) {
                if (!data.hasEntity(s) && !parser.isStopWord(s)) {
                    //uniqueKeys.add(s);
                    //these are all entities, add them
                    addEntity(s, false);
                    //Entity e = data.getEntity(s);
                    //adds Entity to list in Doc, Doc to list in Entity
                    //TODO: if already in list, does nothing right now. Should add weight?
                    //data.link(e, doc);
                    //System.out.println("Found " + entcount + " " + e.getName());
                    entcount++;
//                        if (entcount % 1000 == 0) {
//                            System.out.print(".");
//                        }
                } else {
                    //System.out.println(s+" is already an entity, or stopword!");
                }
                System.out.print(".");
            }

            long end = System.currentTimeMillis();
            System.out.println("Thread " + Thread.currentThread().getName()
                    + " Entities extracted in " + (end - start) + " miliseconds");
            System.out.println("Found " + entcount + " unique, new entities");
            System.out.println("       from " + doccount + " documents.");
            graphLayout.start();
        }
    }
    
    /**
     * This generates entities using CPA's entity extractor when a new set
     * text documents are imported in to ForceSPIRE and sent to hiddenDocs.
     */
    private void generateNewHiddenEntities() {
        if (this != null) {
            graphLayout.stop();

            long start = System.currentTimeMillis();
            System.out.print("Thread " + Thread.currentThread().getName()
                    + "Generating entities");
            Iterator<Document> docs = data.hiddenDocsIterator();
            int doccount = 0;
            int entcount = 0;
            //TODO initial size should be some kind of config variable
            //HashSet<String> uniqueKeys = new HashSet<String>(60000);
            Parser parser = new Parser();
            String toParse = "";
            while (docs.hasNext()) {
                Document doc = docs.next();
                //System.out.println("("+doccount+")");
                doccount++;
                //Parse each document for entities
                toParse += doc.getContent() + "\n\n";
            }

            ArrayList<String> stringList = EntityExtractorWrapper.extractEntities(toParse);
            System.out.println("Number of entities found: " + stringList.size());

            for (String s : stringList) {
                if (!data.hasEntity(s) && !parser.isStopWord(s)) {
                    //uniqueKeys.add(s);
                    //these are all entities, add them
                    addEntity(s, false);
                    //Entity e = data.getEntity(s);
                    //adds Entity to list in Doc, Doc to list in Entity
                    //TODO: if already in list, does nothing right now. Should add weight?
                    //data.link(e, doc);
                    //System.out.println("Found " + entcount + " " + e.getName());
                    entcount++;
//                        if (entcount % 1000 == 0) {
//                            System.out.print(".");
//                        }
                } else {
                    //System.out.println(s+" is already an entity, or stopword!");
                }
                System.out.print(".");
            }

            long end = System.currentTimeMillis();
            System.out.println("Thread " + Thread.currentThread().getName()
                    + " Entities extracted in " + (end - start) + " miliseconds");
            System.out.println("Found " + entcount + " unique, new entities");
            System.out.println("       from " + doccount + " documents.");
            graphLayout.start();
        }
    }

    private void generateTFIDFWeights() {
        Iterator<Entity> ents = data.entityIterator();
        Iterator<Document> docs = data.documentIterator();

        while (ents.hasNext()) {
        }


    }

    /**
     * Returns the number of docs that contain the entity
     * @param ent Entity being looked up
     * @return int number of documents that contain the entity
     */
    private double docsContainingEntity(Entity ent) {
        int documentsContainingTerm = 0;
        Iterator<Document> docs = data.documentIterator();

        while (docs.hasNext()) {
            if (docs.next().hasEntity(ent)) {
                documentsContainingTerm++;
            }
        }
        return documentsContainingTerm;
    }

    /**
     * Returns the tf
     * @param ent Entity being looked up
     * @param doc doc Document being checked
     * @return int tf
     */
    private double tf(Document doc, Entity ent) {
        String entString = ent.getName();
        String docString = doc.getContent();

        int lastIndex = 0;
        int count = 0;

        while (lastIndex != -1) {
            lastIndex = docString.indexOf(entString, lastIndex);
            if (lastIndex != -1) {
                count++;
            }
        }
        return Math.sqrt((double) count);
    }

    /**
     * This removes the given listeners from the graph's listener list.
     * The given listener will no longer get calls.
     * @param gl A graphListener (typically a view)
     */
    public void removeGraphListener(GraphListener gl) {
        graph.removeListener(gl);
    }

    /**
     * Removes a listener from the data model.
     * @param listener listener to remove
     */
    public void removeDataListener(DataListener listener) {
        data.removeDataListener(listener);
    }

    /**
     * This removes a node from the graph
     * @param selected
     */
    public void removeNode(Node selected) {
        graph.removeNode(selected);
    }

    /**
     * Removes an entity from the model
     * @param selectedEntity entity to remove.
     */
    public void removeEntity(Entity selectedEntity) {
        data.removeEntity(selectedEntity);
        mainPanel.tryRefresh();
    }
    
    public void removeEntity(String s) {
    	Entity e = data.getEntity(s);
    	if(e != null) {
    		data.removeEntity(e);
    		mainPanel.tryRefresh();
    	}
    }

    /**
     * Removes a search from the model.
     * @param search to remove from model.
     */
    public void removeSearch(Search search) {
        //System.out.println("Adout to remove a search in removeSearch(s)");
        clearSearchHighlight(search);
        data.removeSearch(search);
        mainPanel.tryRefresh();
    }

    /**
     * Removes a document from the data model.
     * @param selectedDocument Document to remove
     */
    public void removeDocument(Document selectedDocument) {
        data.removeDocument(selectedDocument);
        mainPanel.tryRefresh();
    }

    /**
     * Helper function for generating random numbers,
     * it's useful when randomizing node positions.
     * @param min Lower bound
     * @param max Upper bound
     * @return returns an integer between (inclusive) min and max.
     */
    private int random(int min, int max) {
        return min + r.nextInt(max - min);
    }

    /**
     * TODO do this with actions like the node thing
     */
    public void startLayout() {
        graphLayout.start();
    }
    
    public void stopLayout() {
        graphLayout.stop();
    }
    
    public void addSearch(String query) {
        Search s = null;
        //graph.addNode(GraphModel.Position.CENTER, s);
        PointerInfo a = MouseInfo.getPointerInfo();
        Point point = new Point(a.getLocation());
        SwingUtilities.convertPointFromScreen(point, mainPanel);
        SearchNode sn = (SearchNode) graph.addNode(point,s);
        s = search(sn, query);
        sn.setEnabledSearchButton(false);
        
        
    }

    /**
     * Searches the content of all documents for the search string.
     * If the search term is not currently and entity, an entity is created from the query
     * @param query String to search for
     * @return number of search hits (# of documents in which the string was found)
     */
    public Search search(SearchNode s, String query)  {
        graphLayout.stop();
        
        //Webscraping modifications
        //BingHandler bh = new BingHandler();
        //bh.storeArticles(data, bh.getArticles(query));
        
        Iterator<Document> docs = data.documentIterator();
        Iterator<Document> hiddenDocs = data.hiddenDocsIterator();
        //ArrayList<Document> results = new ArrayList<Document>();
        int hits = 0;
        currentSearchNode = s;
        Search r = null;
        boolean newTerm = false;
        
        if(!data.hasEntity(query)) {
        	newTerm = true;
        }

        /*
         * We updating a search box clear first.
         */
        if (s.getSearch() != null) {
            this.clearSearchHighlight(s.getSearch());
        }


        
        /*
         * make search dynamically load a list of it's documents (not saved?)
         * use document list to highlight...
         */
        while (docs.hasNext()) {
            Document current = docs.next();
            if (current.getContent().toUpperCase().contains(query.toUpperCase())) {
                hits++;
                //results.add(current);
                this.documentModified(current, DataListener.SEARCH);
                //this.getGraphView().repaint();
                //this.getGraphView().documentModified(current, DataListener.SEARCH);
                Node n = findNode(current);
                if(n.isOpen()) {
                    data.updateDocument(current, DataListener.SEARCH);
                    //this.graph.setNodeOpen(n, true);
                    //this.data.fireDocumentChange(current, EventType.MODIFIED, DataListener.SEARCH);
                }
            }
        }
        
        int docCount = data.getDocumentCount();
        
        ArrayList<Entity> entToUpweight = new ArrayList<Entity>();
        entToUpweight.add(data.getEntity(query));
        retrieveDocuments(entToUpweight, DataListener.SEARCH);
        hits += data.getDocumentCount() - docCount;
        /*
        boolean docsAdded = false;
        ArrayList<Document> docsToAdd = new ArrayList<Document>();
        while (hiddenDocs.hasNext()) {
            Document current = hiddenDocs.next();
            if (current.getContent().toUpperCase().contains(query.toUpperCase())) {
                hits++;
                //results.add(current);
                
                //Add the hidden doc to the visible docs
                //data.addDocument(current);
                docsToAdd.add(current);
                docsAdded = true;
            }
        }
        
        System.out.println(hits + " hidden docs match the search");
       // for(int i = 0; i < docsToAdd.size(); i++) {
         //   data.addDocument(docsToAdd.get(i));
       // }
        for(Document d : docsToAdd) {
            data.addDocument(d);
            Iterator<Entity> entities = d.iterator();
            while(entities.hasNext()) {
                Entity e = entities.next();
                data.getEntity(e.getName()).addDocument(d);
                this.entityDocumentAdded(e, d);
            }
            
        }
        */

        System.out.println("Hits: " + hits);
        if (hits > 0) {
        	if(newTerm) {
        		data.addEntity(query, false);
        	}
            if (data.hasEntity(query)) {
                /*
                 * Entity Already exists
                 */
                Entity e = data.getEntity(query);
                r = data.addSearch(query, e, hits);
                increaseEntityStrength(e, DataListener.SEARCH);
            } else {
                /*
                 * entity does not exist in the dataset yet
                 */
            	data.addEntity(query, true);
                //addEntity(query, true);
                Entity e = data.getEntity(query);
                r = data.addSearch(query, e, hits);
                increaseEntityStrength(e, DataListener.SEARCH);
            }

            /*
             * highlight results
             */
            graph.setNodeSearch(s, r);
            searchHighlight(r);
        }
        

        
        /*
        if(docsAdded) {
            data.newDocsAdded();
        }*/
        
        mainPanel.tryRefresh();

        
        
        graphLayout.start();
        return r;
    }

    /**
     * Clears the search, resetting all the nodes to default color.
     */
    public void clearSearchHighlight(Search s) {
        Iterator<Node> nodes = graph.nodeIterator();
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n instanceof DocumentNode && n.getHighlight() == s.getHue()) {
                n.setHighlight(0);
            }
        }
    }

    /**
     * Highlight a search's results
     */
    public void searchHighlight(Search s) {
        Iterator<Document> docs = data.documentIterator();
        String query = s.getSearchTerm();

        while (docs.hasNext()) {
            Document current = docs.next();
            if (current.getContent().toUpperCase().contains(query.toUpperCase())) {
                findNode(current).setHighlight(s.getHue());
            }
        }
        
    }

    /**
     * Graph was resized, let the layout know so it can deal with that.
     * @param e Details about the resize event.
     */
    public void componentResized(ComponentEvent e) {
        GraphView g = (GraphView) e.getComponent();
        graph.setGraphSize(g.getSize());
    }

    /**
     * We don't care about this right now.
     * @param e Details about the moved event
     */
    public void componentMoved(ComponentEvent e) {
    }

    /**
     * We don't care about this right now.
     * @param e Details about the show event
     */
    public void componentShown(ComponentEvent e) {
    }

    /**
     * We don't care about this right now.
     * @param e Details about the hide event
     */
    public void componentHidden(ComponentEvent e) {
    }

    /**
     * A document was added create a node for it.
     * @param doc document that was added
     */
    public void documentAdded(Document doc) {
  
       graph.addNode(GraphModel.Position.RANDOM, doc);


        
        needSave++;
    }
    
    
    public void setMSSI(boolean mssi) {
        mssiOn = mssi;
    }

    /**
     * A document was modified update if needed.
     * @param doc document that was modified
     */
    public void documentModified(Document doc, int type) {

        //if the entity was modified, update the weight accordingly
        switch (type) {
            case DataListener.OTHER:
                break;
            case DataListener.HIGHLIGHT:
                break;
            case DataListener.NOTE:
                break;
            case DataListener.SEARCH:
                break;
            case DataListener.LINK:
            	break;
            default:
                break;
        }

        needSave++;
        doAutoSave();
    }

    /**
     * A document was removed, remove corresponding node
     * @param doc document that was removed.
     */
    public void documentRemoved(Document doc) {
        graph.removeNode(doc);
        needSave++;
    }

    public void searchAdded(Search s) {
        graph.changeNodeSearch(currentSearchNode, s);
        needSave++;
    }

    /**
     * Event called when a search is removed.
     * @param s Search that is removed
     */
    public void searchRemoved(Search s) {
        graph.removeNode(s);
        needSave++;
    }

    /**
     * An entity was added, create corresponding edges.
     * @param ent Entity that was added
     */
    public void entityAdded(Entity ent) {
        Edge e;
        //System.out.println("EntityAdded: creating edges, doc count " + ent.documents());
        for (int i = 0; i < ent.documents() - 1; i++) {
            //connect to all other documents that share the entity
            for (int ii = i; ii < ent.documents(); i++) {
                //this really is i++
                //you only want a single edge between the list, so it's doing a skip within the nested loop!
                //System.out.println("Adding edge for doc" + i + " and " + ii);
                e = getEdge(ent.getDocument(i), ent.getDocument(ii));
                graph.addEdgeEntity(e, ent);
                e.calculateEdgeStrength();
            }
        }
        needSave++;
        doAutoSave();
    }

    /**
     * An entity was modified, remove/create edges if needed
     * @param ent entity that was modified
     * @param type gives information as to what caused the entity modification
     */
    public void entityModified(Entity ent, int type) {
        /*
         * Handles when document added one by one
         */
        //System.out.println("entityModified: Not supported yet.");

        //if an entity was modified, I want to update edges
        Iterator<Edge> edgeIt = graph.edgeIterator();

        while (edgeIt.hasNext()) {
            Edge e = edgeIt.next();
            if (e.hasEntity(ent)) {
                //this edge contains the modified entity, update!
                graph.updateEdge(e);
                //e.calculateEdgeStrength();
            }
        }

        //if the entity was modified, update the weight accordingly
        //increaseEntityStrength(ent, type);

        /*
         * switch (type) {
        case DataListener.ADDEDNOTE:
        increaseEntityStrength(ent, EntityUpdate.ANNOTATE);
        break;
        case DataListener.REMOVEDNOTE:
        decreaseEntityStrength(ent, EntityUpdate.ANNOTATE);
        break;
        case DataListener.OTHER:
        break;
        default:
        break;
        }
         * 
         */

        needSave++;
        doAutoSave();
    }

    /**
     * A document has been added to an entity, create an edge if needed
     * @param ent Entity modified
     * @param doc Document added to entity
     */
    public void entityDocumentAdded(Entity ent, Document doc) {
        Iterator<Document> it = ent.iterator();

        Document currentDoc;
        while (it.hasNext()) {
            currentDoc = it.next();
            if (!currentDoc.equals(doc)) {
                Edge e = getEdge(doc, currentDoc);
                graph.addEdgeEntity(e, ent);
            }
        }
        needSave++;
    }

    /**
     * A document has been removed from an entity, check if the edge has to be deleted.
     * @param ent Entity modified
     * @param doc Document removed from the entity
     */
    public void entityDocumentRemoved(Entity ent, Document doc) {
        Iterator<Document> it = ent.iterator();

        Document currentDoc;
        while (it.hasNext()) {
            currentDoc = it.next();
            if (!currentDoc.equals(doc)) {
                Edge e = getEdge(doc, currentDoc);
                graph.removeEdgeEntity(e, ent);
            }
        }
        needSave++;
        doAutoSave();
    }

    /**
     * A search has been added to an entity.
     * Add edges to documents if needed.
     * @param ent Entity modified
     * @param s Search being added to the entity
     */
    public void entitySearchAdded(Entity ent, Search s) {
        Iterator<Search> it = ent.searchIterator();
        Iterator<Document> itDoc = ent.iterator();

        Search currentSearch;
        Document currentDocument;
        while (it.hasNext()) {
            currentSearch = it.next();
            //make the edges between the search and the remaining nodes
            while (itDoc.hasNext()) {
                currentDocument = itDoc.next();
                Edge e = getEdge(currentSearch, currentDocument);
                graph.addEdgeEntity(e, ent);
            }
        }
        needSave++;
        doAutoSave();
    }

    /**
     * A search has been removed from an entity.
     * Remove edges from documents if needed.
     * @param ent Entity modified
     * @param s Search being removed from the entity
     */
    public void entitySearchRemoved(Entity ent, Search s) {
        //Iterator<Search> it = ent.searchIterator();
        Iterator<Document> itDoc = ent.iterator();


        System.out.println("entitySearchRemoved called");
        Search currentSearch;
        Document currentDocument;
        System.out.println("ent has search? ... " + ent.hasSearch(s));
        while (itDoc.hasNext()) {
            currentDocument = itDoc.next();
            System.out.println("currentSearch: " + s);
            //make the edges between the search and the remaining nodes
            if (currentDocument.hasEntity(s.getEntity())) {
                Edge e = getEdge(s, currentDocument);
                graph.removeEdgeEntity(e, ent);
                System.out.println("removeEdgeEntity removed an edge");
            }
        }
        needSave++;
        doAutoSave();
//
//            while(itDoc.hasNext()) {
//                currentDocument = itDoc.next();
//                Edge e = getEdge(currentSearch, currentDocument);
//                graph.removeEdgeEntity(e, ent);
//                System.out.println("removeEdgeEntity removed an edge");
//            }
//        }
//        needSave++;
//
//
//        Iterator<Document> it = ent.iterator();
//
//        Document currentDoc;
//        while (it.hasNext()) {
//            currentDoc = it.next();
//            if (!currentDoc.equals(doc)) {
//                Edge e = getEdge(doc, currentDoc);
//                graph.removeEdgeEntity(e, ent);
//            }
//        }




    }

    /**
     * An entity was removed, remove/update edges
     * @param ent entity that was removed
     */
    public void entityRemoved(Entity ent) {

        //System.out.println("in entityRemoved with ent: " + ent.toString());

        //remove the entity from edges
        Iterator<Edge> edgeIt = graph.edgeIterator();
        ArrayList<Edge> edgesToRemove = new ArrayList<Edge>();
        Edge e;
        int numOfEdges = 0;

        while (edgeIt.hasNext()) {
            e = edgeIt.next();
            numOfEdges++;
            if (e.hasEntity(ent)) {
                if (e.numOfEntities() < 2) {
                    //System.out.println("numOfEntities in edge: " + e.numOfEntities());
                    edgesToRemove.add(e);
                }
                //System.out.println("Removing entity: " + ent.toString());
                //System.out.println("from edge: " + e.toString() + "\n");
                graph.removeEdgeEntity(e, ent);
                e.calculateEdgeStrength();
            }
        }

        for (Edge ed : edgesToRemove) {
            //System.out.println("Removing the edge: " + ed.toString());
            graph.removeEdge(ed);
        }

        //System.out.println("^^ num of edges before delete: " + numOfEdges);
        needSave++;
        doAutoSave();
    }
}
