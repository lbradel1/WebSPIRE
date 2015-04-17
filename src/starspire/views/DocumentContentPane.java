package starspire.views;

/*
 * Imports
 */
import java.awt.Color;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;

import rtf.AdvancedRTFEditorKit;
import starspire.models.Entity;

/**
 * The node content pane is an enhanced TextEditorPane. It already has a
 * highlighter and a pop up menu .All it needs is for a few callbacks to be set.
 *
 * Since the node text pane is used in multiple places in the code this will
 * improve code reuse.
 * @author Patrick Fiaux, Alex Endert, Lauren Bradel
 */
public class DocumentContentPane extends JEditorPane {

    private Highlighter highlights;
    private Highlighter.HighlightPainter hc;
    private JPopupMenu addMenu;
    private JMenuItem addMenuHighlight;
    private JMenuItem addMenuEntity;
    private JMenuItem addMenuSearch;
    private JMenuItem addMenuRemoveEntity;
    private ArrayList<DocumentContentPaneListener> listeners;

    /**
     * Default Constructor.
     * Creates a JEditorPane but also adds the pop up menu and highlighter.
     * Sets content editor to RTF.
     */
    public DocumentContentPane() {
        super();

        listeners = new ArrayList<DocumentContentPaneListener>();

        highlights = new DefaultHighlighter();
        this.setHighlighter(highlights);
        hc = new DefaultHighlighter.DefaultHighlightPainter(new Color(90,230,85));

        addMenu = new JPopupMenu();
        addMenuHighlight = new JMenuItem("Highlight");
        addMenuHighlight.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    addHighlight(getSelectionStart(), getSelectionEnd());
                    fireHighlight(getSelectionStart(), getSelectionEnd());
                } catch (BadLocationException ex) {
                    System.err.println("Exception: Highlight BadLocationException...");
                }
            }
        });
        addMenu.add(addMenuHighlight);
        
        addMenuEntity = new JMenuItem("Make Entity");
        addMenuEntity.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireEntity(getSelectedText());
            }
        });
        addMenu.add(addMenuEntity);
        
        addMenuRemoveEntity = new JMenuItem("Remove Entity");
        addMenuRemoveEntity.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		//remove entity
        		
        		fireRemoveEntity(getSelectedText());
        	}
        });
        addMenu.add(addMenuRemoveEntity);
        
        
        
        addMenuSearch = new JMenuItem("Search");
        addMenuSearch.addActionListener(new ActionListener() {
            
           public void actionPerformed(ActionEvent e) {
               fireSearch(getSelectedText());
               
           } 
        });
        addMenu.add(addMenuSearch);

        this.setEditorKit(new AdvancedRTFEditorKit());
        this.setEditable(false);
        
        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                //if right click open menu
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (getSelectedText() != null) { //if has selection show regular menu
                        addMenuHighlight.setEnabled(true);
                        addMenuEntity.setEnabled(true);
                        addMenuRemoveEntity.setEnabled(true);
                        addMenuSearch.setEnabled(true);
                        addMenu.show(e.getComponent(), e.getX(), e.getY());
                    } else {
                        addMenuHighlight.setEnabled(false);
                        addMenuEntity.setEnabled(false);
                        addMenuRemoveEntity.setEnabled(false);
                        addMenuSearch.setEnabled(false);
                        addMenu.show(e.getComponent(), e.getX(), e.getY());
                        //if has no selection remove highlight/entity
                        //maybe pop up a different menu?
                    }
                }
            }
        });
    }

    /**
     * This is a shortcut. It calls removeAllHighlights on the builtIn
     * Highlighter.
     */
    public void clearHighlights() {
        highlights.removeAllHighlights();
    }

    /**
     * Another shortcut, calls addHighlight on the buildin highlighter.
     * @param start Start of highlihgt position
     * @param end end position of highlight
     * @throws BadLocationException the end and start are no good!
     */
    public void addHighlight(int start, int end) throws BadLocationException {
        highlights.addHighlight(start, end, hc);
    }
    
    /**
     * Use for highlighting search terms within a document
     * @param start
     * @param end
     * @param color
     * @throws BadLocationException 
     */
    public void addHighlight(int start, int end, Color color) throws BadLocationException {
        highlights.addHighlight(start, end, new DefaultHighlighter.DefaultHighlightPainter(color));
    }

    /**
     * A way to update the highlighter color.
     * @param c Color to highlight with.
     */
    public void setHighlighterColor(Color c) {
        hc = new DefaultHighlighter.DefaultHighlightPainter(c);
    }

    /**
     * Add a document listener to this pane.
     * @param l new listener
     */
    public void addDocumentContentPaneListener(DocumentContentPaneListener l) {
        listeners.add(l);
    }

    /**
     * Remove a listener. sometimes needed
     * @param l listener to remove.
     */
    public void removeDocumentContentPaneListener(DocumentContentPaneListener l) {
        listeners.remove(l);
    }

    /**
     * Helper fires a highlight event to all listeners
     * @param start start of highlight
     * @param end end of highlight
     */
    private void fireHighlight(int start, int end) {
        for (DocumentContentPaneListener l : listeners) {
            l.textHighlighted(start, end);
        }
    }

    /**
     * Helper fires a create entity event to all listeners.
     * @param s string containing the entity content
     */
    private void fireEntity(String s) {
        for (DocumentContentPaneListener l : listeners) {
            l.createEntity(s);
        }
    }
    
    private void fireRemoveEntity(String s) {
    	for(DocumentContentPaneListener l : listeners) {
    		l.removeEntity(s);
    	}
    }
    
    private void fireSearch(String s) {
        for (DocumentContentPaneListener l : listeners) {
            l.createSearch(s);
        }
    }

    /**
     * Interface to handle the 2 events that this class throws.
     */
    public interface DocumentContentPaneListener {

        /**
         * Some text was selected and then chosen for highlighting.
         * Here's the indexs of the highlight.
         * @param start start of highlight
         * @param end end of highlight
         */
        public void textHighlighted(int start, int end);

        /**
         * Some text was selected and then chosen for entity
         * creation
         * @param s selected text to create entity with.
         */
        public void createEntity(String s);
        
        public void createSearch(String s);
        
        public void removeEntity(String s);
    }
}
