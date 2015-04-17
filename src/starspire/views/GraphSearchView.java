package starspire.views;

/*
 * Imports
 */
import starspire.models.Node;
import starspire.models.SearchNode;
import starspire.models.Search;
import starspire.controllers.StarSpireController;
import starspire.StarSpireApp;

import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

/**
 * Well catching up on the JavaDoc... This is rather important,
 * it's the view for nodes on the graph when open, it's not a pop up!
 *
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class GraphSearchView extends JInternalFrame implements InternalFrameListener, ComponentListener {

    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());
    public static final String TITLE_PREFIX = "Search: ";
    private static final String HIGHLIGHT_ON = "Clear";
    private static final String HIGHLIGHT_OFF = "Highlight";
    private final Icon pinnedIcon = new ImageIcon(getClass().getResource("/starspire/images/pin_red.png"));
    private final Icon notpinnnedIcon = new ImageIcon(getClass().getResource("/starspire/images/pin_gray.png"));
    /**
     * This is one is worth a comment,
     * A panel with card layout we'll add in the splitView.
     * Will allow switching from Just doc view to split view with note.
     */
    private SearchNode node;
    private StarSpireController controller;
    private JTextField query;
    private JTextArea result;
    private JButton go, clear;
    private JToolBar toolbar;
    private JButton pin;
    private boolean highlight;
    /*
     * Tracks whether the location needs to be updated for this node.
     */
    private boolean updateLocation;
    
    private JPopupMenu addMenu;
    private JMenuItem addMenuPin;

    /**
     * Constructor.
     * Takes in a node to link to this view and the controller it's from.
     * @param n Node this view should link to
     * @param c Controller to allow view to make calls based on user actions.
     */
    public GraphSearchView(SearchNode n, StarSpireController c) {
        super("Untitled", false, true, false, true);
        updateLocation = true;
        node = n;
        controller = c;
        highlight = true;
        addMenu = new JPopupMenu();


        setSize(n.getWidth(), n.getHeight());
        setLayout(new BorderLayout());
        setTitle("New Search");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        addMenuPin = new JMenuItem("UnPin Node");
        addMenuPin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.pinNode(node, !node.isPinned());
            }
        });
        addMenu.add(addMenuPin);

        ActionListener newSearch = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.out.println("New Search...");
                String q = query.getText();
                if (q.length() > 1) {
                    Search s = controller.search(node, query.getText());
                    toolbar.remove(go);
                    query.setEnabled(false);
                    refresh();
                    
                } else {
                    //clear the search
                    if (node.getSearch() != null) {
                        /*if (highlight) {
                            controller.clearSearchHighlight(node.getSearch());
                            clear.setText(HIGHLIGHT_OFF);
                            highlight = false;
                        } else {*/
                            controller.searchHighlight(node.getSearch());
                            clear.setText(HIGHLIGHT_OFF);
                            highlight = true;
                            clear.setVisible(true);
                        //}
                    }
                }
            }
        };
        
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                //if right click open menu
                if (e.getButton() == MouseEvent.BUTTON3) {
                    addMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                
            }
        });

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setMargin(new Insets(0, 0, 0, 0));
        toolbar.add(Box.createHorizontalGlue());
        add(toolbar, BorderLayout.PAGE_START);

        query = new JTextField();
        query.putClientProperty("JComponent.sizeVariant", "small");

        go = new JButton("Search");
        go.putClientProperty("JComponent.sizeVariant", "small");
        go.addActionListener(newSearch);
        query.addActionListener(newSearch);
        clear = new JButton(HIGHLIGHT_OFF);
        clear.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (node.getSearch() != null) {
                    /*if (highlight) {
                        controller.clearSearchHighlight(node.getSearch());
                        clear.setText(HIGHLIGHT_OFF);
                        highlight = false;
                    } else {*/
                        controller.searchHighlight(node.getSearch());
                        clear.setText(HIGHLIGHT_OFF);
                        highlight = true;
                        clear.setVisible(true);
                    //}
                }
            }
        });
        clear.putClientProperty("JComponent.sizeVariant", "small");

        toolbar.add(query);
        toolbar.add(go);
        
        if(!node.getenableSearchButton()) {
            toolbar.remove(go);
            query.setEnabled(false);
            clear.setVisible(true);
            refresh();
        } else {
        	clear.setVisible(false);
        }
        
        toolbar.add(clear);

        /*
        result = new JTextArea();
        result.setEditable(false);
        add(result, BorderLayout.CENTER);
        */

        /*
        pin = new JButton(pinnedIcon);
        pin.setBorder(null);
        pin.setBorderPainted(false);
        pin.setMargin(new Insets(0, 0, 0, 0));
        pin.setAlignmentX(Component.RIGHT_ALIGNMENT);
        pin.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                controller.pinNode(node, !node.isPinned());
            }
        });
        toolbar.add(pin);
        */

        setMinimumSize(new Dimension(SearchNode.OPEN_SEARCH_MIN_WIDTH, SearchNode.OPEN_SEARCH_MIN_HEIGHT));
        addInternalFrameListener(this);
        addComponentListener(this);
        setVisible(node.isOpen());
        refresh();
    }

    /**
     * This calculates the color using the HSB that the search node randomly
     * generates. It uses the same constants that are used in GraphView, so
     * if changed in one place, make sure that you change them in the other.
     * @param n The Node from where the color is being calculated from
     * @return Color based on the hue of the Node
     */
    private Color calculateSearchColor(Node n) {

        int NODE_SATURATION = 60;
        int NODE_SELECTED_MOD = 20;
        int NODE_VAL = 60;
        int NODE_HUE = 226;
        int NODE_PIN_VAL = 60;
        int NODE_PIN_HUE = 0;
        int NODE_LINK_VAL = 60;
        int NODE_LINK_HUE = 300;
        int NODE_HIGHLIGHT_VAL = 100;
        int NODE_HIGHLIGHT_HUE = 106;


        /**
         * Determine Node Color
         */
        int h, s, b;
        s = NODE_SATURATION;
        if (n.getHighlight() == 1) {
            h = NODE_HIGHLIGHT_HUE;
            b = NODE_HIGHLIGHT_VAL;
        } else if (n.getHighlight() > 1) {
            h = n.getHighlight();
            b = NODE_HIGHLIGHT_VAL;
        } else if (n.isPinned()) {
            h = NODE_PIN_HUE;
            b = NODE_PIN_VAL;
        } else {
            h = NODE_HUE;
            b = NODE_VAL;
        }

        return Color.getHSBColor(h / 360.0f, s / 100.0f, b / 100.0f);
    }

    /**
     * This reloads the data in the frame based on the node's content.
     * Called when node content changes usually.
     */
    protected void refresh() {
        if (node.getSearch() != null) {
            setTitle(TITLE_PREFIX + node.getSearch().getSearchTerm());
            query.setText(node.getSearch().getSearchTerm());
            /*int r = node.getSearch().getResults();
            if (r == 0) {
                result.setText("No Match found.");
            } else {
                result.setText("Found in " + r + " documents.");
            }*/
            query.setEnabled(false);
            clear.setVisible(true);
            toolbar.remove(go);
            toolbar.setBackground(calculateSearchColor(node));
        }

        
        if (node.isPinned()) {
            //pin.setToolTipText("Unpin");
            //pin.setIcon(pinnedIcon);
            //pin.setBorder(null);
            addMenuPin.setText("UnPin Node");
        } else {
            //pin.setToolTipText("Pin");
            //pin.setIcon(notpinnnedIcon);
            //pin.setBorder(null);
            addMenuPin.setText("Pin Node");
        }

        if (isVisible()) {
            revalidate();
            repaint();
        }
    }

    /**
     * Closes this view if it's not already.
     */
    protected void close() {
        if (isVisible()) {
            setVisible(false);
        }
    }

    /**
     * Focuses the search view.
     */
    public void getFocus() {
        try {
            this.setSelected(true);
            //query.requestFocus();
        } catch (PropertyVetoException ex) {
            logger.log(Level.SEVERE, "Couldn't find search box focus", ex);
        }
    }

    /**
     * Opens this view if it's not already then refresh data.
     */
    protected void open() {
        if (!isVisible()) {
            setVisible(true);
            updateLocation();
            setSize(node.getWidth(), node.getHeight());
            refresh();
        }
    }

    /**
     * Get the node this graph view is linked to.
     * @return node
     */
    protected Node getNode() {
        return node;
    }

    public void updateLocation() {
        updateLocation = true;
    }

    /**
     * The node has moved, move frame to node's new location.
     */
    public void refreshLocation() {
        if (updateLocation) {
            Point newLoc = new Point(node.getX() - node.getWidth() / 2, node.getY() - node.getHeight() / 2);
            if (!newLoc.equals(getLocation())) {
                //System.out.println("Node Detail moved " + node + "...");
                //System.out.println("\tcurrent location" + getLocation());
                //System.out.println("\tnew location " + newLoc);
                setLocation(newLoc);
                //System.out.println("Node Detail move done!");
            }
            updateLocation = false;
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void internalFrameOpened(InternalFrameEvent e) {
    }

    /**
     * Delete search when closing the frame.
     * @param e Event details
     */
    public void internalFrameClosing(InternalFrameEvent e) {
        if (node.getSearch() != null) {
            /*
             * We have a search so remove it
             */
            controller.removeSearch(node.getSearch());
        } else {
            /*
             * this is an empty search node remove just the node
             */
            controller.removeNode(node);
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void internalFrameClosed(InternalFrameEvent e) {
    }

    /**
     * Customized minimize action. Highjack the event and close the
     * node instead of minimizing the frame.
     * @param e Event details
     */
    public void internalFrameIconified(InternalFrameEvent e) {
        //we don't want it to be an icon and we can't cancel it so undo it:
        try {
            setIcon(false);
            controller.setNodeOpen(node, false);
        } catch (PropertyVetoException ex) {
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    /**
     * Frame was activated, if this was triggered by user set node as selected.
     * So we should set the node of this frame as selected if it isn't already...
     * @param e Event details
     */
    public void internalFrameActivated(InternalFrameEvent e) {
        if (!controller.isSelected(node)) {
            controller.setSelected(node);
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    /**
     * The Frame was resized, resize the node.
     * @param e Event details
     */
    public void componentResized(ComponentEvent e) {
    }

    /**
     * The frame was moved move the node the same amount.
     * @param e Event details
     */
    public void componentMoved(ComponentEvent e) {
        if (controller.isSelected(node)) {
            Point newloc = getLocation();
            //convert to node location
            newloc.x += node.getWidth() / 2;
            newloc.y += node.getHeight() / 2;
            //move node to window location...
            controller.moveNode(node, newloc.x, newloc.y);
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void componentShown(ComponentEvent e) {
    }

    /**
     * Unused
     * @param e Event details
     */
    public void componentHidden(ComponentEvent e) {
    }
}
