package starspire.models;

import java.awt.Color;
import org.json.*;
import java.util.*;
import java.util.ArrayList;

/**
 * The model represents an element loaded into Force/StarSpire, usually a small
 * text document.
 *
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class Document {

    private static int NEXT_SERIAL_ID = 0;
    private int id;
    private String name;
    private String content;
    private String notes;
    private ArrayList<Highlight> highlights;
    private ArrayList<Entity> entities; //should we have this AND Entity has list of Documents?
    private boolean isVisible;
    private double totalStrength;
    private int rank;
    private int quartile;
    private boolean isMSSI;
    private int recency = -1;

    /**
     * Default Constructor
     */
    public Document() {
        isMSSI = false;
        setup(++NEXT_SERIAL_ID, "ID" + NEXT_SERIAL_ID, "", "", null);
    }

    /**
     * Content Constructor
     * @param cont
     */
    public Document(String cont) {
        isMSSI = false;
        setup(++NEXT_SERIAL_ID, "ID" + NEXT_SERIAL_ID, cont, "", null);
    }
    
        /**
     * Content Constructor
     * @param cont
     */
    public Document(String cont, boolean SI) {
        isMSSI = SI;
        //System.out.println("used correct MSSI constructor");
        setup(++NEXT_SERIAL_ID, "ID" + NEXT_SERIAL_ID, cont, "", null);
    }

    /**
     * Content and name constructor, creates a document with the specified
     * content and name.
     * @param content content of the document
     * @param name title for doc
     */
    public Document(String content, String name) {
        isMSSI = false;
        setup(++NEXT_SERIAL_ID, name, content, "", null);
    }

    /**
     * JSON constructor. Used for loading a document from a JSON save.
     * @param doc JSON object containing doc data.
     * @throws JSONException JSON object not in the correct format.
     */
    public Document(JSONObject doc) throws JSONException {
        int lid;
        String n, c, not;
        ArrayList<Highlight> h = new ArrayList<Highlight>();
        lid = doc.getInt("ID");
        n = doc.getString("name");
        c = doc.getString("content");
        not = doc.getString("notes");

        JSONArray a = doc.getJSONArray("highlights");
        for (int i = 0; i < a.length(); i++) {
            h.add(new Highlight(a.getJSONArray(i)));
        }

        setup(lid, n, c, not, h);

        if (lid >= NEXT_SERIAL_ID) {
            NEXT_SERIAL_ID = lid + 1;
        }
    }

    /**
     * Helper for constructor.
     * @param nid id to use
     * @param n name for document
     * @param c content for document
     */
    private void setup(int nid, String n, String c, String not, ArrayList<Highlight> h) {
        id = nid;
        name = n;
        content = c;
        entities = new ArrayList<Entity>();
        highlights = new ArrayList<Highlight>();
        if (h != null) {
            highlights.addAll(h);
        }
        notes = not;
        
        isVisible = true;


        //System.out.println("Created Doc: " + id+","+n+",");
    }

    /**
     * This returns a JSON object representation of a document
     * @return a JSONObject representing this document's data
     * @throws JSONException something went wrong
     */
    public JSONObject getJSONObject() throws JSONException {
        JSONObject nodeJSON = new JSONObject();

        nodeJSON.put("ID", id);
        nodeJSON.put("name", name);
        nodeJSON.put("content", content);
        nodeJSON.put("notes", notes);
        JSONArray h = new JSONArray();
        for (Highlight s : highlights) {
            h.put(s.getJSONObject());
        }
        nodeJSON.put("highlights", h);

        return nodeJSON;
    }

    /**
     * This adds an entity to the entity list for Entity
     * @param e The entity to add to the list.
     */
    public void addEntity(Entity e) {
        entities.add(e);
    }

    /**
     * Check if the entity is part of the entity list for this document.
     * @param ent The entity to check.
     * @return boolean True if it exists, False if not
     */
    public boolean hasEntity(Entity ent) {
        for (Entity e : entities) {
            if (e.getName().equalsIgnoreCase(ent.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove the entity from the entity list of this document.
     * @param ent The entity to remove.
     */
    protected void removeEntity(Entity ent) {
        if (this.hasEntity(ent)) {
            //System.out.println("removing ent from doc");
            entities.remove(entities.indexOf(ent));
        }
    }

    /**
     * Returns an iterator for the entities of this document.
     * @return Entity iterator.
     */
    public Iterator<Entity> iterator() {
        return entities.iterator();
    }

    /**
     * Returns the name of this document
     * @return String containing the doc name
     */
    public String getName() {
        return name;
    }

    /**
     * This sets the name of the document.
     * @param n new doc name
     */
    protected void setName(String n) {
        name = n;
    }

    /**
     * Returns the content of this document
     * @return String containing the document's content
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the substring of the content of this document.
     * @param start int starting the substring
     * @param end int ending the substring
     * @return
     */
    public String getContent(int start, int end) {
        /* make sure that you don't go out of bounds */
        end = Math.min(content.length(), end);
        start = Math.max(0, start);
        return content.substring(start, end);
    }

    /**
     * Sets the content of this document
     * @param s New document content
     */
    protected void setContent(String s) {
        content = s;
    }

    /**
     * Returns the notes for this node.
     * @return notes string
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets the notes for this node
     * @param s new notes string
     */
    protected void setNotes(String s) {
        notes = s;
    }

    /**
     * Add a string to the highlights for this document
     * @param start
     * @param end
     */
    protected void addHighlight(int start, int end) {
        highlights.add(new Highlight(start, end));
    }
    
    protected void addHighlight(int start, int end, Color color) {
        highlights.add(new Highlight(start, end, color));
    }

    /**
     * Removes a string from the highlight list
     * @param h
     * @return true if successfully removed
     */
    protected boolean removeHightlight(Highlight h) {
        return highlights.remove(h);
    }

    /**
     * Returns an iterators for the highlights in this document
     * @return string iterator.
     */
    public Iterator<Highlight> highlightIterator() {
        return highlights.iterator();
    }

    /**
     * Returns the id of this document
     * @return Document Unique ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * 
     * @return true if the document should be visible, hidden otherwise
     */
    public boolean getIsVisible() {
        return this.isVisible;
    }
    
    /**
     * Sets the status of the document. This is for forcespire (psuedo) scale.
     * If a document falls below a relevance threshold, hide the document.
     * If a document falls above a relevance threshold, show the document.
     * @param b
     * @return 
     */
    public void setIsVisible(boolean b) {
        this.isVisible = b;
    }
    
    /**
     *  
     * @return the total strengths of entities contained in this document
     */
    public double getTotalEntityStrength() {
        this.totalStrength = 0;
        for(int i = 0; i < this.entities.size(); i++) {
            totalStrength += this.entities.get(i).getStrength();
        }
        return this.totalStrength;
    }
    
    public int getRank() {
        return rank;
    }
    
    public void setRank(int i) {
        rank = i;
    }
    
    public int getQuartile() {
        return quartile;
    }
    
    public void setQuartile(int i) {
        quartile = i;
    }
    
    public boolean isMSSI() {
        return isMSSI;
    }
    
    public void setMSSI(boolean b) {
        isMSSI = b;
    }
    
    public int getEntityCount() {
        return entities.size();
    }
    
    public Iterator<Entity> getEntityIterator() {
        return entities.iterator();
    }
    
    /**
     * 
     * @return how many iterations ago the document was added to the workspace
     */
    public int getRecency() {
        return recency;
    }
    
    /**
     * Increase the iteration number from when the document was retrieved
     */
    public void increaseRecency() {
        recency++;
    }

    /**
     * Checks if both docs have equal id, name and content.
     * @param o Document to compare
     * @return true if both are identical documents.
     */
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o.getClass() == this.getClass()) {
                Document e = (Document) o;
                if (e.id != this.id) {
                    return false;
                }
                if (!e.name.equals(this.name)) {
                    return false;
                }
                if (!e.content.equals(this.content)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns string name
     * @return name
     */
    @Override
    public String toString() {
        return "Document " + id + ": " + name;
    }
}
