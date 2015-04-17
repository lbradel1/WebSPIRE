package starspire.models;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
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
public abstract class Node {

    protected int CLOSE_WIDTH = 15;
    protected int CLOSE_HEIGHT = 15;
    /**
     * Minimum Open node width.
     */
    public static final int OPEN_MIN_WIDTH = 270;
    /**
     * Minimum Open node height.
     */
    public static final int OPEN_MIN_HEIGHT = 300;

    protected static enum EventType {

        OPENED, CLOSED, MOVED, MODIFIED
    };
    protected final static double DEFAULT_WEIGHT = 1.0;
    protected static int SERIAL_ID = 0;
    protected boolean pinned;
    protected int ID;
    protected double accelx;
    protected double accely;
    protected int x;
    protected int y;
    protected int px; //previous x
    protected int py; //previous y
    protected int w;
    protected int h;
    protected double vx;
    protected double vy;
    protected ArrayList<Edge> edgeList;
    protected double weight;

    protected int selectionX; // Set to the location when a node is selected
    protected int selectionY; // allows to move back after link drag

    /**
     * tells us if node is open(true) or closed(false)
     */
    protected boolean isOpen;
    protected int highlight = 0;
    
    private boolean isVisible;
    private int rank;
    private int quartile;
    private boolean isMSSI = false;
    private boolean wasOpened = false;
    private int recency = 0;

    /**
     * Document Constructor
     * @param nX x location of node
     * @param nY y location of node
     * @param doc Document to link to this node
     */
    public Node(int nX, int nY) {
        setup(++SERIAL_ID, nX, nY, OPEN_MIN_WIDTH, OPEN_MIN_HEIGHT, DEFAULT_WEIGHT, false, false);
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
    public Node(JSONObject node) throws JSONException {
        int lid, lx, ly, did, width, height;
        double lw, divider;
        boolean p, open;
        Document d;
        lid = node.getInt("ID");
        lx = node.getInt("X");
        ly = node.getInt("Y");
        lw = node.getDouble("weight");
        p = node.getBoolean("pinned");
        open = node.getBoolean("open");
        //open = true;    //TODO always open!
        width = node.getInt("W");
        height = node.getInt("H");


        setup(lid, lx, ly, width, height, lw, p, open);

        if (lid >= SERIAL_ID) {
            SERIAL_ID = lid + 1;
        }
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
    private void setup(int nID, int nX, int nY, int nW, int nH, double wght, boolean pin, boolean open) {
        ID = nID;
        x = nX;
        y = nY;
        px = x;
        py = y;
        selectionX = x;
        selectionY = y;
        vx = vy = 0;
        w = Math.max(nW, OPEN_MIN_WIDTH);
        h = Math.max(nH, OPEN_MIN_HEIGHT);
        pinned = pin;
        isOpen = open;
        edgeList = new ArrayList<Edge>();
        weight = wght;   //default = 1.0  >1 = heavy <1 = light

        highlight = 0;  //default highlight state
        isVisible = true;
        rank = 1;
    }
    
    public boolean isMSSI() {
        return isMSSI;
    }
    
    public void setMSSI(boolean b) {
        isMSSI = b;
    }
    
    /**
     * This returns a JSON object representation of a node
     * @return a JSONObject representing this node's data
     * @throws JSONException something went wrong
     */
    public JSONObject getJSONObject() throws JSONException {
        JSONObject nodeJSON = new JSONObject();

        nodeJSON.put("ID", ID);
        nodeJSON.put("X", x);
        nodeJSON.put("Y", y);
        nodeJSON.put("W", w);
        nodeJSON.put("H", h);
        nodeJSON.put("weight", weight);
        nodeJSON.put("pinned", pinned);
        nodeJSON.put("open", isOpen);

        return nodeJSON;
    }

    /**
     * Return the weight of the Node.
     * @return double weight
     */
    public double getWeight() {
        return this.weight;
    }

    /**
     * Set the weight of the Node.
     * @param newWeight
     */
    protected void setWeight(double newWeight) {
        this.weight = newWeight;
        //fireNodeEvent(this, EventType.MODIFIED);
    }

    /**
     * Return the edgeList for this node.
     * @return ArrayList<Edge> edgeList
     */
    public ArrayList<Edge> getEdgeList() {
        return this.edgeList;
    }

    /**
     * Adds an edge to the edgeList for this node.
     * @param newEdge
     */
    protected void addEdge(Edge newEdge) {
        boolean edgeExists;

        //Check to make sure edge does not exist already
        edgeExists = false;
        for (Edge e : this.edgeList) {
            if (newEdge.links(e.getNode1(), e.getNode2())) {
                edgeExists = true;
                break;
            } else {
                //do nothing?
            }
        }
        if (!edgeExists) {
            this.edgeList.add(newEdge);
        } else {
            System.err.println(newEdge.toString() + " already exists. No edge added.");
        }

    }

    /**
     * Returns the state of the Node
     * True = open, false = closed
     * @return Boolean state of node
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Sets the state open/closed state of this node.
     * @param state Boolean true = open, false = closed
     */
    protected void setOpen(boolean state) {
        isOpen = state;
        wasOpened = true;
        if (isOpen) {
            //node is now open, make weight of node bigger
            //TODO: this should be relative to the size, highlight, etc. of the document
            this.weight = this.weight + 2;
        } else {
            //node is now closed, make weight or node smaller
            this.weight = Math.max(this.weight - 2, 1);
        }
        
        if(wasOpened && !state) {
            //downweight entities?
            
        }
    }

    /**
     * Returns the current width of the node
     * @return width in pixels
     */
    public int getWidth() {
        if (isOpen) {
            return w;
        } else {
            return CLOSE_WIDTH;
        }
    }

    /**
     * Returns the current height of the node
     * @return height in pixels
     */
    public int getHeight() {
        if (isOpen) {
            return h;
        } else {
            return CLOSE_HEIGHT;
        }
    }

    /**
     * Returns this node's open width whether it's open or not.
     * @return node's open width
     */
    public int getOpenWidth() {
        return w;
    }

    /**
     * Return this nodes open hight wehther it's open or not
     * @return node's open height
     */
    public int getOpenHeight() {
        return h;
    }

    /**
     * Checks if this node has an edge to the node passed in.
     * @param otherNode
     * @return
     */
    public boolean hasEdgeTo(Node otherNode) {
        boolean isConnected = false;

        for (Edge e : edgeList) {
            if (e.links(this, otherNode)) {
                isConnected = true;
                break;
            }
        }
        return isConnected;
    }

    /**
     * Removes an edge from the edgeList for this node.
     * @param removeThisEdge
     */
    protected void removeEdge(Edge removeThisEdge) {
        Edge remove = null;
        for (Edge e : edgeList) {
            if (e.equals(removeThisEdge)) {
                remove = e;
            } else if (e.equals(removeThisEdge)) {
                remove = e;
            }
        }
        edgeList.remove(remove);
    }

    /**
     * Changes the highlight status of this node
     * ** this is a temp value **
     * it will not be saved
     * @param i Highlight code
     */
    public void setHighlight(int i) {
        highlight = i;
    }

    /**
     * Clears the highlight back to 0.
     */
    public void clearHighlight() {
        highlight = 0;  //default highlight state
    }

    /**
     * Returns the nodes current temp highlight status.
     * @return highlight value (0 means not highlighted)
     */
    public int getHighlight() {
        return highlight;
    }

    /**
     * Returns the unique id of that node
     * @return node id
     */
    public int getID() {
        return ID;
    }
    
    public boolean wasOpened() {
        return wasOpened;
    }

    /**
     * Return node x location
     * @return x location
     */
    public int getX() {
        return x;
    }

    /**
     * Return node y location
     * @return y location
     */
    public int getY() {
        return y;
    }

    /**
     * Set a new x position for node
     * @param newX
     */
    protected void setX(int newX) {
        if (x != newX) {
            px = x;
            x = newX;
        }
    }

    /**
     * Set a new y location for node
     * @param newY
     */
    protected void setY(int newY) {
        if (y != newY) {
            py = y;
            y = newY;
        }
    }
    

    
    protected void resetAcceleration() {
        accelx = 0;
        accely = 0;
    }

    /**
     * Updates the location of the node.
     * @param newX new x location
     * @param newY new y location
     */
    protected void setXY(int newX, int newY) {
        if (y != newY || x != newX) {
            px = x;
            py = y;
            x = newX;
            y = newY;
        }
    }

    /**
     * Resize the node.
     * @precondition new width > OPEN_MIN_WIDTH
     * @precondition new height > OPEN_MIN_HEIGHT
     * @param d new dimension
     */
    protected void setSize(Dimension d) {
        w = Math.max(d.width, OPEN_MIN_WIDTH);
        h = Math.max(d.height, OPEN_MIN_HEIGHT);
    }
    
    protected void setClosedSize(Dimension d) {
        CLOSE_WIDTH = d.width;
        CLOSE_HEIGHT = d.height;
    }

    /**
     * Return node x velocity
     * @return x velocity
     */
    public double getVX() {
        return vx;
    }

    /**
     * Return node y velocity
     * @return y velocity
     */
    public double getVY() {
        return vy;
    }

    /**
     * Set a new x velocity for node
     * @param newVX
     */
    protected void setVX(double newVX) {
        vx = newVX;
    }

    /**
     * Set a new y velocity for node
     * @param newVY
     */
    protected void setVY(double newVY) {
        vy = newVY;
    }

    public Point getSelectionLocation() {
        return new Point(selectionX,selectionY);
    }

    /**
     * This resets the velocity of the node to 0.
     */
    protected void resetVelocity() {
        vy = 0;
        vx = 0;
    }

    /**
     * Returns the pinned value of this node
     * @return true if pinned false otherwise
     */
    public boolean isPinned() {
        return pinned;
    }

    /**
     * Sets whether this node should be pinned or not.
     * @param pin true for pinned false otherwise
     */
    protected void setPinned(boolean pin) {
        pinned = pin;
    }

    /**
     * Used to change the appearance of a node
     * @return 
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Used to change the appearance of a node
     * @param b 
     */
    public void setVisible(boolean b) {
        this.isVisible = b;
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
    
    public void setRecency(int r) {
        recency = r;
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
                Node n = (Node) o;
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
                return true;
            }
        }
        return false;
    }
}
