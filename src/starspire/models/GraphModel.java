package starspire.models;

import starspire.StarSpireApp;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.*;

/**
 * Model for the graph, holds nodes and edges.
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class GraphModel {

    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());
    private static final double JITTER_ADJUSTMENT = 0.85;
    private static final double JITTER_ADJUSTMENT_OPEN = 1.25;

    /**
     * This enumeration represents a set of places where the nodes can be created.
     * i.e. CENTER will place a node in the current center of the graph,
     * RANDOM will place it at a random position in the graph.
     */
    public static enum Position {

        CENTER, RANDOM //,TOP_LEFT // could add more if data available, mouse loc and ???
    }

    private static enum EventType {

        ADDED, MODIFIED, MOVED, SELECTED, REMOVED, OPENED, CLOSED
    };
    private static final int COLLISION_MARGIN = 2;
    private ArrayList<Node> nodes;
    private ArrayList<Edge> edges;
    private Node selectedNode;
    private ArrayList<Node> selectedNodes;
    private Node containNodeCache;
    private Edge containEdgeCache;
    private ArrayList<GraphListener> listeners;
    private Dimension graphSize;

    /**
     * Default Constructor.
     */
    public GraphModel() {
        nodes = new ArrayList<Node>();
        edges = new ArrayList<Edge>();
        listeners = new ArrayList<GraphListener>();
    }

    /**
     * Construct graph from loaded file.
     * @param graph JSON object containing nodes and edges arrays
     * @param data
     * @throws JSONException JSON object not in the correct format.
     */
    public GraphModel(JSONObject graph, DataModel data) throws JSONException {
        JSONArray jsonNodes, jsonEdges;

        nodes = new ArrayList<Node>();
        edges = new ArrayList<Edge>();
        listeners = new ArrayList<GraphListener>();

        jsonNodes = graph.getJSONArray("Nodes");
        jsonEdges = graph.getJSONArray("Edges");

        /*
         * Load nodes by simply looping through array
         */
        //System.out.println(jsonNodes.toString(4));
        for (int i = 0; i < jsonNodes.length(); i++) {
            JSONObject node;
            node = jsonNodes.getJSONObject(i);
            //System.out.println("Loading node: " + node);
            String nodeClass = node.getString("class");
            if (nodeClass.equals(DocumentNode.class.getName())) {
                nodes.add(new DocumentNode(node, data));
            } else if (nodeClass.equals(SearchNode.class.getName())) {
                nodes.add(new SearchNode(node, data));
            } else {
                throw new JSONException("Unknown node type.");
            }
        }

        /*
         * Load edges...
         * for each edge, get node ids
         * look up node 1 and 2,
         * pass nodes and JSON obj to constructor
         * last add edge to nodes 1 and 2 as well.
         */
        for (int i = 0; i < jsonEdges.length(); i++) {
            JSONObject edge;
            edge = jsonEdges.getJSONObject(i);
            Node n1 = lookUpNode(edge.getInt("node1"));
            Node n2 = lookUpNode(edge.getInt("node2"));
            Edge e = new Edge(edge, n1, n2);
            edges.add(e);
            /*
             * For each entid in entarr
             *      lookup ent
             *      add ent to edge
             */
            JSONArray entIds = edge.getJSONArray("entIds");
            for (int d = 0; d < entIds.length(); d++) {
                int id = entIds.getInt(d);
                Entity ent = data.lookUpEntity(id);
                if (ent == null) {
                    System.err.println("Loading Exception: Entity with id " + id + " not found!");
                } else {
                    e.addEntity(ent);
                }
            }
            n1.addEdge(e);
            n2.addEdge(e);
        }
    }

    /**
     * lookUpNode is a helper for creating edges from JSONObjects,
     * the edge has the node id but no way to look it up.
     * This loops through nodes and returns the node with the given id.
     * @param id ID of node to find in the model.
     * @return null if node not found, if found returns the node.
     */
    public synchronized Node lookUpNode(int id) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n.getID() == id) {
                return n;
            }
        }
        return null;
    }

    /**
     * Add a new node to the graph
     * @param x x location
     * @param y y location
     * @return newly added node
     */
    /*public synchronized Node addNode(int x, int y) {
    Node n = new Node(x, y);
    nodes.add(n);
    fireNodeChange(n, EventType.ADDED);
    return n;
    }*/
    /**
     * Add a new node to the graph
     * @param x x location
     * @param y y location
     * @param doc document this node links to
     * @return newly added node
     */
    public synchronized Node addNode(int x, int y, Document doc) {
        Node n = new DocumentNode(x, y, doc);
        n.setMSSI(doc.isMSSI());
        nodes.add(n);
        fireNodeChange(n, EventType.ADDED);
        return n;
    }

    /**
     * This adds a new node to the graph model, taking a position.
     * The position specifies where on the layout the node
     * @param p
     * @param doc
     * @return
     */
    public synchronized Node addNode(Position p, Document doc) {
        int x, y;
        switch (p) {
            case RANDOM:
                x = random(0, graphSize.width);
                y = random(0, graphSize.height);
                break;
            case CENTER:
            default:
                x = graphSize.width / 2;
                y = graphSize.height / 2;
        }
        Node n = new DocumentNode(x, y, doc);
        n.setMSSI(doc.isMSSI());
        nodes.add(n);
        fireNodeChange(n, EventType.ADDED);
        return n;
    }

    /**
     * This adds a new node to the graph model, taking a position.
     * The position specifies where on the layout the node
     * @param p Position to place the initial position of the node
     * @param s Search used to make the node
     * @return Node created
     */
    public synchronized Node addNode(Position p, Search s) {
        int x, y;
        switch (p) {
            case RANDOM:
                x = random(0, graphSize.width);
                y = random(0, graphSize.height);
                break;
            case CENTER:
            default:
                x = graphSize.width / 2;
                y = graphSize.height / 2;
        }
        Node n = new SearchNode((int)x,(int)y, s);
        if (s != null) {
            n.highlight = s.getHue();
        }
        nodes.add(n);
        fireNodeChange(n, EventType.ADDED);
        return n;
    }
    
    /**
     * This adds a new node to the graph model, taking a point.
     * The point specifies where on the layout the node
     * @param p Point to place the initial position of the node
     * @param s Search used to make the node
     * @return Node created
     */
    public synchronized Node addNode(Point p, Search s) {
        int x, y;
        
        Node n = new SearchNode((int)p.x,(int)p.y, s);
        if (s != null) {
            n.highlight = s.getHue();
        }
        nodes.add(n);
        fireNodeChange(n, EventType.ADDED);
        return n;
    }

    /**
     * Helper function for generating random numbers,
     * it's useful when randomizing node positions.
     * @param min Lower bound
     * @param max Upper bound
     * @return returns an integer between (inclusive) min and max.
     */
    private int random(int min, int max) {
        return min + (int) ((max - min) * Math.random());
    }

    /**
     * The get edge function returns the edge for 2 given nodes if it exists.
     * It does this regardless of the order of the link between node 1 and 2.
     * @param n1 First node
     * @param n2 Second node
     * @return Edge containing n1 and n2 or null if there is no such edge.
     */
    public synchronized Edge getEdge(Node n1, Node n2) {
        for (Edge e : edges) {
            if (e.links(n1, n2)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Add a new edge to the graph
     * @param n1 node 1
     * @param n2 node 2
     * @return newly added edge or already existing edge?? plus strength?
     */
    public synchronized Edge addEdge(Node n1, Node n2) {
        if (linked(n1, n2)) {
            //an edge already exists, return it
            Edge e = getEdge(n1, n2);
            //double newStrength = e.getStrength() + 0.05;
            //e.setStrength(newStrength);
            //System.out.println("New strength for " + e.toString() + ", " +newStrength);
            return e;
        } else {
            Edge e = new Edge(n1, n2);
            //add the edge to the edgeList of Node
            n1.addEdge(e);
            n2.addEdge(e);
            edges.add(e);
            fireEdgeChange(e, EventType.ADDED);
            //System.out.println("adding edge: " + e.toString());
            return e;
        }
    }

    /**
     * This function adds an entity to an existing edge.
     * This will fire the changes that will in turn fix the layout or whatever.
     * Entities should be added to Edges through here not through the edge itself
     * @param e Edge to update
     * @param ent Entity to add to edge
     */
    public synchronized void addEdgeEntity(Edge e, Entity ent) {
        fireEdgeChange(e, EventType.MODIFIED);
        e.addEntity(ent);
        fireEdgeChange(e, EventType.MODIFIED);
    }

    /**
     * This function removes the entity from the edge's entity list.
     * This fires the events for the change.
     * @param e Edge to be updated
     * @param ent Entity to be removed
     */
    public synchronized void removeEdgeEntity(Edge e, Entity ent) {
        if (e.hasEntity(ent)) {
            fireEdgeChange(e, EventType.MODIFIED);
            e.removeEntity(ent);
            fireEdgeChange(e, EventType.MODIFIED);
        }

        //check if no more entities are left in the edge
        //if so, remove the edge
        if (e.numOfEntities() < 1) {
            removeEdge(e);
        }
    }

    /**
     * Total number of nodes
     * @return Total number of nodes
     */
    public synchronized int nodeCount() {
        return nodes.size();
    }

    /**
     * Total number of edges
     * @return Total number of edges
     */
    public synchronized int edgeCount() {
        return edges.size();
    }

    /**
     * Sets the given node as selected.
     * @param n Node to set as selected.
     */
    public synchronized void setSelected(Node n) {
        Node old = selectedNode;
        if (n != old) {
            selectedNode = n;
            if (n != null) {
                // Remeber the position of the last selection.
                n.selectionX = n.x;
                n.selectionY = n.y;
                fireNodeChange(n, EventType.SELECTED);
                
                //selectedNodes.add(n); //multiple selected nodes. How to deselect?
            }
        }
        
        
        //System.out.println("Node selected: " + n.getID() + " (linked to doc: " + n.getDocument().getName()+ ")");
    }

    /**
     * Resizes a node, fires an event before and after so that we can
     * have both the previous and next size if needed...
     *
     * Also note set size is based on the top left corner of the node!
     * @param n Node to resize
     * @param d New Dimensions
     */
    public synchronized void setNodeSize(Node n, Dimension d) {
        //up date the center based coordinates.
        int x = n.getX() + (d.width / 2 - n.getWidth() / 2);
        int y = n.getY() + (d.height / 2 - n.getHeight() / 2);
        n.setXY(x, y);
        n.setSize(d);
        fireNodeModified(n, GraphListener.NodeModType.OTHER);
    }
    
    public synchronized void setNodeClosedSize(Node n, Dimension d) {
        n.setClosedSize(d);
        fireNodeModified(n, GraphListener.NodeModType.OTHER);

    }

    /**
     * Sets the open/close state of the node.
     * True = open, False = close
     * @param n Node that will change state
     * @param b Boolean True = open, False = closed
     */
    public synchronized void setNodeOpen(Node n, boolean b) {
        n.setOpen(b);
        if (b) {
            fireNodeChange(n, EventType.OPENED);
        } else {
            fireNodeChange(n, EventType.CLOSED);
        }
    }

    public synchronized void setNodeSearch(SearchNode n, Search s) {
        n.setSearch(s);
        fireNodeModified(n, GraphListener.NodeModType.OTHER);
    }

    /**
     * Return selected node
     * TODO: make this return a list for when multiple nodes can be selected
     * @return returns selected node or null if there is none.
     */
    public synchronized Node getSelected() {
        return selectedNode;
    }
    
    public synchronized ArrayList<Node> getSelectedNodes() {
        return selectedNodes;
    }

    /**
     * Return pinned nodes
     * @return returns list of pinned nodes or null if there are none
     */
    public synchronized ArrayList<Node> getPinned() {
        ArrayList<Node> pinnedNodes = new ArrayList<Node>();

        for (Node n : nodes) {
            if (n.isPinned()) {
                pinnedNodes.add(n);
            }
        }

        if (pinnedNodes.isEmpty()) {
            return null;
        } else {
            return pinnedNodes;
        }
    }

    /**
     * Checks if a given node is part of the current selection.
     * @param n node
     * @return true if node is selected false otherwise
     */
    public synchronized boolean isSelected(Node n) {
        return selectedNode == n;
    }

    /**
     * Move node to specified coordinates.
     * @param n node to move
     * @param x x to move to
     * @param y y to move to
     * @precondition != null
     */
    public synchronized void moveNode(Node n, int x, int y) {
        assert (n != null);
        /*
         * Do nothing if node hasn't moved
         */
        if (n.x == x && n.y == y) {
            return;
        }
        //fireNodeChange(n, EventType.MOVED);
        Point newLoc = new Point(x, y);
        //checkJitterMagic(n, newLoc);
        checkNodesCollision(n, newLoc);
        checkFrameCollision(n, newLoc);
        n.setXY(newLoc.x, newLoc.y);
        fireNodeChange(n, EventType.MOVED);
    }

    /**
     * Update the search of a node
     * @param n node
     * @param s new search
     */
    public synchronized void changeNodeSearch(SearchNode n, Search s) {
        n.search = s;
        n.setHighlight(s.getHue());
        fireNodeModified(n, GraphListener.NodeModType.OTHER);
    }

    /**
     * A magician never reveals his tricks!
     *
     * @param n Node who might be a jitter offender
     * @param newLoc the possible offenders next target
     */
    private void checkJitterMagic(Node n, Point newLoc) {
        // get delta of previous move:
        int dx = n.x - n.px;
        int dy = n.y - n.py;
        // get the magnitue of previous and current move
        double delta = Math.sqrt(Math.pow(dx + n.vx, 2) + Math.pow(dy + n.vy, 2));
        // get the velocity magnitude threshold:
        double vthreshold = Math.sqrt(Math.pow(n.vx, 2) + Math.pow(n.vy, 2))
                * (n.isOpen ? JITTER_ADJUSTMENT_OPEN : JITTER_ADJUSTMENT);

        /*
         * Check if it will move a minimum away from previous move and thus
         * avoid the almighty jittering.
         */
        if (delta < vthreshold) {
            //System.out.println("Jitter Offender:" + n);
            //System.out.println("dx:" + dx + " dy:" + dy + " delta:" + delta + " vt:" + vthreshold);
            newLoc.x = n.x;
            newLoc.y = n.y;
            n.vx = 0;
            n.vy = 0;
        }

    }

    /**
     * Make sure node doesn't move out of frame...
     * @param n Node to check collision for
     * @param newLoc desired location
     */
    private void checkFrameCollision(Node n, Point newLoc) {
        //cap on frame size in the x
        if (newLoc.x > (graphSize.width - n.getWidth() / 2)) {
            newLoc.x = graphSize.width - n.getWidth() / 2;
            n.setVX(0);
        }
        if (newLoc.x < n.getWidth() / 2) {
            newLoc.x = n.getWidth() / 2;
            n.setVX(0);
        }
        //cap on frame size in the y
        if (newLoc.y > (graphSize.height - n.getHeight() / 2)) {
            newLoc.y = (graphSize.height - n.getHeight() / 2);
            n.setVY(0);
        }
        if (newLoc.y < n.getHeight() / 2) {
            newLoc.y = n.getHeight() / 2;
            n.setVY(0);
        }
    }

    /**
     * Checks for collisions between nodes
     * Big O (n)
     * @param n Node we want to fix
     * @param newLoc current desired location.
     */
    private void checkNodesCollision(Node current, Point newLoc) {
        //don't collide a pinned and a selected node.
        if (current.isPinned() || isSelected(current)) {
            return;
        }

        int cTop = newLoc.y - current.getHeight() / 2;
        int cBottom = newLoc.y + current.getHeight() / 2;
        int cLeft = newLoc.x - current.getWidth() / 2;
        int cRight = newLoc.x + current.getWidth() / 2;
        int nTop, nBottom, nLeft, nRight;
        boolean collision;

        for (Node n : nodes) {
            if (!current.equals(n)) {
                collision = true;

                if (isSelected(current)) {
                    collision = false; //avoid collisions with selected node
                }

                if (!(n.isOpen() || current.isOpen())) {
                    collision = false; //if neither is open don't bother with collision.
                }

                nTop = n.getY() - n.getHeight() / 2 - COLLISION_MARGIN;
                nBottom = n.getY() + n.getHeight() / 2 + COLLISION_MARGIN;
                nLeft = n.getX() - n.getWidth() / 2 - COLLISION_MARGIN;
                nRight = n.getX() + n.getWidth() / 2 + COLLISION_MARGIN;

                //check for no collision casses first:
                if (cTop > nBottom) {
                    collision = false;
                } else if (nTop > cBottom) {
                    collision = false;
                } else if (cRight < nLeft) {
                    collision = false;
                } else if (nRight < cLeft) {
                    collision = false;
                }

                //TODO add a threshold and move node around if needed... so that
                //it doesn't get stuff if it really wants to get through
                if (collision) {
                    int xdiff = Math.abs(newLoc.x - n.getX());
                    int ydiff = Math.abs(newLoc.y - n.getY());
                    if (xdiff > ydiff) {
                        //avoid collision by moving in the x
                        newLoc.y += current.getVX();
                        current.setVX(0.0);
                        if (newLoc.x < n.getX()) {
                            //move to the left
                            newLoc.x = nLeft - current.getWidth() / 2 - COLLISION_MARGIN;
                        } else {
                            //move to the right
                            newLoc.x = nRight + current.getWidth() / 2 + COLLISION_MARGIN;
                        }
                    } else {
                        //avoid collision by moving in the y
                        newLoc.x += current.getVY();
                        current.setVY(0.0);
                        if (newLoc.y < n.getY()) {
                            //move to the top
                            newLoc.y = nTop - current.getHeight() / 2 - COLLISION_MARGIN;
                        } else {
                            //move to the bottom
                            newLoc.y = nBottom + current.getHeight() / 2 + COLLISION_MARGIN;
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets a node to pinned or not pinned and fires the needed events.
     * @param n Node to modify
     * @param pin New pinned status
     */
    public synchronized void pinNode(Node n, boolean pin) {
        assert (n != null);
        n.setPinned(pin);
        fireNodeModified(n, GraphListener.NodeModType.PIN);
    }

    /**
     * Sets a new weight fires the needed events.
     * @param n Node to modify
     * @param weight New weight
     */
    public synchronized void setNodeWeight(Node n, double weight) {
        assert (n != null);
        n.setWeight(weight);
        fireNodeModified(n, GraphListener.NodeModType.WEIGHT);
    }

    /**
     * Checks if the given node is contained in this graph.
     * @param n node to check
     * @return true if node is in the graph false otherwise
     */
    public synchronized boolean contains(Node n) {
        if (n == null) {
            return false;
        } else {
            /* check cache */
            if (n == containNodeCache) {
                return true;
            } else {
                if (nodes.contains(n)) {
                    /* this node is contained, cache result */
                    containNodeCache = n;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Update the size of the graph. This will affect the layout.
     * @param d New graph dimensions...
     */
    public void setGraphSize(Dimension d) {
        System.out.println("updating graph size to : " + d);
        graphSize = new Dimension(d.width, d.height);
        for (GraphListener g : listeners) {
            g.graphResized(graphSize);
        }
    }

    /**
     * Checks if the given edge is in the graph, false otherwise.
     * @param e edge to check
     * @return true if e is in the graph false if not.
     */
    public synchronized boolean contains(Edge e) {
        if (e == null) {
            return false;
        } else {
            /* check cache */
            if (e == containEdgeCache) {
                return true;
            } else {
                if (edges.contains(e)) {
                    /* this edge is contained, cache result */
                    containEdgeCache = e;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Finds out if the two given node are liked by an edge.
     * @param n1 Node 1
     * @param n2 Node 2
     * @return true if there's an edge from node 1 to node 2 regardless of order, false otherwise.
     */
    public synchronized boolean linked(Node n1, Node n2) {
        for (Edge e : edges) {
            if (e.links(n1, n2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a node from the graph.
     * @precondition node n is contained in the graph
     * @param n node to remove
     */
    public synchronized void removeNode(Node n) {
        assert (contains(n));
        nodes.remove(n);
        containNodeCache = null; //removed from cache
        /* now remove any edges that contain this node */
        Iterator<Edge> it = edgeIterator();
        while (it.hasNext()) {
            Edge e = it.next();
            if (e.contains(n)) {
                it.remove();
                fireEdgeChange(e, EventType.REMOVED);
                if (containEdgeCache == e) {
                    containEdgeCache = null;
                }
            }
        }
        System.out.println("about to fire event");
        fireNodeChange(n, EventType.REMOVED);
    }

    /**
     * Remove node helper that takes in a document.
     * It first looks up that document's corresponding node, then
     * remove that node.
     * @param d Document to match with a node to remove
     */
    public synchronized void removeNode(Document d) {
        Node n = null;
        /*
         * Look up node
         */
        for (Node current : nodes) {
            if (current instanceof DocumentNode && ((DocumentNode) current).getDocument().equals(d)) {
                n = current;
                break;
            }
        }
        /*
         * Then if found remove it (can't remove it in loop it would cause concurent modif exception)
         */
        if (n != null) {
            removeNode(n);
        } else {
            logger.log(Level.WARNING, "Unable to remove documentNode: no match", d);
        }

    }

    /**
     * Remove node helper that takes in a search.
     * It first looks up that search 's corresponding node, then
     * remove that node.
     * @param s Search to match with a node to remove
     */
    public synchronized void removeNode(Search s) {
        assert (s != null); //would be bad!
        Node n = null;
        /*
         * Look up node
         */
        for (Node i : nodes) {
            if (i instanceof SearchNode && s.equals(((SearchNode) i).getSearch())) {
                n = i;
                break;
            }
        }
        /*
         * Then if found remove it (can't remove it in loop it would cause concurent modif exception)
         */
        if (n != null) {
            removeNode(n);
        } else {
            logger.log(Level.WARNING, "Unable to remove searchNode: no match", s);
        }
    }

    /**
     * This forces an edge to recalculate it's strength. Usually should
     * be called if the documents it points to or entities were updated.
     * @param e Edge to update.
     */
    public synchronized void updateEdge(Edge e) {
        e.calculateEdgeStrength();
        fireEdgeChange(e, EventType.MODIFIED);
    }

    /**
     * Remove the given edge from the graph
     * @precondition edge is contained in the graph
     * @param e edge to remove
     */
    public synchronized void removeEdge(Edge e) {
        assert (contains(e));
        edges.remove(e);
        //remove edge from each edgeList of Node that contains it
        Iterator<Node> it = nodeIterator();
        while (it.hasNext()) {
            Node n = it.next();
            n.removeEdge(e);
        }
        containEdgeCache = null;
        fireEdgeChange(e, EventType.REMOVED);
    }

    /**
     * Gets an iterator for the nodes.
     * @return node iterator
     */
    public synchronized Iterator<Node> nodeIterator() {
        return nodes.iterator();
    }

    /**
     * Gets and iterator for the edges in the graph.
     * @return edge iterator
     */
    public synchronized Iterator<Edge> edgeIterator() {
        return edges.iterator();
    }

    /**
     * Changes the strength of a given edge
     * @param e edge to update
     * @param str new strength value
     */
    public synchronized void changeEdgeStrength(Edge e, double str) {
        fireEdgeChange(e, EventType.MODIFIED);
        e.setStrength(str);
        fireEdgeChange(e, EventType.MODIFIED);
    }

    /**
     * Registers a new listener for this graph.
     * @param g listener to add
     * @precondition g != null
     */
    public synchronized void addListener(GraphListener g) {
        assert (g != null);
        listeners.add(g);
    }

    /**
     * Removes a registered listener for this graph.
     * @param g listener to remove
     * @precondition g != null
     * @precondition g is already a listener
     */
    public synchronized void removeListener(GraphListener g) {
        assert (g != null);
        listeners.remove(g);
    }

    /**
     * Fire a node change of the specified type to all the listeners.
     * @param n node that changed
     * @param t type of change
     */
    private void fireNodeChange(Node n, EventType t) {
        for (GraphListener g : listeners) {
            switch (t) {
                case ADDED:
                    g.nodeAdded(n);
                    break;
                case REMOVED:
                    g.nodeRemoved(n);
                    break;
                case MOVED:
                    g.nodeMoved(n);
                    break;
                case SELECTED:
                    g.nodeSelected(n);
                    break;
                case OPENED:
                    g.nodeOpened(n);
                    break;
                case CLOSED:
                    g.nodeClosed(n);
                    break;
                default:
                    g.nodeModified(n, GraphListener.NodeModType.OTHER);
                    break;
            }
        }
    }

    /**
     * A node was modified
     * @param n
     * @param t
     */
    private void fireNodeModified(Node n, GraphListener.NodeModType t) {
        for (GraphListener g : listeners) {
            g.nodeModified(n, GraphListener.NodeModType.OTHER);
            
        }
    }

    /**
     * Fire an edge change of the specified type to all the listeners.
     * @param e edge that changed
     * @param t type of change
     */
    private void fireEdgeChange(Edge e, EventType t) {
        for (GraphListener g : listeners) {
            switch (t) {
                case ADDED:
                    g.edgeAdded(e);
                    break;
                case REMOVED:
                    g.edgeRemoved(e);
                    break;
                default:
                    g.edgeModified(e);
                    break;
            }
        }
    }
}
