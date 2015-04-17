package starspire.models;

import java.awt.Dimension;
import org.json.*;

/*
 * Imports
 */
/**
 * Node is the base item on a graph. They represent each documents in the current
 * data model.
 *
 * Nodes will have the ability to be open or closed. Open will display the document
 * close only a circle and may be a label.
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class SearchNode extends Node {

    /**
     * Minimum Open node width for search nodes
     */
    public static final int OPEN_SEARCH_MIN_WIDTH = 250;
    /**
     * Minimum Open node height for search nodes
     */
    public static final int OPEN_SEARCH_MIN_HEIGHT = 76; //change to 100 if showing num of results
    protected Search search;
    
    private boolean enableSearchButton = true;

    /**
     * Search Constructor
     * @param nX x location of node
     * @param nY y location of node
     * @param s Search to link to this node
     */
    public SearchNode(int nX, int nY, Search s) {
        super(nX, nY);
        setup(s);
    }

    /**
     * JSONObject constructor.
     * Used when loading files.
     * Note to avoid Serial_Id conflicts this will set Serial id to
     * max of(serial_id, node_id) so that by the time we're done loading
     * we know we'll avoid duplicating IDs.
     * @param node JSONObject containing node's parameters including SID.
     * @param model data model this node's document points to
     * @throws JSONException JSON object not in the correct format.
     */
    public SearchNode(JSONObject node, DataModel model) throws JSONException {
        super(node);

        //Load/Link Search
        int sid = node.getInt("searchID");
        Search s = model.lookUpSearch(sid);
        if (s == null) {
            // model.addSearch("Search Id: " + did + "Could not be linked");
            System.err.println("Node could not be linked to a search!");
        }

        setup(s);
    }

    private void setup(Search s) {
        /* Show up in green... */
        highlight++;
        /* default to pinned */
        pinned = true;

        isOpen = true;
        w = OPEN_SEARCH_MIN_WIDTH;
        h = OPEN_SEARCH_MIN_HEIGHT;
        /* Search Ties */
        search = s;
    }

    /**
     * Resize the node.
     * @precondition new width > OPEN_MIN_WIDTH
     * @precondition new height > OPEN_MIN_HEIGHT
     * @param d new dimension
     */
    @Override
    protected void setSize(Dimension d) {
        w = Math.max(d.width, OPEN_SEARCH_MIN_WIDTH);
        h = Math.max(d.height, OPEN_SEARCH_MIN_HEIGHT);
    }

    /**
     * This returns a JSON object representation of a node
     * @return a JSONObject representing this node's data
     * @throws JSONException something went wrong
     */
    @Override
    public JSONObject getJSONObject() throws JSONException {

        JSONObject nodeJSON = super.getJSONObject();

        nodeJSON.put("class", this.getClass().getName());
        nodeJSON.put("searchID", search.getId());

        return nodeJSON;
    }

    /**
     * Return the Search associated with this node
     * @return Search linked to this node
     */
    public Search getSearch() {
        return search;
    }

    protected void setSearch(Search s) {
        search = s;
    }
    
    public void setEnabledSearchButton(boolean b) {
        enableSearchButton = b;
    }
    
    public boolean getenableSearchButton() {
        return enableSearchButton;
    }

    /**
     * Implements to string, shows node id and location.
     * @return string representation of node.
     */
    @Override
    public String toString() {
        return "Search Node " + ID + " x: " + x + " y: " + y + " weight: " + weight + (pinned ? " pinned" : " not pinned");
    }

    /**
     * Override equals.
     * Check if object is a node, if so
     * check that they have:
     * same id
     * same x
     * same y
     * same weight
     * same number of edges
     * Note: does not check through edges for speed benefit.
     * @param o
     * @return true if these 2 nodes are basically the same.
     */
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o.getClass() == this.getClass()) {
                SearchNode n = (SearchNode) o;
                if (n.ID != this.ID) {
                    //System.out.println("FAIL id");
                    return false;
                }
                if (n.x != this.x) {
                    //System.out.println("FAIL x");
                    return false;
                }
                if (n.y != this.y) {
                    //System.out.println("FAIL y");
                    return false;
                }
                if (n.weight != this.weight) {
                    //System.out.println("FAIL weogjt");
                    return false;
                }
                if (n.pinned != this.pinned) {
                    //System.out.println("FAIL pinned");
                    return false;
                }
                if (n.edgeList.size() != this.edgeList.size()) {
                    //System.out.println("FAIL edge");
                    return false;
                }
                if (n.getSearch() != null && getSearch() != null) {
                    if (!n.getSearch().equals(this.getSearch())) {
                        //System.out.println("FAIL doc");
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
