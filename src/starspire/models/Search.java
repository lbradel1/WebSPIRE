package starspire.models;

import org.json.*;

/**
 * The model represents an element loaded into ForceSpire, usually a small
 * text document.
 *
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class Search {

    private static int NEXT_SERIAL_ID = 0;
    private int id;
    private int results;
    private int hue; //where search node color gets set
    private String searchTerm;
    private Entity entity; //should we have this AND Entity has list of Documents?

    /**
     * Default Constructor
     */
    public Search() {
        setup(++NEXT_SERIAL_ID, (int) (Math.random() * 360.0), 0, "", null);
    }

    /**
     * Content Constructor
     * @param query 
     * @param ent
     * @param resutls
     */
    public Search(String query, Entity ent, int results) {
        //setup(++NEXT_SERIAL_ID, 2, results, query, ent);
        setup(++NEXT_SERIAL_ID, (int) (Math.random() * 360.0), results, query, ent);
    }
    
    public Search(String query, Entity ent, int results, int hue) {
        setup(++NEXT_SERIAL_ID, hue, results, query, ent);
    }

    public Search(JSONObject search) throws JSONException {
        int lid, h, r;
        String q;
        lid = search.getInt("ID");
        q = search.getString("query");
        try {
            h = search.getInt("hue");
        } catch (JSONException ex) {
            h = (int) (Math.random() * 360.0);
        }
        try {
            r = search.getInt("results");
        } catch (JSONException ex) {
            r = 0;
        }

        setup(lid, h, r, q, null);

        if (lid >= NEXT_SERIAL_ID) {
            NEXT_SERIAL_ID = lid + 1;
        }
    }

    /**
     * Helper for constructor.
     * @param nid id to use
     * @param s String searched
     */
    private void setup(int nid, int h, int r, String n, Entity e) {
        id = nid;
        searchTerm = n;
        entity = e;
        results = r;
        hue = h;
    }

    /**
     * This returns a JSON object representation of a document
     * @return a JSONObject representing this document's data
     * @throws JSONException something went wrong
     */
    public JSONObject getJSONObject() throws JSONException {
        JSONObject nodeJSON = new JSONObject();

        nodeJSON.put("ID", id);
        nodeJSON.put("query", searchTerm);
        nodeJSON.put("hue", hue);
        nodeJSON.put("results", results);

        return nodeJSON;
    }

    /**
     * Check if the entity is part of the entity list for this search.
     * @param ent The entity to check.
     * @return boolean True if it exists, False if not
     */
    public boolean hasEntity(Entity ent) {
        if (entity.getName().equalsIgnoreCase(ent.getName())) {
            return true;
        }
        return false;
    }

    public void setHue(int h) {
        hue = h;
    }
    /**
     * Get the hue to generate a color for this search...
     * @return hue (0-256)
     */
    public int getHue() {
        return hue;
    }

    /**
     * Get the number of results this search has...
     * @return number of results
     */
    public int getResults() {
        return results;
    }

    /**
     * Returns this search's entity.
     * @return Entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Update this search's entity used by load mostly
     * @param e Entity to set for this search...
     */
    protected void setEntity(Entity e) {
        entity = e;
    }

    /**
     * Returns the term(text) searched for
     * @return String The search query
     */
    public String getSearchTerm() {
        return searchTerm;
    }

    /**
     * This sets the term being searched for.
     * @param n The term being searched for
     */
    protected void setSearchTerm(String n) {
        searchTerm = n;
    }

    /**
     * Returns the id of this search
     * @return Search Unique ID
     */
    public int getId() {
        return id;
    }

    /**
     * Checks if both searches have equal id, name and content.
     * @param o Search to compare
     * @return true if both are identical searches.
     */
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o.getClass() == this.getClass()) {
                Search e = (Search) o;
                if (e.id != this.id) {
                    return false;
                }
                if (!e.searchTerm.equals(this.searchTerm)) {
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
    public String toString() {
        return searchTerm;
    }
}
