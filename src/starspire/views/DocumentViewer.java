package starspire.views;

/*
 * Imports
 */
import starspire.models.Entity;
import starspire.models.DataListener;
import starspire.models.Document;
import starspire.models.Highlight;
import starspire.models.Search;
import starspire.controllers.StarSpireController;

import javax.swing.event.ListSelectionEvent;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;

import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;


/**
 * The document viewer will be a view for the documents.
 * It lists the documents in the models and provide UI for adding,
 * removing and modifying them.
 * @author Patrick Fiaux, Alex Endert
 */
public class DocumentViewer extends JFrame implements DataListener {

    private StarSpireController controller;
    private Document selectedDocument;

    /*
     * UI components
     */
    JTextField docName;
    //JEditorPane docContent;
    DocumentContentPane docContent1;
    JList documentList;
    DefaultListModel documents;
    JTextArea docEntities;
    JButton addDocument;
    JButton removeDocument;
    JTextField search;

    /**
     * Default constructor, creates a new document viewer for a given data
     * model.
     * @param c controller to use for this view.
     */
    public DocumentViewer(StarSpireController c) {
	controller = c;
	selectedDocument = null;

	//load all the form elements and place them.
	initializeLayout();

	//load the data and set listeners.
	initializeData();

	//show it up
	pack();
	setVisible(true);
    }

    /**
     * This initializes the listeners and loads documents already in the model.
     */
    private void initializeData() {
	controller.addDataListener(this);

	//addd all documents to list
	refreshListView();
    }

    /**
     * Constructor helper that sets up the layout of the components
     * and initializes them.
     */
    private void initializeLayout() {
	getContentPane().setLayout(new BorderLayout());
	setLocationRelativeTo(null); //make it open in empty space
	setTitle("Document Viewer");
	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	/*
	 * Start with north
	 */
	Box group = Box.createHorizontalBox();
	getContentPane().add(group, BorderLayout.NORTH);
	addDocument = new JButton("+");
	addDocument.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		String name, content;
		name = (String)JOptionPane.showInputDialog(
                    this,
                    "Please enter a name for the new Document:");
		content = (String)JOptionPane.showInputDialog(
                    this,
                    "Please enter content for the new Document:");
		if (!content.isEmpty()) {
		    if (name.isEmpty()) {
			controller.addDocument(content);
		    }
		    controller.addDocument(content, name);
		}
	    }
	});
	group.add(addDocument);

	removeDocument = new JButton("-");
	removeDocument.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		if (selectedDocument != null)
		    controller.removeDocument(selectedDocument);
		    selectedDocument = null;
		    refreshDetailView();
	    }
	});
	group.add(removeDocument);
	group.add(new JLabel("Search:"));
	search = new JTextField("not yet working");
	search.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		//Do something here
	    }
	});
	group.add(search);

	/*
	 * Add List section
	 */
	documents = new DefaultListModel();
	documentList = new JList(documents);
	documentList.addListSelectionListener(new ListSelectionListener() {

	    public void valueChanged(ListSelectionEvent e) {
		refreshListSelection();
	    }
	});
	documentList.setVisibleRowCount(10);
	JScrollPane scrollPane = new JScrollPane(documentList);
	scrollPane.setPreferredSize(new Dimension(300, 200));
	getContentPane().add(scrollPane, BorderLayout.CENTER);

	/*
	 * Add detailed section at the bottom
	 */
	group = Box.createHorizontalBox();
	getContentPane().add(group, BorderLayout.SOUTH);
	Box vgroup = Box.createVerticalBox();
	group.add(vgroup);
	vgroup.add(new JLabel("Doc Name"));
	docName = new JTextField(10);
	docName.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		if (selectedDocument != null) {
		    String value = docName.getText();
		    controller.setDocumentName(selectedDocument, value);
		}
	    }
	});
	vgroup.add(docName);
	vgroup.add(new JLabel("Doc Content"));
	//docContent = new JEditorPane();
        //docContent.setEditorKit(new RTFEditorKit());
        //docContent.setEditable(false);
	docContent1 = new DocumentContentPane();
	docContent1.addDocumentContentPaneListener(new DocumentContentPane.DocumentContentPaneListener() {

            public void textHighlighted(int start, int end) {
                if (selectedDocument != null)
                    controller.addDocumentHighlight(selectedDocument, start, end);
            }

            public void createEntity(String s) {
                if (selectedDocument != null) {
                    controller.addEntity(s, true);
                    refreshDetailView();
                }

            }

            public void createSearch(String s) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            

			@Override
			public void removeEntity(String s) {
				// TODO Auto-generated method stub
				
			}
        });

        scrollPane = new JScrollPane(docContent1);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(250, 145));
        scrollPane.setMinimumSize(new Dimension(200, 75));
	vgroup.add(scrollPane);
	vgroup = Box.createVerticalBox();
	group.add(vgroup);
	vgroup.add(new JLabel("Doc Entities"));
	docEntities = new JTextArea(5, 20);
	docEntities.setLineWrap(true);
	docEntities.setEditable(false);
	scrollPane = new JScrollPane(docEntities);
	vgroup.add(scrollPane);

	refreshDetailView();
    }

    /**
     * Gets called by list selection listener
     */
    private void refreshListSelection() {
	int index = documentList.getSelectedIndex();
	if (index >= 0) {
	    selectedDocument = (Document) documents.get(index);
	} else {
	    selectedDocument = null;
	}
	refreshDetailView();
    }

    /**
     * When this is called it forces a refresh of the detail view.
     */
    private void refreshDetailView() {
	if (selectedDocument == null) {
	    docName.setText("No Document Selected");
	    docContent1.setText("{\\rtf1\\ansi\\deff0 <none> }");
	    docEntities.setText("<none>");
	} else {
	    docContent1.setText(controller.getDocumentRTF(selectedDocument));
	    try {
                docContent1.clearHighlights();
                Iterator<Highlight> iter = selectedDocument.highlightIterator();
		while (iter.hasNext()) {
		    Highlight h = iter.next();
		    docContent1.addHighlight(h.start, h.end);
		}
            } catch (BadLocationException ex) {
                System.err.println("Failed to refresh highlights due to BadLocationException");
            }
	    docName.setText(selectedDocument.getName());
	    String temp = "";
	    Iterator<Entity> entities = selectedDocument.iterator();

	    while (entities.hasNext()) {
		Entity e = entities.next();
		temp += e.getName() + "\n";
	    }
	    docEntities.setText(temp.toString());
	}
    }

    /**
     * When this is called it forces a refresh of the list view.
     */
    private void refreshListView() {
	documents.clear();
	Iterator<Document> docs = controller.documentIterator();
	while (docs.hasNext()) {
	    Document d = docs.next();
	    documents.addElement(d);
	}
    }

    /**
     * A new document was added refresh the ListView.
     * @param doc doc added, unused.
     */
    public void documentAdded(Document doc) {
	documents.addElement(doc);
    }

    /**
     * A document was modified. Refresh the list view incase
     * name changed.
     * If this document is selected refresh details view too!
     * @param doc doc modified, unused.
     * @param type the type describing the modification that's been done to the document
     */
    public void documentModified(Document doc, int type) {
	refreshListView();
	if (doc.equals(selectedDocument)) {
	    refreshDetailView();
	}
    }

    /**
     * A document was removed. refresh the list. Also clear details view
     * if that document was selected.
     * @param doc doc removed, unused.
     */
    public void documentRemoved(Document doc) {
	documents.removeElement(doc);
	if (doc.equals(selectedDocument)) {
	    selectedDocument = null;
	    refreshDetailView();
	}
    }

    /**
     *
     * @param ent unused here
     */
    public void entityAdded(Entity ent) {
	refreshDetailView();
    }

    /**
     * Well this entity might be in the doc in used and rather than find out
     * we'll just waste a refresh.
     * @param ent unused here
     * @param type gives information as to what caused the entity modification (unused here)
     */
    public void entityModified(Entity ent, int type) {
	refreshDetailView();
    }

    /**
     * Entity doc added, refresh detail if this is the document.
     * @param ent unused here
     */
    public void entityDocumentAdded(Entity ent, Document doc) {
	if (doc.equals(selectedDocument));
	refreshDetailView();
    }

    /**
     * Well this entity might be in the doc in used and rather than find out
     * we'll just waste a refresh.
     * @param ent unused here
     */
    public void entityRemoved(Entity ent) {
	refreshDetailView();
    }

    public void entityDocumentRemoved(Entity ent, Document doc) {
    }

    public void searchAdded(Search s) {
        
    }

    public void searchRemoved(Search s) {

    }

    public void entitySearchAdded(Entity ent, Search s) {

    }

    public void entitySearchRemoved(Entity ent, Search s) {

    }
}
