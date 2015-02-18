package starspire.models;

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
 * @author Patrick Fiaux, Alex Endert
 */
public class DocumentNode extends Node {

    private static final double DEFAULT_DIVIDER_LOCATION = 0.5;

    private Document doc;
    private double dividerLoc;


    /**
     * Default Constructor
     * @param doc Document to link to this node
     */
    public DocumentNode(Document doc) {
        super(0,0);
        setup(DEFAULT_DIVIDER_LOCATION, doc);
    }

    /**
     * Document Constructor
     * @param nX x location of node
     * @param nY y location of node
     * @param doc Document to link to this node
     */
    public DocumentNode(int nX, int nY, Document doc) {
        super(nX, nY);
        setup(DEFAULT_DIVIDER_LOCATION, doc);
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
    public DocumentNode(JSONObject node, DataModel model) throws JSONException {
        super(node);
        double divider;
        int did;
        Document d;
        divider = node.getDouble("divider");

        //Load/Link Document
        did = node.getInt("docID");
        d = model.lookUpDocument(did);
        if (d == null) {
            model.addDocument("Document Id: " + did + "Could not be linked");
        }

        setup(divider, d);
    }

    /**
     * Setup constructor helper.
     * @param nID id of the node
     * @param nX x location
     * @param nY y location
     * @param nW width
     * @param nH height
     * @param nDLoc new divider location % (between 0 and 1)
     * @param wght Weight of that node (how much it repels other nodes
     * @param pinned true if this node is pinned (shouldn't be affected by layout) false otherwise.
     */
    private void setup(double nDLoc, Document d) {
        dividerLoc = nDLoc;

        /* Document Ties */
        doc = d;
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
        nodeJSON.put("divider", dividerLoc);
        nodeJSON.put("docID", doc.getId());

        return nodeJSON;
    }


    /**
     * Return the preferred divider location as a percentage
     * @return
     */
    public double getDividerPosition() {
        return dividerLoc;
    }

    /**
     * Return the document associated with this node
     * @return document linked to this node
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Implements to string, shows node id and location.
     * @return string representation of node.
     */
    @Override
    public String toString() {
        return "Node " + ID + " x: " + x + " y: " + y + " weight: " + weight + (pinned ? " pinned" : " not pinned");
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
                DocumentNode n = (DocumentNode) o;
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
                if (n.getDocument() != null && getDocument() != null) {
                    if (!n.getDocument().equals(this.getDocument())) {
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