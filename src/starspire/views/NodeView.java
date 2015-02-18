package starspire.views;

/*
 * Imports
 */
import starspire.models.Entity;
import starspire.models.Node;
import starspire.models.Highlight;
import starspire.models.Edge;
import starspire.models.GraphListener;
import starspire.models.DocumentNode;
import starspire.controllers.StarSpireController;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * Alright, this will be a small debug pop up window that will show
 * details on the selected node.
 * This will allow us to edit things like weight on the fly.
 * @author Pat
 */
public class NodeView extends JFrame implements GraphListener {
    
    private final int ENTITY_LIST_NUM = 2;

    private StarSpireController controller;
    private Node displayedNode;
    private JTextField id = new JTextField(10);
    private JTextField xcoord = new JTextField(4);
    private JTextField ycoord = new JTextField(4);
    private JTextField vx = new JTextField(4);
    private JTextField vy = new JTextField(4);
    private JTextField weight = new JTextField(10);
    private JTextField docName = new JTextField(10);
    private JTextArea entitiesInDoc = new JTextArea(10, 10);
    private JCheckBox pinned = new JCheckBox();
    private JEditorPane docNotes = new JEditorPane();
    /*
     * Content box stuff
     */
    private DocumentContentPane docContent = new DocumentContentPane();

    /**
     * default constructor
     * @param c ForceSpire Controller this view gets data from.
     */
    public NodeView(StarSpireController c) {
        super("Node Detail View");
        controller = c;
        c.addGraphListener(this);
        getContentPane().setLayout(new BorderLayout());

        /**
         * Build user interface
         */
        Box vbox, hbox;

        //vertical layout
        vbox = Box.createVerticalBox();

        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("Node id"));
        id.setEditable(false); //read only
        hbox.add(id);

        hbox.add(new JLabel("Pinned"));
        hbox.add(pinned);
        pinned.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                controller.pinNode(displayedNode, pinned.isSelected());
            }
        });

        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("X coordinate"));
        xcoord.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int value = Integer.valueOf(xcoord.getText());
                    controller.moveNode(displayedNode, value, displayedNode.getY());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(rootPane, ex,
                            "NumberFormatException for X Coord", JOptionPane.ERROR_MESSAGE);
                    xcoord.setText("" + displayedNode.getWeight());
                }
            }
        });
        hbox.add(xcoord);

        hbox.add(new JLabel("Y coordinate"));
        ycoord.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int value = Integer.valueOf(ycoord.getText());
                    controller.moveNode(displayedNode, displayedNode.getX(), value);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(rootPane, ex,
                            "NumberFormatException for Y Coord", JOptionPane.ERROR_MESSAGE);
                    ycoord.setText("" + displayedNode.getWeight());
                }
            }
        });
        hbox.add(ycoord);

        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("X Velocity"));
        vx.setEditable(false); //read only
        hbox.add(vx);

        hbox.add(new JLabel("Y Velocity"));
        vy.setEditable(false); //read only
        hbox.add(vy);

        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("Weight"));
        hbox.add(weight);
        weight.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    double value = Double.valueOf(weight.getText());
                    controller.setNodeWeight(displayedNode, value);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(rootPane, ex,
                            "NumberFormatException for Weight", JOptionPane.ERROR_MESSAGE);
                    weight.setText("" + displayedNode.getWeight());
                }
            }
        });


        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("Document Name"));
        docName.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String value = docName.getText();
                if (displayedNode instanceof DocumentNode) {
                    controller.setDocumentName(((DocumentNode) displayedNode).getDocument(), value);
                }
            }
        });
        hbox.add(docName);

        getContentPane().add(vbox, BorderLayout.NORTH);
        vbox = Box.createVerticalBox();
        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        Box groupBox = Box.createVerticalBox();
        groupBox.add(new JLabel("Document Content"));
        docContent.addDocumentContentPaneListener(new DocumentContentPane.DocumentContentPaneListener() {

            public void textHighlighted(int start, int end) {
                if (displayedNode instanceof DocumentNode && displayedNode != null) {
                    controller.addDocumentHighlight(((DocumentNode) displayedNode).getDocument(), start, end);
                }
            }

            public void createEntity(String s) {
                if (displayedNode != null) {
                    controller.addEntity(s, true);
                    updateDisplay(displayedNode);
                }

            }

            public void createSearch(String s) {
                    //do nothing
            }

			@Override
			public void removeEntity(String s) {
				if (displayedNode instanceof DocumentNode && displayedNode != null) {
					controller.removeEntity(s);
				}
				
			}
        });
        JScrollPane docScrollPane = new JScrollPane(docContent);
        docScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        docScrollPane.setPreferredSize(new Dimension(250, 145));
        docScrollPane.setMinimumSize(new Dimension(200, 100));
        groupBox.add(docScrollPane);
        hbox.add(groupBox);

        groupBox = Box.createVerticalBox();
        groupBox.add(new JLabel("Entities in Doc"));
        entitiesInDoc.setEditable(false); //read only
        entitiesInDoc.setLineWrap(false);
        JScrollPane entScrollPane = new JScrollPane(entitiesInDoc);
        groupBox.add(entScrollPane);
        hbox.add(groupBox);


        //new line
        hbox = Box.createHorizontalBox();
        vbox.add(hbox);

        hbox.add(new JLabel("Notes"));

        docNotes.setEditorKit(new RTFEditorKit());
        docNotes.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                //System.out.println("Key " + e.getKeyChar() + " Code " + e.getKeyCode());
                if (e.getKeyChar() == '\n' && displayedNode != null) {
                    try {
                        javax.swing.text.Document doc = docNotes.getDocument();
                        if (displayedNode instanceof DocumentNode) {
                            controller.setNote(((DocumentNode) displayedNode).getDocument(), doc.getText(0, doc.getLength()));
                        }
                    } catch (BadLocationException ex) {
                        System.err.println("Failed to get note text due to exception");
                        Logger.getLogger(NodeView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        docNotes.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                //Do nothing
            }

            public void focusLost(FocusEvent e) {
                if (displayedNode != null) {
                    try {
                        javax.swing.text.Document doc = docNotes.getDocument();
                        if (displayedNode instanceof DocumentNode) {
                            controller.setNote(((DocumentNode) displayedNode).getDocument(), doc.getText(0, doc.getLength()));
                        }
                    } catch (BadLocationException ex) {
                        System.err.println("Failed to get note text due to exception");
                        Logger.getLogger(NodeView.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        //docNotes.setEditable(false);
        docScrollPane = new JScrollPane(docNotes);
        docScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        docScrollPane.setPreferredSize(new Dimension(250, 145));
        docScrollPane.setMinimumSize(new Dimension(200, 100));
        hbox.add(docScrollPane);




        //setLocation(new Point(900, 800));
        setLocationRelativeTo(null);
        setSize(250, 180);


        getContentPane().add(vbox, BorderLayout.CENTER);
        pack();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setVisible(true);
    }

    /**
     * This updates the display with the node info
     * @param n node to use for update
     */
    private void updateDisplay(Node n) {
        assert (n != null);
        assert (n instanceof DocumentNode);
        id.setText("" + n.getID());
        xcoord.setText("" + n.getX());
        ycoord.setText("" + n.getY());
        vx.setText("" + round(n.getVX()));
        vy.setText("" + round(n.getVY()));
        weight.setText("" + round(n.getWeight()));
        pinned.setSelected(n.isPinned());

        //if Doc linked with Node, show Doc content
        if (((DocumentNode) n).getDocument() != null) {
            docName.setText(((DocumentNode) n).getDocument().getName());
            String edges = "";

            for (Edge e : n.getEdgeList()) {
                double str = ((int) (e.getStrength() * 100)) / 100.0;
                edges += "Edge " + e.getID() + " str: " + str + "\n";
                //String[] ents = e.entitiesToString();
                for (String s : e.entitiesToString()) {
                    edges += "   " + s + "\n";
                }
            }
            entitiesInDoc.setText(edges);
            docContent.setText(controller.getDocumentRTF(((DocumentNode) n).getDocument()));
            try {
                docContent.clearHighlights();
                Iterator<Highlight> iter = ((DocumentNode) n).getDocument().highlightIterator();
                while (iter.hasNext()) {
                    Highlight h = iter.next();
                    docContent.addHighlight(h.start, h.end);
                }
            } catch (BadLocationException ex) {
                System.err.println("Failed to refresh highlights due to BadLocationException");
            }
            docNotes.setText(controller.getNotesRTF(((DocumentNode) n).getDocument()));
        } else {
            docContent.setText("{\\rtf1\\ansi\\deff0 (none) }");
            docNotes.setText("{\\rtf1\\ansi\\deff0 (none) }");
            entitiesInDoc.setText("(none)");
            docName.setText("n/a");
        }
    }

    private double round(double number) {
        return (int) (number * 1000) / 1000.0;
    }

    /**
     * this clears the display like when no node is selected
     */
    private void clearDisplay() {
        id.setText("None");
        xcoord.setText("0");
        ycoord.setText("0");
        vx.setText("0");
        vy.setText("0");
        weight.setText("0");
        pinned.setSelected(false);
        docContent.setText(" ");
        entitiesInDoc.setText(" ");
    }

    /**
     * Not used right now
     * @param n Node in question.
     */
    public void nodeAdded(Node n) {
    }

    /**
     * If modified node is selected we need to update.
     * @param n Node in question.
     */
    public void nodeModified(Node n,NodeModType t) {
        if (n.equals(displayedNode)) {
            updateDisplay(n);
        }
    }

    /**
     * If moved node is selected node we need to update
     * @param n Node in question.
     */
    public void nodeMoved(Node n) {
        if (n.equals(displayedNode)) {
            updateDisplay(n);
        }
    }

    /**
     * Make sure node removed isn't the selected one, if so display nothing.
     * @param n Node in question.
     */
    public void nodeRemoved(Node n) {
        if (n.equals(displayedNode)) {
            updateDisplay(n);
        }
    }

    /**
     * New node selected, display that node's information.
     * @param n Node in question.
     */
    public void nodeSelected(Node n) {
        displayedNode = n;
        if (n == null) {
            clearDisplay();
        } else {
            updateDisplay(n);
        }
    }

    /**
     * Not displaying edges
     * @param e edge that changed
     */
    public void edgeAdded(Edge e) {
    }

    /**
     * Not displaying edges
     * @param e edge that changed
     */
    public void edgeModified(Edge e) {
    }

    /**
     * Not displaying edges
     * @param e edge that changed
     */
    public void edgeRemoved(Edge e) {
    }

    /**
     * Node was ... We don't care.
     * @param n Node
     */
    public void nodeOpened(Node n) {
    }

    /**
     * Node was ... We don't care.
     * @param n Node
     */
    public void nodeClosed(Node n) {
    }

    /**
     * Size changed...
     * @param d Dimensions.
     */
    public void graphResized(Dimension d) {
    }
}
