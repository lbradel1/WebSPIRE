package starspire.controllers;

import starspire.models.Entity;
import starspire.models.Node;
import starspire.models.DataListener;
import starspire.models.Document;
import starspire.models.Highlight;
import starspire.models.Search;
import starspire.models.Edge;
import starspire.models.GraphListener;
import starspire.StarSpireApp;
import java.awt.Dimension;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.util.Iterator;

/**
 * This file will be used to log soft data into a CSV file so we can look at it later.
 * @author Patrick Fiaux
 */
public class SoftDataLogger implements DataListener, GraphListener { //TODO implement stuff

    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());
    private static final SimpleDateFormat date_format = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SSS");
    //private static final Logger softdata = Logger.getLogger(SoftDataLogger.class.getName());
    FileWriter fileStream;
    BufferedWriter buffer;
    StarSpireController controller;
    private Node selectedNode;
    private Node lastModifiedNode;

    /**
     * Basic constructor, takes file to log to and controller to log from to re-
     * quest additional data in some cases.
     * @param f File to log to
     * @param c Controller this is tied to
     */
    public SoftDataLogger(File f, StarSpireController c) {
        controller = c;
        try {
            fileStream = new FileWriter(f);
            buffer = new BufferedWriter(fileStream);

        } catch (IOException ex) {
            //logger.
            logger.log(Level.SEVERE, "Failed to initialize file for soft data log", ex);
        }
    }

    /**
     * This just flushes the buffer... not sure that does much but at least
     * we tried to make sure stuff gets written...
     */
    public void flush() {
        try {
            buffer.flush();
            fileStream.flush();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to flush buffer", ex);
        }
    }

    /**
     * Close the file when done. Should be called when closing the project.
     */
    public void closeLog() {
        flush();
        try {
            //buffer.close();
            fileStream.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to close the log", ex);
        }
    }

    /**
     * Takes current milisec time and format it to something nice.
     * @return Formated time string
     */
    private String currentTime() {
        return date_format.format(System.currentTimeMillis());
    }

    /**
     * Adds an entry to the log.
     * @param type
     * @param soft
     * @param params
     */
    private void log(String type, boolean soft, Object[] params) {
        try {
            buffer.write("\"" + currentTime() + "\",");
            buffer.write("\"" + type + "\",");
            if (soft) {
                buffer.write("\"true\",");
            } else {
                buffer.write("\"false\",");
            }
            for (Object o : params) {
                buffer.write("\"" + o.toString() + "\",");
            }
            buffer.write('\n');
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to write to BufferedWriter", ex);
        }
    }

    /**
     * Adds an entry to the log, given a document that has been modified.
     * @param type
     * @param soft
     * @param params
     */
    private void log(String type, boolean soft, Document doc) {
        try {
            //time
            buffer.write("\"" + currentTime() + "\",");
            //type
            buffer.write("\"" + type + "\",");
            //soft data
            if (soft) {
                buffer.write("\"true\",");
            } else {
                buffer.write("\"false\",");
            }
            //doc to string
            buffer.write("\"" + doc.toString() + "\",");
            //Doc text
           // buffer.write("\"" + doc.getContent().trim() + "\",");


            /*
            if (type.equalsIgnoreCase("Document_Highlight")) {
                Iterator<Highlight> it = doc.highlightIterator();
                Highlight current;
                while (it.hasNext()) {
                    current = it.next();
                    buffer.write("\"" + doc.getContent(current.start, current.end) + "\",");
                }
            } else if (type.equalsIgnoreCase("Document_Note")) {
                //doc notes
                buffer.write("\"" + doc.getNotes().trim() + "\",");
            }
            */
            
            buffer.write('\n');
           
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to write to BufferedWriter", ex);
        }
    }
    
    private String eventTypeToString(int type) {
        String eventType = "";
        
        switch(type) {
            case DataListener.ADDEDNOTE:
                eventType = "Note_Added";
                break;
            case DataListener.HIGHLIGHT:
                eventType = "Highlight";
                break;
            case DataListener.LINK:
                eventType = "LINK";
                break;
            case DataListener.NOTE:
                eventType = "Note";
                break;
            case DataListener.OTHER:
                eventType = "Other";
                break;
            case DataListener.REMOVEDNOTE:
                eventType = "Note_Removed";
                break;
            case DataListener.SEARCH:
                eventType = "Search";
                break;
        }
        return eventType;
    }

    /**
     * Unused
     * @param doc
     */
    public void documentAdded(Document doc) {
        log("Document_added", true, doc);
    }

    /**
     * When a document is modified figure out if it's
     * a modification of:
     * * the notes and annotations
     * * the highlights
     * @param doc Document modified
     * @param type modification type
     */
    public void documentModified(Document doc, int type) {
        switch (type) {
            case DataListener.NOTE:
                log("Document_Note", true, doc);
                break;
            case DataListener.HIGHLIGHT:
                log("Document_Highlight", true, doc);
                break;
            case DataListener.INCREASED:
            	log("Document_Increased", false, doc);
            	break;
            case DataListener.LINK:
            	log("Document_Linked", true, doc);
            	break;
        }
    }

    /**
     * A document was removed must be a user action too.
     * @param doc Document removed
     */
    public void documentRemoved(Document doc) {
        log("Document_removed", true, doc);
    }

    /**
     * An entity was added log so and mark wether it was a user or software action.
     * @param ent
     */
    public void entityAdded(Entity ent) {
        Object[] params = new Object[1];
        params[0] = ent;

        log("Enitity_Added", ent.isSoftData(), params);
    }

    /**
     * An entity was modified, so we will log the ent.toString() and the ent.getStrength()
     * @param ent Entity being logged
     * @param type The type of entity modification that has occurred.
     */
    public void entityModified(Entity ent, int type) {
        if(ent.getStrength() > 1) {
	    	Object[] params = new Object[4];
	        params[0] = ent;
	        params[1] = ent.getName();
	        params[2] = eventTypeToString(type);
	        params[3] = ent.getStrength();
	
	        log("Enitity_Modified", ent.isSoftData(), params);
	        flush();    //just to make sure?
        }
    }

    /**
     * Unused
     * @param ent
     * @param doc
     */
    public void entityDocumentAdded(Entity ent, Document doc) {
    }

    /**
     * Unused
     * @param ent
     * @param doc
     */
    public void entityDocumentRemoved(Entity ent, Document doc) {
    }

    /**
     * An entity was removed log it.
     * @param ent entity that was removed.
     */
    public void entityRemoved(Entity ent) {
        Object[] params = new Object[1];
        params[0] = ent;

        //As of now software never removes entities so it has to be a user action
        log("Enitity_Reoved", true, params);
    }

    /**
     * Unused
     * @param n
     */
    public void nodeAdded(Node n) {
    }

    /**
     * When a node moved log it if it's the selected node because the user is
     * moving it.
     * @param n Moved node
     */
    public void nodeMoved(Node n) {
        if (n == selectedNode) {
            Object[] params = new Object[1];
            params[0] = n;
            log("Node_Moved", true, params);
        }
    }

    /**
     * Unused
     * @param n
     */
    public void nodeRemoved(Node n) {
    }

    /**
     * A node was modified check what the modification was and log accordinly
     * @param n Node modified
     * @param t Type of modification
     */
    public void nodeModified(Node n, NodeModType t) {
        Object[] params = new Object[1];
        params[0] = n;
        if (t == NodeModType.PIN) {
            if (n.isPinned()) {
                log("Node_Pinned", true, params);
            } else {
                log("Node_unpinned", true, params);
            }
        } else if (t == NodeModType.WEIGHT) {
            if (n.isPinned()) {
                log("Node_Weight", false, params);
            } else {
                log("Node_Weight", false, params);
            }
        }
    }

    /**
     * Log when a node was selected
     * @param n a node was selected
     */
    public void nodeSelected(Node n) {
        if (n != null) {
            selectedNode = n;

            //log stuff
            Object[] params = new Object[1];
            params[0] = n;

            log("Node_Selected", true, params);
        }
    }

    /**
     * Log that a node was opened.
     * @param n opened Node
     */
    public void nodeOpened(Node n) {
        assert (n != null);

        Object[] params = new Object[1];
        params[0] = n;

        log("Node_Open", true, params);
    }

    /**
     * Log that a node was closed
     * @param n closed node
     */
    public void nodeClosed(Node n) {
        assert (n != null);

        Object[] params = new Object[1];
        params[0] = n;
        log("Node_Close", true, params);
    }

    /**
     * Unused
     * @param e
     */
    public void edgeAdded(Edge e) {
    }

    /**
     * Unused
     * @param e
     */
    public void edgeModified(Edge e) {
    }

    /**
     * Unused
     * @param e
     */
    public void edgeRemoved(Edge e) {
    }

    /**
     * Unused
     * @param d
     */
    public void graphResized(Dimension d) {
    }

    /**
     * Log that a search was added.
     * @param s Search added
     */
    public void searchAdded(Search s) {
        Object[] params = new Object[1];
        params[0] = s;
        log("New Search", true, params);
    }

    /**
     * Log that a search was deleted (entities stay)
     * @param s Search Removed
     */
    public void searchRemoved(Search s) {
        Object[] params = new Object[1];
        params[0] = s;
        log("Search Deleted", true, params);
    }

    /**
     * Unused
     * @param ent
     * @param s
     */
    public void entitySearchAdded(Entity ent, Search s) {
    }

    /**
     * Unused
     * @param ent
     * @param s
     */
    public void entitySearchRemoved(Entity ent, Search s) {
    }
}
