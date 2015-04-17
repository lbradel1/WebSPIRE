package starspire.views;

import starspire.models.Entity;
import starspire.models.Node;
import starspire.models.DocumentLink;
import starspire.models.DataListener;
import starspire.models.Document;
import starspire.models.SearchNode;
import starspire.models.Search;
import starspire.models.Edge;
import starspire.models.GraphListener;
import starspire.models.DocumentNode;
import starspire.controllers.StarSpireController;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import javax.swing.*;
import java.util.Iterator;

/**
 * The graph view displays the graph visually.
 * @author Patrick Fiaux, Alex Endert
 */
public class GraphView extends JDesktopPane implements MouseListener, MouseMotionListener, MouseWheelListener, GraphListener, DataListener {

    private final static boolean NODE_RESIZING_ON = false;
	private final static int ENTITY_LIST_MAX = 4;
    private final static BasicStroke STROKE = new BasicStroke(1.0f);
    private final static BasicStroke THICK_STROKE = new BasicStroke(2.0f);
    private final static Color NODE_COLOR = new Color(53, 151, 143);
    private final static Color NODE_SELECTED_COLOR = new Color(1, 102, 193);
    private final static Color QUARTILE_2_COLOR = new Color(128, 205, 193);
    private final static Color QUARTILE_3_COLOR = new Color(199, 234, 245);
    private final static Color QUARTILE_4_COLOR = new Color(245, 245, 245);
    private final static int NODE_SATURATION = 80;
    private final static int NODE_SELECTED_MOD = 20;
    private final static int NODE_VAL = 70;
    private final static int NODE_HUE = 226;
    private final static int MSSI_NODE_HUE = 285;
    private final static int NODE_PIN_VAL = 60;
    private final static int NODE_PIN_HUE = 0;
    private final static int NODE_LINK_VAL = 60;
    private final static int NODE_LINK_HUE = 300;
    private final static int NODE_HIGHLIGHT_VAL = 100;
    private final static int NODE_HIGHLIGHT_HUE = 325;//106;
    private final static Color EDGE_SELECTED_COLOR = new Color(50, 50, 50);
    private final static Color EDGE_COLOR = new Color(200, 200, 200);
    private final static Color EDGE_LABEL_COLOR = new Color(75, 72, 158);
    private final static Color FORCESPIRE_BACKGROUND_COLOR = new Color(200, 200, 200);//Color.LIGHT_GRAY;   //this is the background color for the main window
    private final static int NODE_HIDDEN_HUE = 0;
    private final static int NODE_HIDDEN_VAL = 0;
    private final static double ARC_RADIUS_CLOSE = 6.0;
    private final static double ARC_RADIUS_OPEN = 10.0;
    private final static boolean PRINT_NODE_NAME = true;
    private static Line2D line = new Line2D.Double();
    private static RoundRectangle2D node = new RoundRectangle2D.Double();
    private StarSpireController controller;
    private Point offset;
    private Point lastPt;
    private int initialX, initialY;
    private ArrayList<GraphDocumentView> openDocuments;
    private ArrayList<GraphSearchView> openSearches;
    private ArrayList<DocumentNode> documentsToLink;
    private DocumentLink docLink;
    private Node linkTarget = null;

    /**
     * Default Constructor creates a new graph view for the given model.
     * @param fs
     */
    public GraphView(StarSpireController fs) {
        super();
        // Init variables
        setBackground(FORCESPIRE_BACKGROUND_COLOR);
        setLayout(null);
        lastPt = new Point();
        openDocuments = new ArrayList<GraphDocumentView>();
        openSearches = new ArrayList<GraphSearchView>();
        controller = fs;
        documentsToLink = new ArrayList<DocumentNode>();

        // Set up listeners
        controller.addGraphListener(this);
        controller.addDataListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);

        initializeOpenNodeViews();
    }

    /**
     * This is a helper function that will create an link an open node
     * view for all the nodes already in the graph.
     * Other wise id doesn't work when you open a file.
     */
    private void initializeOpenNodeViews() {
        Iterator<Node> iter = controller.getNodeIterator();
        while (iter.hasNext()) {
            Node n = iter.next();
            if (n instanceof DocumentNode) {
                GraphDocumentView iframe = new GraphDocumentView(((DocumentNode) n), controller);
                iframe.addMouseListener(new MouseListener() {

                    public void mouseClicked(MouseEvent e) {
                    }

                    public void mousePressed(MouseEvent e) {
                    }

                    public void mouseReleased(MouseEvent e) {
                        linkOnDragNDrop(e);
                    }

                    public void mouseEntered(MouseEvent e) {
                    }

                    public void mouseExited(MouseEvent e) {
                    }
                });
                openDocuments.add(iframe);
                add(iframe);
            } else if (n instanceof SearchNode) {
                GraphSearchView iframe = new GraphSearchView(((SearchNode) n), controller);
                openSearches.add(iframe);
                add(iframe);
            }
        }
    }

    /**
     * Overrides the paint method to display our graph. This paints everything
     * that's not a piece of the UI like edges and stuff..
     * @param g Graphics object
     */
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(FORCESPIRE_BACKGROUND_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        /* Paint all the edges */
        Iterator<Edge> edges = controller.getEdgeIterator();
        while (edges.hasNext()) {
            paintEdge(g2, edges.next());
        }
        /* Paint the nodes on top */
        Iterator<Node> nodes = controller.getNodeIterator();
        Node n;
        while (nodes.hasNext()) {
            n = nodes.next();
            //only paint closed nodes
            if (!n.isOpen()) {
                paintNode(g2, n);
            }
        }

        /*
         * While we have the lock move any internal frames that need to be moved
         */
        for (GraphDocumentView v : openDocuments) {
            if (v.getNode().isOpen()) {
                v.refreshLocation();
            }
        }
        for (GraphSearchView v : openSearches) {
            if (v.getNode().isOpen()) {
                v.refreshLocation();
            }
        }
    }

    /**
     * Draws the given edge on the screen.
     * uses graphics 2d.
     * The entities contained in the edge are drawn,
     * in order of weight of entities.
     * @param g graphics2D object
     * @param e edge to draw
     */
    private void paintEdge(Graphics2D g, Edge e) {

        //print entities
        int x = Math.min(e.getNode1().getX(), e.getNode2().getX())
                + Math.abs(e.getNode1().getX() - e.getNode2().getX()) / 2;
        int y = Math.min(e.getNode1().getY(), e.getNode2().getY())
                + Math.abs(e.getNode1().getY() - e.getNode2().getY()) / 2;
        boolean selected = controller.isSelected(e.getNode1()) || controller.isSelected(e.getNode2());

        line.setLine(e.getNode1().getX(), e.getNode1().getY(),
                e.getNode2().getX(), e.getNode2().getY());

        if (selected) {
            //draw edges for the selected nodes
            g.setPaint(EDGE_SELECTED_COLOR);
            g.setStroke(STROKE);
            g.draw(line);

            g.setPaint(EDGE_LABEL_COLOR);
            String[] ents = e.entitiesToStringSorted();
            String[] entList;
            if(ents.length <= ENTITY_LIST_MAX) {
                entList = ents;
            }
            else {
                entList = new String[ENTITY_LIST_MAX + 1];
                for(int i = 0; i < entList.length - 1; i++) {
                    entList[i] = ents[i];
                }
                entList[ENTITY_LIST_MAX] = "(" + (ents.length - ENTITY_LIST_MAX) + " more)";
            }
            
            for (int i = 0; i < entList.length; i++) {
                g.drawString(entList[i], x, y + i * 10);
            }
        }
    }

    /**
     * update the 2d rectangle based on the current node.
     * @param n
     */
    private void setRectangle(Node n) {
        int x, y;
        double r;
        if (n.isOpen()) {
            r = ARC_RADIUS_OPEN;
        } else {
            r = ARC_RADIUS_CLOSE;
        }
        
        int length = n.getWidth();
        
        if(n.getQuartile() == 0) {
            length = length;
            //TODO: try using a continuous scale
        }
        else if(n.getQuartile() == 1) {
            length -= 3;
        }
        else if(n.getQuartile() == 2) {
            length -= 5;
        }
        else if(n.getQuartile() == 3) {
            length -= 6;
        }
        
        
        x = n.getX() - n.getWidth() / 2;
        y = n.getY() - n.getHeight() / 2;

        node.setRoundRect(x, y, length, length, r, r);
        
    }

    /**
     * Draws the given node on the screen.
     * uses graphics 2D.
     * @param g Graphics2D object
     * @param n node to draw.
     */
    private void paintNode(Graphics2D g, Node n) {
        /**
         * Set up
         */
        setRectangle(n);
        boolean selected = controller.isSelected(n);

        /**
         * Specific actions based on node type
         */
        if (n instanceof DocumentNode) {
            /* Document Node Stuff */
            if (PRINT_NODE_NAME && !n.isOpen()) {
            	g.setColor(Color.BLACK);
                g.drawString(((DocumentNode) n).getDocument().getName(), n.getX() + (n.getWidth() / 2) + 5, n.getY());
            }
        }
        if (n instanceof SearchNode) {
            /* Search Node Stuff */
            SearchNode s = (SearchNode) n;
            //if(!n.isOpen()) {
            //if (!s.isOpen() && s.getSearch() != null) {
            	g.setColor(Color.BLACK);
                g.drawString(s.getSearch().getSearchTerm(), n.getX() + 10, n.getY());
            //}
        }


        /**
         * Determine Node Color
         */
        int h, s, b;
        h = NODE_HUE;
        s = NODE_SATURATION;
        b = NODE_VAL;
        
        /*if (n == linkTarget) {
            h = NODE_LINK_HUE;
            b = NODE_LINK_VAL;
        } else */
        if (n.getHighlight() == 1) {
            h = NODE_HIGHLIGHT_HUE;
            b = NODE_HIGHLIGHT_VAL;
        } else if (n.getHighlight() > 1) {
            h = n.getHighlight();
            b = NODE_HIGHLIGHT_VAL;
        } else {
            h = NODE_HUE;
            b = NODE_VAL;
        }
        
        
        if(n.getQuartile() == 0) {
            s = 90;
            //TODO: try using a continuous range
        }
        else if(n.getQuartile() == 1) {
            s = 75;
        }
        else if(n.getQuartile() == 2) {
            s = 60;
        }
        else if(n.getQuartile() == 3){
            s = 30;
        }
        
        
        //System.out.println("Value: " + b);
        

        if (selected) {
            s = Math.max(s - NODE_SELECTED_MOD, 0);
            b = Math.min(b + NODE_SELECTED_MOD, 100);
        }
        else {
            s = Math.max(0, s - (n.getRecency()));

        }

        //System.out.println("painting node");
        g.setColor(Color.getHSBColor(h / 360.0f, s / 100.0f, b / 100.0f));
        g.fill(node);

        //stroke
        if (selected) {
            g.setPaint(Color.darkGray);
            
        } 
        else if(!n.wasOpened()) {
            g.setPaint(Color.white);
        }else {
            g.setPaint(Color.black);
        }
        
        g.setStroke(STROKE);
        if(n.isPinned()) {
            g.setStroke(THICK_STROKE);
        }
        g.draw(node);
        repaint();
        g.setStroke(STROKE);
        g.setPaint(Color.black);
    }

    /**
     * Generates a rectangle around a node to be more efficient on repaint.
     * @param n node to be repainted
     * @return bounding rectangle for the given node
     */
    private Rectangle shapeRectangle(Node n) {
        int x = n.getX() - n.getWidth() / 2;
        int y = n.getY() - n.getHeight() / 2;
        return new Rectangle(x - 5, y - 5,
                x + n.getWidth() + 5, y + n.getHeight() + 5);
    }

    /**
     * Generates a rectangle around an edge to be more efficient on repaint.
     * we not only want to repaint the edge but the nodes it points to.
     *
     * We'll just take the rectangle for each node and make the upmost leftmost
     * point the origin and the bottommost rightmost point the opposite corner.
     *
     * @param e Edge to use
     * @return bounding rectangle for the given node
     */
    private Rectangle shapeRectangle(Edge e) {
        Rectangle r1 = shapeRectangle(e.getNode1());
        Rectangle r2 = shapeRectangle(e.getNode2());
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);

        return new Rectangle(x, y,
                //The width and height are the opposite corner - first.
                Math.max(r1.x + r1.width, r2.x + r2.width) - x,
                Math.max(r1.y + r1.height, r2.y + r2.height) - y);
    }

    /**
     * Adds a Node to the link list when they are clicked on, and linking is enabled.
     * @param n Node to add to link list
     */
    private void linkDocumentsAdd(Node n) {
       if (n instanceof DocumentNode) {
            documentsToLink.add((DocumentNode) n);
        }
       
       if (documentsToLink.size() >= 2) {
            //begin the linking
            //System.out.println("found 2 nodes, starting linking");

            //get the DocumentLink with all the entities in it that are relevant
            docLink = controller.populateDocumentLink(documentsToLink);

            //automatically accept the doclink and execute the upweighting
            docLink.setAccepted(true);
            docLink = controller.executeDocumentLink(docLink);
            docLink.clear();
            documentsToLink.clear();       

        }
    }

    /**
     * Handler for a mouse click.
     *
     * @param me MouseEvent passed in
     */
    public void mouseClicked(MouseEvent me) {
        Point pt = me.getPoint();
        //System.out.println("Click at " + pt);
        //right click
        
        if(me.getButton() == MouseEvent.BUTTON2 || (me.getButton() == MouseEvent.BUTTON3 && me.getClickCount() > 1)) {
        	Node selected = controller.getGraphSelected(); // can use this because pressed gets called before click
            if (selected != null) {
	            if(selected instanceof DocumentNode) {
	            	DocumentNode dn = (DocumentNode) selected;
	            	controller.decreaseDocWeight(dn.getDocument());
	            	controller.decreaseDocWeight(dn.getDocument());
	            	
	            	controller.removeDocument(dn.getDocument());
	            }
            }
        } //left click
        else if (me.getButton() == MouseEvent.BUTTON3) {
            Node selected = controller.getGraphSelected(); // can use this because pressed gets called before click
            if (selected != null) {
                controller.setSelected(selected);
                controller.pinNode(selected, !selected.isPinned());
            }
        } 
        else if (me.getButton() == MouseEvent.BUTTON1) {
            Node selected = controller.getGraphSelected(); // can use this because pressed gets called before click
            if (selected == null) {
                controller.setSelected(null);
            } else if (me.getClickCount() == 1) {
                controller.setSelected(selected);

            } else if (me.getClickCount() > 1) {
                controller.setSelected(selected);
                controller.setNodeOpen(selected, !selected.isOpen());
                if(selected instanceof DocumentNode) {
                    DocumentNode dn = (DocumentNode) selected;
                    controller.increaseDocWeight(dn.getDocument(), 0.5, false);
                    controller.stopLayout();
                    controller.pinNode(dn, true);
                    controller.startLayout();
                }
            }
        }
    }

    /**
     * This method handles dragging a node.
     * mouseClicked handles a click. (click + release)
     *
     * @param me MouseEvent passed in
     */
    public void mousePressed(MouseEvent me) {
        Point pt = me.getPoint();
        Node clicked = controller.findByPoint(pt);
        controller.setSelected(clicked);
        if (me.getButton() == MouseEvent.BUTTON1) {

            if (clicked != null) {
                if (pt != null) {
                    offset = new Point();
                    offset.setLocation(clicked.getX() - pt.getX(), clicked.getY() - pt.getY());
                    initialX = (int) pt.getX();
                    initialY = (int) pt.getY();
                }
                lastPt = me.getPoint();
            }
        }
    }

    /**
     * Does nothing yet
     * @param me mouse event details
     */
    public void mouseReleased(MouseEvent me) {
        //GENERATE UNDO HERE
        linkOnDragNDrop(me);
    }

    /**
     * Private Helper for linking on drag and drop so that the code is in
     * 1 place instead of 3. That way when it breaks there's only 1 place to fix.
     * @param e
     */
    private void linkOnDragNDrop(MouseEvent me) {
        Node n = controller.getGraphSelected();
        if (n != null) {
            lastPt = me.getPoint();
            Node hover = controller.findOverlappingNode(n);
            if (hover != null && hover.isPinned() && !(hover instanceof SearchNode)) {
                linkDocumentsAdd(n);
                linkDocumentsAdd(hover);
                linkTarget = null;
                /*
                 * This is the old code that makes the node go back to the previous location
                 * and puts the dialog box in the middle of the two nodes when linking
                Point startPoint = n.getSelectionLocation();
                controller.moveNode(n, startPoint.x, startPoint.y);
                linkDocumentsAdd(n);
                linkDocumentsAdd(hover);
                linkTarget = null;
                 */
            }
        }
    }

    /**
     * unused
     * @param me mouse event details
     */
    public void mouseEntered(MouseEvent me) {
    }

    /**
     * unused
     * @param me mouse event details
     */
    public void mouseExited(MouseEvent me) {
    }

    /**
     * Handles dragging of selected node here.
     * @param me mouse event details
     */
    public void mouseDragged(MouseEvent me) {
        Node n = controller.getGraphSelected();
        if (n != null) {
            Point pt = me.getPoint();
            int x = pt.x + offset.x;
            int y = pt.y + offset.y;
            if (me.isShiftDown()) {
                controller.moveNode(n, n.getX(), y);
            } // check on control- move only on x
            else if (me.isControlDown()) {
                controller.moveNode(n, x, n.getY());
            } // free dragging
            else {
                controller.moveNode(n, x, y);
            }
            lastPt = me.getPoint();

            if (controller.findOverlappingNode(n) != null) {
                //System.out.println("nodes are overlapping!");
                linkTarget = n;
            } else {
                linkTarget = null;
            }
        }
    }

    /**
     * nothing yet but will handle hover possibly.
     * @param me Mouse event details.
     */
    public void mouseMoved(MouseEvent me) {
        //change mouse cursors and stuff like that
        //also handle hover menu for nodes?
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        //System.out.println("mouse wheel moved");
        
    	if(NODE_RESIZING_ON) {
	        Node node = controller.getGraphSelected();
	        int amount = e.getWheelRotation();
	        //System.out.println(amount);
	        if(node != null && !(node instanceof SearchNode)) {
	        	amount = 0 - amount;
	        	controller.adjustNodeSize(node, amount);
	        }
    	}
        
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param n Node
     */
    public void nodeAdded(Node n) {
        if (n instanceof DocumentNode) {
            repaint(shapeRectangle(n));
            GraphDocumentView o = new GraphDocumentView(((DocumentNode) n), controller);
            o.addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent e) {
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                    linkOnDragNDrop(e);
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });
            openDocuments.add(o);
            add(o);
            try {
                o.setSelected(true);
            } catch (PropertyVetoException ex) {
            }
        } else if (n instanceof SearchNode) {
            repaint(shapeRectangle(n));
            GraphSearchView o = new GraphSearchView(((SearchNode) n), controller);
            openSearches.add(o);
            add(o);
            try {
                o.setSelected(true);
            } catch (PropertyVetoException ex) {
            }
        }
    }

    /**
     * A node was opened.Update stuff accordingly...
     * @param n Node that opened.
     */
    public void nodeOpened(Node n) {
        if (n instanceof DocumentNode) {
            for (GraphDocumentView g : openDocuments) {
                if (n.equals(g.getNode())) {
                    g.open();
                }
            }
        } else if (n instanceof SearchNode) {
            for (GraphSearchView g : openSearches) {
                if (n.equals(g.getNode())) {
                    g.open();
                }
            }
        }
    }

    /**
     * A node was closed. Update stuff accordingly...
     * @param n Node that closed.
     */
    public void nodeClosed(Node n) {
        if (n instanceof DocumentNode) {
            for (GraphDocumentView g : openDocuments) {
                if (g.isVisible() && n.equals(g.getNode())) {
                    g.close();
                }
            }
        } else if (n instanceof SearchNode) {
            for (GraphSearchView g : openSearches) {
                if (g.isVisible() && n.equals(g.getNode())) {
                    g.close();
                }
            }
        }
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param n Node
     */
    public void nodeModified(Node n, NodeModType t) {
        if (n instanceof DocumentNode) {
            for (GraphDocumentView g : openDocuments) {
                if (n.equals(g.getNode())) {
                    g.refresh();
                }
            }
        } else if (n instanceof SearchNode) {
            for (GraphSearchView g : openSearches) {
                if (n.equals(g.getNode())) {
                    g.refresh();
                }
            }
        } else {
            
        }


        repaint(shapeRectangle(n));
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param n Node
     */
    public void nodeMoved(Node n) {
        if (n.isOpen()) {
            if (n instanceof DocumentNode) {
                for (GraphDocumentView g : openDocuments) {
                    if (n.equals(g.getNode())) {
                        g.updateLocation();
                    }
                }
            } else if (n instanceof SearchNode) {
                for (GraphSearchView g : openSearches) {
                    if (n.equals(g.getNode())) {
                        g.updateLocation();
                    }
                }
            }
            
            
        }
        repaint();
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param n Node
     */
    public void nodeRemoved(Node n) {
        repaint(shapeRectangle(n));
        if (n instanceof DocumentNode) {
            GraphDocumentView remove = null;
            for (GraphDocumentView o : openDocuments) {
                if (o.getNode().equals(n)) {
                    remove = o;
                    break;
                }
            }
            if (remove != null) {
                openDocuments.remove(remove);
                remove(remove);
            }
        } else if (n instanceof SearchNode) {
            GraphSearchView remove = null;
            for (GraphSearchView o : openSearches) {
                if (o.getNode().equals(n)) {
                    remove = o;
                    break;
                }
            }
            if (remove != null) {
                openSearches.remove(remove);
                remove(remove);
            }
        }

    }

    /**
     * Repaint graph with new model elements or changes.
     * @param n Node
     */
    public void nodeSelected(Node n) {
        repaint();
        // check for linking
        if (controller.isLinking()) {
            linkDocumentsAdd(n); //LINKS WHEN DOC IS SELECTED?
        }
        if (n instanceof DocumentNode) {
            for (GraphDocumentView g : openDocuments) {
                if (g.isVisible()) {
                    if (g.getNode().equals(n)) {
                        try {
                            g.setSelected(true);
                        } catch (PropertyVetoException ex) {
                        }
                    } else {
                        try {
                            g.setSelected(false);
                        } catch (PropertyVetoException ex) {
                        }
                    }
                }
            }
        } else if (n instanceof SearchNode) {
            for (GraphSearchView g : openSearches) {
                if (g.isVisible()) {
                    if (g.getNode().equals(n)) {
                        try {
                            g.setSelected(true);
                        } catch (PropertyVetoException ex) {
                        }
                    } else {
                        try {
                            g.setSelected(false);
                        } catch (PropertyVetoException ex) {
                        }
                    }
                }
            }
        }

    }

    /**
     * Repaint graph with new model elements or changes.
     * @param e edge
     */
    public void edgeAdded(Edge e) {
        repaint(shapeRectangle(e));
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param e edge
     */
    public void edgeModified(Edge e) {
        repaint(shapeRectangle(e));
    }

    /**
     * Repaint graph with new model elements or changes.
     * @param e edge
     */
    public void edgeRemoved(Edge e) {
        repaint(shapeRectangle(e));
    }

    /**
     * unused.
     * @param doc document added.
     */
    public void documentAdded(Document doc) {
    }
    
    public void tryRefresh() {
        for (GraphDocumentView g : openDocuments) {
            g.refresh();
        }
        repaint();
    }

    /**
     * Unused
     * @param doc document that was modified.
     * @param type the type of the modification that's been done to the document
     */
    public void documentModified(Document doc, int type) {
        for (GraphDocumentView g : openDocuments) {
            if(controller.findOverlappingNode(controller.findNode(doc)) != null) {
                linkTarget = controller.findNode(doc);
            }
            g.refresh();
            
            
            if(g.getNode().getDocument().getIsVisible()) {
                g.getNode().setVisible(true);
                
                //g.setForeground(Color.getHSBColor(this.NODE_HUE / 360.0f, this.NODE_SATURATION / 100.0f, this.NODE_VAL / 100.0f));
                
                g.repaint();
            }
            else {
                //g.setVisible(false); //currently hides the document itself
                g.getNode().setVisible(false);
                //g.setForeground(this.NODE_HIDDEN_COLOR);
                //this.nodeRemoved(n); //not sure what this does
                g.repaint();
            } 
            //g.repaint();
            g.getNode().setRank(doc.getRank());
            g.getNode().setQuartile(doc.getQuartile());
            g.getNode().setRecency(doc.getRecency());

            g.refresh();
            g.repaint();
        }
        Iterator<Document> d = controller.documentIterator();
        while(d.hasNext()) {
            Document dn = d.next();
            boolean visible = dn.getIsVisible();
            if(controller.findNode(dn).isVisible() != visible) {
                controller.findNode(dn).setVisible(visible);
                nodeModified(controller.findNode(dn), NodeModType.OTHER);
            }
           controller.findNode(dn).setRank(dn.getRank());
           controller.findNode(dn).setQuartile(dn.getQuartile());
        }
        
        
        repaint();
    }

    /**
     * Unused
     * @param doc document that was removed.
     */
    public void documentRemoved(Document doc) {
    }

    /**
     * Unused
     * @param ent Entity that was added
     */
    public void entityAdded(Entity ent) {
        //repaint(shapeRectangle(n));
    }

    /**
     * Unused
     * @param ent Entity that was modified
     * @param type gives information as to what caused the entity modification (unused here)
     */
    public void entityModified(Entity ent, int type) {
    }

    /**
     * When an entity is added to a document find the node for that
     * document and refresh it.
     * @param ent Entity that was added
     * @param doc document it was added to.
     */
    public void entityDocumentAdded(Entity ent, Document doc) {
        for (GraphDocumentView g : openDocuments) {
            if (doc.equals(g.getNode().getDocument())) {
                g.refresh();
            }
        }
    }

    /**
     * Unused
     * @param ent Entity that was removed
     */
    public void entityRemoved(Entity ent) {
        for (GraphDocumentView g : openDocuments) {
            g.refresh();
        }
    }

    /**
     * Unused
     * @param ent Entity that was removed
     * @param doc Document it was removed from.
     */
    public void entityDocumentRemoved(Entity ent, Document doc) {
    }

    /**
     * Unused
     * @param d New dimensions for the graph
     */
    public void graphResized(Dimension d) {
    }

    /**
     * Unused
     * @param s search
     */
    public void searchAdded(Search s) {
    }

    /**
     * Unused
     * @param s search
     */
    public void searchRemoved(Search s) {
    }

    public void entitySearchAdded(Entity ent, Search s) {
    }

    public void entitySearchRemoved(Entity ent, Search s) {
    }
}
