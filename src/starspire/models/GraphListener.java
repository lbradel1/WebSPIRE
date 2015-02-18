package starspire.models;

import java.awt.Dimension;
import java.util.EventListener;

/**
 * This interface describes the needs to listen to a graph.
 *
 * @author Patrick Fiaux, Alex Endert
 */
public interface GraphListener extends EventListener {
    public static  enum NodeModType {
        OTHER, PIN, WEIGHT
    }

    /**
     * Event triggered when a new node is added to the model.
     * @param n Node that was just added
     */
    public void nodeAdded(Node n);

    /**
     * Event Triggered when a node was modified (not moved)
     * @param n Node that was modified
     */
    public void nodeModified(Node n, NodeModType t);

    /**
     * Event Triggered when a node was moved
     * @param n Node that was moved
     */
    public void nodeMoved(Node n);

    /**
     * Event triggered when a node is removed
     * @param n Node that was removed
     */
    public void nodeRemoved(Node n);

    /**
     * Event Triggered when a node is selected
     * @param n Node that was selected
     */
    public void nodeSelected(Node n);

    /**
     * This even is triggered when a node is opened.
     * @param n Node opened
     */
    public void nodeOpened(Node n);

    /**
     * This event is triggered when a node is closed.
     * @param n Node closed
     */
    public void nodeClosed(Node n);

    /**
     * Event triggered when a new edge is added between 2 nodes
     * @param e Edge that was added
     */
    public void edgeAdded(Edge e);

    /**
     * Event triggered when an edge is modified
     * Entity added/removed or weight changed...
     * @param e Edge that was modified
     */
    public void edgeModified(Edge e);

    /**
     * Event triggered when an edge is removed
     * @param e Edge that was removed
     */
    public void edgeRemoved(Edge e);

    /**
     * The graph was resized, here's the new size.
     * @param d new size of the graph.
     */
    public void graphResized(Dimension d);



}
