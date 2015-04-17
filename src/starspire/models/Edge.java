package starspire.models;

import org.json.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * An edge represents a link between 2 nodes.
 * A node cannot be linked to itself. Also edges are considered unidirectional:
 *      Edge( n1, n2 ) == Edge( n2, n1 )
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class Edge {

    /**
     *
     */
    public final static double DEFAULT_STRENGTH = 1.0;
    /**
     *
     */
    public final static Color DEFAULT_COLOR = Color.black;
    private static int SERIAL_ID = 0;
    private int ID;
    private double strength;
    private Node node1, node2;
    private Color edgeColor;
    private double tension;
    private ArrayList<Entity> entities;

    /**
     * Default constructor with 2 edges. Sets strength to 1.
     * Node order doesn't matter.
     * @precondition n1 != n2
     * @param n1 first node
     * @param n2 second node
     */
    public Edge(Node n1, Node n2) {
        setup(++SERIAL_ID, DEFAULT_STRENGTH, n1, n2);
    }

    /**
     * Advanced constructor, also takes strength.
     * @precondition n1 != n2
     * @precondition str > 0.0
     * @param str edge Strength
     * @param n1 first node
     * @param n2 second node
     */
    public Edge(Node n1, Node n2, double str) {
        setup(++SERIAL_ID, str, n1, n2);
    }

    /**
     * Construct Edge from JSON object.
     * Nodes still passed as parameters since edge has no way to look them up.
     *
     * Then hacks the SERIAL_ID to make sure it works after loading by using
     * max number.
     * @param edge Edge as a JSON object
     * @param n1 Node to link to this edge
     * @param n2 other ndoe to link to this edge
     * @throws JSONException JSON object not in the correct format
     */
    public Edge(JSONObject edge, Node n1, Node n2) throws JSONException {
        int jid = edge.getInt("ID");
        setup(jid, edge.getDouble("strength"), n1, n2);
        if (jid >= SERIAL_ID) {
            SERIAL_ID = jid + 1;
        }
        //System.out.println("Edge Loaded ("+ toString() +")");
    }

    /**
     * Setup helper
     * @param str Node Strength
     * @param n1 first node
     * @param n2 second node
     */
    private void setup(int nID, double str, Node n1, Node n2) {
        assert (str > 0);
        assert (n1 != n2);
        ID = nID;
        strength = str;
        node1 = n1;
        node2 = n2;
        edgeColor = DEFAULT_COLOR;
        entities = new ArrayList<Entity>();
    }

    /**
     * This returns a JSON object representation of an edge.
     * Note: it can't store it's nodes so it'll just store their IDs.
     * @return a JSONObject representing this edge's data
     * @throws JSONException something went wrong
     */
    public JSONObject getJSONObject() throws JSONException {
        JSONObject edge = new JSONObject();

        edge.put("ID", ID);
        edge.put("node1", node1.getID());
        edge.put("node2", node2.getID());
        edge.put("strength", strength);
        JSONArray entIds = new JSONArray();
        for (Entity e : entities) {
            entIds.put(e.getID());
        }
        edge.put("entIds", entIds);

        return edge;
    }

    /**
     * Returns the unique id of this edge.
     * @return edge id
     */
    public int getID() {
        return ID;
    }

    /**
     * Returns edge strength.
     * @return edge strength
     */
    public double getStrength() {
        return strength;
    }

    /**
     * returns the first node
     * @return first node
     */
    public Node getNode1() {
        return node1;
    }

    /**
     * returns the second node
     * @return second node
     */
    public Node getNode2() {
        return node2;
    }

    /**
     * Returns the tension on the edge (i.e. spring)
     * @return double tension
     */
    public double getTension() {
        return tension;
    }

    /**
     * Set the tension on the edge (i.e. spring)
     * This is not the strength of the edge!
     * @param t The new tension
     */
    protected void setTension(double t) {
        tension = t;
    }

    /**
     * Sets the color of the edge.
     * @param color the new Color of the edge
     */
    protected void setColor(Color color) {
        edgeColor = color;
    }

    /**
     * Returns the current color of the edge.
     * @return Color The color of the edge.
     */
    protected Color getColor() {
        return edgeColor;
    }

    /**
     * Check whether this edge contains the given node.
     * @param n node to check
     * @return true if either node1 or node2 is equal to n false otherwise.
     */
    protected boolean contains(Node n) {
        return (n == node1 || n == node2);
    }

    /**
     * Checks if this edge links the 2 given node, regardless of order.
     * @param n1 first node
     * @param n2 second node
     * @return true if the edge links these 2 nodes together, false otherwise.
     */
    public boolean links(Node n1, Node n2) {
        return ((n1.equals(node1) && n2.equals(node2))
                || (n1.equals(node2) && n2.equals(node1)));
    }

    /**
     * Changes the edge strength.
     * @precondition str > 0
     * @param str new edge strength.
     */
    protected void setStrength(double str) {
        assert (str > 0);
        strength = str;
    }

    /**
     * This adds the entity e to this edge if it doesn't already exist.
     * @param e Entity to add to this edge.
     */
    protected void addEntity(Entity e) {
        if (hasEntity(e)) {
            return;
        }
        entities.add(e);

        this.calculateEdgeStrength();
    }


    /**
     * Returns a count of the entities in this edge.
     * @return sum of entities this edge contains.
     */
    public int numOfEntities() {
        return entities.size();
    }


    /**
     * Removes an entity from this edge.
     * Recalculates edge strength afterwards
     * @param e entity to remove if it's contained.
     */
    protected void removeEntity(Entity e) {
        int index = 0;
        int count = 0;

        if (hasEntity(e)) {
            entities.remove(entities.indexOf(e));
        }

        this.calculateEdgeStrength();
    }

    /**
     * Checks if the given entity is one of this edge's entities.
     * @param e Entity to check
     * @return true if edge contains e
     */
    public boolean hasEntity(Entity e) {
        for (Entity current : entities) {
            if (current.equals(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the edge strength based on:
     * 1) The number of entities in the edge
     * 2) The weight of each entity in the edge
     */
    public void calculateEdgeStrength() {
        double newStrength = 0;

        for(Entity e : entities) {
            newStrength += e.getStrength();
        }

        strength = newStrength;
    }

    /**
     * Returns string representation of edge including the 2 nodes it links
     * @return string representation of edge
     */
    @Override
    public String toString() {
        return "Edge " + ID + " str: " + strength + "( N" + node1.getID() + ", N" + node2.getID() + ")";
    }

    /**
     * This returns an array of the entities. It uses the toString() on entites.
     * @return Array of entity strings
     */
    public String[] entitiesToString() {
        String[] list = new String[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            list[i] = entities.get(i).getName();
        }
        return list;
    }

    /**
     * This returns an array of the entities. It uses the toString() on entites.
     * The list is sorted by the entity strengths
     * @return Array of entity strings, sorted
     */
    public String[] entitiesToStringSorted() {
        ArrayList<Entity> list = new ArrayList();
        for (int i = 0; i < entities.size(); i++) {
            list.add(entities.get(i));
        }

        //sort the array by the strength of the entities
        Collections.sort(list, new Comparator<Entity>() {

                    public int compare(Entity a, Entity b) {
                        return Double.compare(b.getStrength(), a.getStrength());
                    }
        });
        String[] sortedList = new String[entities.size()];
        for (int i = 0; i < entities.size(); i++) {
            sortedList[i] = list.get(i).getName();
        }
        return sortedList;
    }

    /**
     * Overrides Equals behavior
     * @param o
     * @return true if they link the same nodes
     */
    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o.getClass() == this.getClass()) {
                Edge e = (Edge) o;
                return e != null && e.links(node1, node2);
            }
        }
        return false;
    }
}
