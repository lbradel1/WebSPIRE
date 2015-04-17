package starspire.views;

/*
 * Imports
 */
import starspire.models.Entity;
import starspire.models.Node;
import starspire.models.Highlight;
import starspire.models.DocumentNode;
import starspire.controllers.StarSpireController;

import java.beans.PropertyVetoException;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;

import starspire.models.Document;

/**
 * Well catching up on the JavaDoc... This is rather important,
 * it's the view for nodes on the graph when open, it's not a pop up!
 *
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class GraphDocumentView extends JInternalFrame implements InternalFrameListener, ComponentListener, MouseMotionListener, MouseListener {

    private final static String CARD_DOC_ONLY = "Document only view";
    private final static String CARD_DOC_WITH_NOTE = "Document and note in split view";
    private final static Color DEFAULT_COLOR = new Color(241, 241, 161);
    //private final static Color PINNED_COLOR = new Color(185, 58, 58);
    private final static Color HOVER_COLOR = new Color(230,230,230);
    private final static Color LINK_COLOR = new Color(191, 78, 206);
    private final static Color PINNED_COLOR = new Color(241, 241, 161);
    private final Icon noteOpenIcon = new ImageIcon(getClass().getResource("/starspire/images/note_grey.png"));
    private final Icon noteCloseIcon = new ImageIcon(getClass().getResource("/starspire/images/note_yellow.png"));
    private final Icon pinnedIcon = new ImageIcon(getClass().getResource("/starspire/images/pin_red.png"));
    private final Icon notpinnnedIcon = new ImageIcon(getClass().getResource("/starspire/images/pin_gray.png"));
   // private final Icon linkIcon = new ImageIcon(getClass().getResource("/starspire/images/link_icon.png"));
    
    /**
     * This is one is worth a comment,
     * A panel with card layout we'll add in the splitView.
     * Will allow switching from Just doc view to split view with note.
     */
    private JPanel centerPanel;
    private CardLayout cardLayout;
    private DocumentContentPane docView; //one doc to view alone
    private JSplitPane splitView;
    private DocumentContentPane content; //one doc to view with notes
    private JEditorPane notes;
    private DocumentNode node;
    private StarSpireController controller;
    private JToolBar toolbar;
    private JButton pin;
    private JButton noteToggle;
    /*
     * Tracks whether the location needs to be updated for this node.
     */
    private boolean updateLocation;
    private boolean note_view;
    
    private int currentWidth = Node.OPEN_MIN_WIDTH;
    private int currentHeight = Node.OPEN_MIN_HEIGHT;
    
    private boolean canUpdate = false;
    
    private JPopupMenu addMenu;
    private JMenuItem addMenuQuery;
    private JMenuItem addMenuPin;
    private JMenuItem addMenuNote;


    /**
     * Constructor.
     * Takes in a node to link to this view and the controller it's from.
     * @param n Node this view should link to
     * @param c Controller to allow view to make calls based on user actions.
     */
    public GraphDocumentView(DocumentNode n, StarSpireController c) {
        super("Untitled", true, true, false, true);
        updateLocation = true;
        node = n;
        controller = c;
        setSize(n.getWidth(), n.getHeight());
                
        setLayout(new BorderLayout());
        setTitle(n.getDocument().getName());
        
        addMenu = new JPopupMenu();
        
        addMenuPin = new JMenuItem("UnPin Node");
        addMenuPin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.pinNode(node, !node.isPinned());
            }
        });
        addMenu.add(addMenuPin);
                
        
        addMenuQuery = new JMenuItem("Find Similar");
        addMenuQuery.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    Document d = node.getDocument();
                    controller.increaseDocWeight(d);
                } catch (Exception ex) {
                    System.err.println("Exception: Query Exception...");
                }
            }
        });
        addMenu.add(addMenuQuery);
        
        addMenuNote = new JMenuItem("Add Note");
        addMenuNote.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (note_view) {
                    addMenuNote.setText("Add Note");
                    cardLayout.show(centerPanel, CARD_DOC_ONLY);
                } else {
                    addMenuNote.setText("Remove Note");
                    cardLayout.show(centerPanel, CARD_DOC_WITH_NOTE);
                }
                note_view = !note_view;
            }
        });
        addMenu.add(addMenuNote);
        

        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                //if right click open menu
                if (e.getButton() == MouseEvent.BUTTON3) {
                    addMenuQuery.setEnabled(true);
                    addMenu.show(e.getComponent(), e.getX(), e.getY());
                }
                
            }
        });
        
        /* DOESN'T WORK
        this.addKeyListener(new KeyAdapter() {
            
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    boolean noMSSI = true;
                    System.out.println("Shift Pressed");
                }
                System.out.println("SHIFT");
            }
        });
        */


        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.add(Box.createHorizontalGlue());
        //every component added to the toolbar after this
        //add(toolbar, BorderLayout.PAGE_START);

        /*
         * Note button stuff
         * Be smart if there's a note set it to enabled, if not off
         */
        noteToggle = new JButton(noteOpenIcon);
        if (n.getDocument().getNotes().length() > 0) {
            note_view = true;
            noteToggle.setIcon(noteCloseIcon);
        } else {
            note_view = false;
        }
        noteToggle.setBorder(null);
        noteToggle.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (note_view) {
                    noteToggle.setIcon(noteOpenIcon);
                    cardLayout.show(centerPanel, CARD_DOC_ONLY);
                } else {
                    noteToggle.setIcon(noteCloseIcon);
                    cardLayout.show(centerPanel, CARD_DOC_WITH_NOTE);
                }
                note_view = !note_view;
                noteToggle.setBorder(null);
            }
        });
        toolbar.add(noteToggle);

        pin = new JButton(pinnedIcon);
        pin.setBorder(null);
        pin.setAlignmentX(Component.RIGHT_ALIGNMENT);
        pin.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                controller.pinNode(node, !node.isPinned());
            }
        });
        toolbar.add(pin);

        docView = new DocumentContentPane();
        docView.addDocumentContentPaneListener(new DocumentContentPane.DocumentContentPaneListener() {

            public void textHighlighted(int start, int end) {
                controller.addDocumentHighlight(node.getDocument(), start, end);
                //TODO make highlight entity...
                refresh();
            }

            public void createEntity(String s) {
                controller.addEntity(s, true);
                refresh();
            }

            public void createSearch(String s) {
                System.out.println("search try a");
                controller.addSearch(s);
                refresh();
            }

			@Override
			public void removeEntity(String s) {
				controller.removeEntity(s);
				refresh();
				
			}
        });
        JScrollPane docViewScrollPane = new JScrollPane(docView);
        docViewScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        content = new DocumentContentPane();
        content.addDocumentContentPaneListener(new DocumentContentPane.DocumentContentPaneListener() {

            public void textHighlighted(int start, int end) {
                controller.addDocumentHighlight(node.getDocument(), start, end);
                //TODO make highlight entity...
                refresh();
            }

            public void createEntity(String s) {
                controller.addEntity(s, true);
                refresh();
            }

            public void createSearch(String s) {
                System.out.println("search try b");
                controller.addSearch(s);
                refresh();
            }

			@Override
			public void removeEntity(String s) {
				controller.removeEntity(s);
				refresh();
				
			}
        });
        JScrollPane docScrollPane = new JScrollPane(content);
        docScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        notes = new JEditorPane();
        notes.setEditorKit(new RTFEditorKit());
        notes.addKeyListener(new KeyAdapter() {

            /**
             * Update the document when the notes are updated.
             */
            @Override
            public void keyTyped(KeyEvent e) {
                //System.out.println("Key " + e.getKeyChar() + " Code " + e.getKeyCode());
                if (e.getKeyChar() == '\n') {
                    try {
                        javax.swing.text.Document doc = notes.getDocument();
                        controller.setNote(node.getDocument(), doc.getText(0, doc.getLength()));
                        refresh();
                    } catch (BadLocationException ex) {
                        System.err.println("Failed to get note text due to exception");
                        Logger.getLogger(NodeView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        notes.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                //Do nothing
            }

            /**
             * Update the document when the notes lose focus
             */
            public void focusLost(FocusEvent e) {
                try {
                    javax.swing.text.Document doc = notes.getDocument();
                    controller.setNote(node.getDocument(), doc.getText(0, doc.getLength()));
                    refresh();
                } catch (BadLocationException ex) {
                    System.err.println("Failed to get note text due to exception");
                    Logger.getLogger(NodeView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        JScrollPane noteScrollPane = new JScrollPane(notes);
        noteScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        splitView = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitView.setTopComponent(docScrollPane);
        splitView.setBottomComponent(noteScrollPane);
        splitView.setDividerLocation((int) (node.getHeight() * node.getDividerPosition()));

        /*
         * Do the split view and card layout
         */
        centerPanel = new JPanel();
        centerPanel.setLayout(new CardLayout());
        centerPanel.add(docViewScrollPane, CARD_DOC_ONLY);
        centerPanel.add(splitView, CARD_DOC_WITH_NOTE);
        cardLayout = (CardLayout) centerPanel.getLayout();
        if (note_view) {
            //display noteview if note isn't empty
            cardLayout.show(centerPanel, CARD_DOC_WITH_NOTE);
        }
        add(centerPanel, BorderLayout.CENTER);
        /*
         * The minus 20 is because MacOS counts the shading around the frame
         * as part of set size but apparently not min size...
         */
        setMinimumSize(new Dimension(Node.OPEN_MIN_WIDTH, Node.OPEN_MIN_HEIGHT));
        addInternalFrameListener(this);
        addComponentListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        setVisible(node.isOpen());
        refresh();
    }

    /**
     * This reloads the data in the frame based on the node's content.
     * Called when node content changes usually.
     */
    public void refresh() {
        int caretPosition = docView.getCaretPosition();
        setTitle(node.getDocument().getName());
        String docText = controller.getDocumentRTF(node.getDocument());
        docView.setText(docText); //TODO optimize based on what's open...
        content.setText(docText);
        String noteText = controller.getNotesRTF(node.getDocument());
        notes.setText(noteText);

        if (node.isPinned()) {
            notes.setBackground(PINNED_COLOR);
            pin.setToolTipText("Unpin");
            pin.setIcon(pinnedIcon);
            pin.setBorder(null);
            addMenuPin.setText("UnPin Node");
        } else {
            notes.setBackground(DEFAULT_COLOR);
            pin.setToolTipText("Pin");
            pin.setIcon(notpinnnedIcon);
            pin.setBorder(null);
            addMenuPin.setText("Pin Node");
        }

        try {
            Iterator<Highlight> iter = node.getDocument().highlightIterator();
            docView.clearHighlights();
            content.clearHighlights();
            while (iter.hasNext()) {
                Highlight h = iter.next();
                content.addHighlight(h.start, h.end);
                docView.addHighlight(h.start, h.end);
            }
        } catch (BadLocationException ex) {
            System.err.println("Failed to refresh highlights due to BadLocationException");
        }

        //return to previous position
        docView.setCaretPosition(caretPosition);
        //content.setCaretPosition(0);
        
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
     * Opens this view if it's not already then refresh data.
     */
    protected void open() {
        if (!isVisible()) {
            setVisible(true);
            updateLocation();
            setSize(node.getWidth(), node.getHeight());
            splitView.setDividerLocation((int) (node.getHeight() * node.getDividerPosition()));
            refresh();
        }
    }

    /**
     * Get the node this graph view is linked to.
     * @return node
     */
    protected DocumentNode getNode() {
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
     * Unused
     * @param e Event details
     */
    public void internalFrameClosing(InternalFrameEvent e) {
        controller.decreaseDocWeight(node.getDocument());
        controller.decreaseDocWeight(node.getDocument());

        controller.removeDocument(node.getDocument());
        
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
            controller.pinNode(node, false);
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
    
    public void mouseClicked(MouseEvent e) {
    }
        
    public void mouseReleased(MouseEvent e) {
        docView.setBackground(Color.WHITE);
        content.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
     * Highlight when over a pinned node.
     * @param e Event details
     */
    public void mouseDragged(MouseEvent e) {
        Node n = controller.findOverlappingNode(node);
        if (n != null) {
            //System.out.println("nodes are overlapping!");
            toolbar.setBackground(LINK_COLOR);
            docView.setBackground(HOVER_COLOR);
            content.setBackground(HOVER_COLOR);
            centerPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 3, 2, LINK_COLOR));
            
            
        } else {
            toolbar.setBackground(SystemColor.window);
            docView.setBackground(Color.WHITE);
            content.setBackground(Color.WHITE);
            centerPanel.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    /**
     * Unused
     * @param e Event details
     */
    public void mouseMoved(MouseEvent e) {
    }    

    public void mousePressed(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }
}
