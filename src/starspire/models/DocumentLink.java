package starspire.models;

import java.util.ArrayList;

/**
 * Document Link represents a symbolic link between 2 nodes or documents.
 * It serves to point out similarities and di-similarities between nodes.
 * If accepted the link will become part of the model by changing the weight of
 * those entities (similar or not similar)
 * @author Patrick Fiaux, Alex Andert
 */
public class DocumentLink {
    private Node node1, node2;
    private boolean accepted;
    private ArrayList<Entity> entitiesUp; //upweight these entities
    private ArrayList<Entity> entitiesDown; //downweight these entities


/**
     * Default constructor, should be used to debug only.
     */
    public DocumentLink() {
        setup(null, null);
        accepted = true;
    }

    /**
     * Base Constructor, takes the 2 nodes this link needs to represent
     * @param n1 Node 1
     * @param n2 Node 2
     */
    public DocumentLink(Node n1, Node n2) {
        setup(n1, n2);
        accepted = true;
    }

    /**
     * Setup is a helper to the constructors...
     * @param n1 node 1
     * @param n2 node 2
     */
    private void setup (Node n1, Node n2) {
        node1 = n1;
        node2 = n2;
        entitiesUp = new ArrayList<Entity>();
        entitiesDown = new ArrayList<Entity>();
    }

    /**
     * Sets up the list of the entities to be up weighted...
     * TODO shouldn't this happen in here?
     * @param entsUp entities list
     */
    public void setEntitiesUpweight(ArrayList<Entity> entsUp) {
        entitiesUp = entsUp;
    }

    /**
     * Get a list of the entities that will be up weighted.
     * @return array of entities to be up weighted
     */
    public ArrayList<Entity> getEntitiesUpweight() {
        return entitiesUp;
    }

    /**
     * Sets the first node of this link
     * @param n first node
     */
    public void setNode1(Node n) {
        node1 = n;
    }

    /**
     * Sets the second node of this link
     * @param n second node
     */
    public void setNode2(Node n) {
        node2 = n;
    }

    /**
     * Sets the acceptance of this link.
     * An accepted link will be processed and apply the entity weight changes.
     * @param b
     */
    public void setAccepted(boolean b) {
        accepted = b;
    }

    /**
     * Returns the status of this link pending or accepted.
     * @return true if accepted, false if rejected or pending.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Clears the link of all the current data.
     */
    public void clear() {
        node1 = null;
        node2 = null;
        accepted = false;
        entitiesUp = new ArrayList<Entity>();
        entitiesDown = new ArrayList<Entity>();
    }
}
