package starspire.views;

/*
 * Imports
 */
import starspire.models.Entity;
import starspire.models.DataListener;
import starspire.models.Document;
import starspire.models.Search;
import starspire.controllers.StarSpireController;
import javax.swing.event.ListSelectionEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import javax.swing.event.ListSelectionListener;

/**
 * The EntityViewer2 will be a view for the entities.
 * It lists the entities in the models and provide user interface for adding,
 * removing and modifying them.
 * @author Patrick Fiaux, Alex Endert
 */
public class EntityViewer extends JFrame implements DataListener {

    private StarSpireController controller;
    private Entity selectedEntity;

    private static enum SortType {

        ABC, WEIGHT
    };
    private SortType sortBy = SortType.WEIGHT; //default is weight for sort

    /*
     * UI components
     */
    JTextField entName;
    JTextField entStrength;
//    JList<Entity> entityList; JAVA 7 only
//    DefaultListModel<Entity> entities;
    JList entityList;
    DefaultListModel entities;
    JTextArea entDocuments;
    JButton addEntity;
    JButton removeEntity;
    JButton abcSort;
    JButton weightSort;
    JTextField search;

    /**
     * Default constructor, creates a new document viewer for a given data
     * model.
     * @param model Model to use in this viewer.
     */
    public EntityViewer(StarSpireController fsc) {
        controller = fsc;
        selectedEntity = null;

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
        setTitle("Entity Viewer");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        /*
         * Start with north
         */
        Box group = Box.createHorizontalBox();
        getContentPane().add(group, BorderLayout.NORTH);
        addEntity = new JButton("+");
        addEntity.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String name;
                name = (String) JOptionPane.showInputDialog(
                        this,
                        "Please enter a name for the new Entity:");
                if (controller.hasEntity(name)) {
                    // The entity exists, do nothing?
                    System.out.println(name + " already exists! Doing nothing...");
                } else {
                    //create the entity, and let the parsing begin
                    System.out.println("Adding entity: " + name);
                    //controller.addEntity(name, true);
                    //FOR THE STUDY, THIS WILL NOT CREATE SOFT DATA ENTITIES
                    //GO BACK AND CHANGE THIS LATER
                    controller.addEntity(name, false);
                }
            }
        });
        group.add(addEntity);

        removeEntity = new JButton("-");
        removeEntity.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectedEntity != null) {
                    controller.removeEntity(selectedEntity);
                }
                selectedEntity = null;
                refreshDetailView();
            }
        });
        group.add(removeEntity);

        abcSort = new JButton("abc");
        abcSort.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sortBy = SortType.ABC;
                refreshListView();
            }
        });
        group.add(abcSort);

        weightSort = new JButton("weight");
        weightSort.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sortBy = SortType.WEIGHT;
                refreshListView();
            }
        });
        group.add(weightSort);

        group.add(new JLabel("Search:"));
        search = new JTextField("not yet working");
        search.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                //Do something here
            }
        });
       // group.add(search);

        /*
         * Add List section
         */
        entities = new DefaultListModel();
        entityList = new JList(entities);
        entityList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                refreshListSelection();
            }
        });
        entityList.setVisibleRowCount(10);
        JScrollPane scrollPane = new JScrollPane(entityList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        /*
         * Add detailed section at the bottom
         */
        group = Box.createHorizontalBox();
        getContentPane().add(group, BorderLayout.SOUTH);
        Box vgroup = Box.createVerticalBox();
        group.add(vgroup);
        vgroup.add(new JLabel("Entity Name"));
        entName = new JTextField(10);
        entName.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectedEntity != null) {
                    String value = entName.getText();
                    controller.setEntityName(selectedEntity, value);
                }
            }
        });
        vgroup.add(entName);
        vgroup.add(new JLabel("Entity Strength"));
        entStrength = new JTextField(10);
        entStrength.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (selectedEntity != null) {
                    double value = Double.parseDouble(entStrength.getText());
                    controller.setEntityStrength(selectedEntity, value, DataListener.OTHER);
                }
            }
        });
        scrollPane = new JScrollPane(entStrength);
        vgroup.add(scrollPane);
        vgroup = Box.createVerticalBox();
        group.add(vgroup);
        vgroup.add(new JLabel("Entity Docs"));
        entDocuments = new JTextArea(5, 20);
        entDocuments.setLineWrap(true);
        entDocuments.setEditable(false);
        scrollPane = new JScrollPane(entDocuments);
        vgroup.add(scrollPane);

        refreshDetailView();
    }

    /**
     * Gets called by list selection listener
     */
    private void refreshListSelection() {
        int index = entityList.getSelectedIndex();
        if (index >= 0) {
            selectedEntity = (Entity) entities.get(index);
        } else {
            selectedEntity = null;
        }
        refreshDetailView();
    }

    /**
     * When this is called it forces a refresh of the detail view.
     */
    private void refreshDetailView() {
        if (selectedEntity == null) {
            entName.setText("No Document Selected");
            entStrength.setText("<none>");
            entDocuments.setText("<none>");
        } else {
            entStrength.setText("" + String.format("%.2f", selectedEntity.getStrength()));
            entName.setText(selectedEntity.getName());
            String temp = "";
            Iterator<Document> documents = selectedEntity.iterator();

            while (documents.hasNext()) {
                Document d = documents.next();
                temp += d.getName() + "\n";
            }
            entDocuments.setText(temp.toString());
        }
    }

    /**
     * When this is called it forces a refresh of the list view.
     * This also sorts the list based on sortBy SortType
     */
    private void refreshListView() {
        entities.clear();
        Iterator<Entity> ents = controller.entityIterator();
        ArrayList<Entity> entList = new ArrayList<Entity>();

        while (ents.hasNext()) {
            entList.add(ents.next());
        }

        switch (sortBy) {
            case WEIGHT:
                Collections.sort(entList, new Comparator<Entity>() {

                    public int compare(Entity a, Entity b) {
                        return Double.compare(b.getStrength(), a.getStrength());
                    }
                });

                for (Entity e : entList) {
                    entities.addElement(e);
                }
                break;
            case ABC:
                Collections.sort(entList, new Comparator<Entity>() {

                    public int compare(Entity a, Entity b) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });

                for (Entity e : entList) {
                    entities.addElement(e);
                }
                break;
        }
    }

    /**
     * A new document was added refresh the detail view.
     * @param doc doc added, unused.
     */
    public void documentAdded(Document doc) {
        refreshDetailView();
    }

    /**
     * A document was modified. Refresh the detail view incase
     * name changed.
     * @param doc doc modified, unused.
     * @param Int the type of the modification that's been done to the document
     */
    public void documentModified(Document doc, int type) {
        refreshDetailView();
    }

    /**
     * A document was removed. refresh the detail view.
     * @param doc doc removed, unused.
     */
    public void documentRemoved(Document doc) {
        refreshDetailView();
    }

    /**
     * An entity was added, add it to the list view.
     * @param ent unused here
     */
    public void entityAdded(Entity ent) {
        entities.addElement(ent);
        refreshListView();
    }

    /**
     * Well this entity might be in the doc in used and rather than find out
     * we'll just waste a refresh.
     * @param ent unused here
     * @param type gives information as to what caused the entity modification (unused here)
     */
    public void entityModified(Entity ent, int type) {
        if (ent.equals(selectedEntity)) {
            refreshDetailView();
        }
        refreshListView();
    }

    /**
     * Entity doc added, refresh detail if this is the document.
     * @param ent unused here
     */
    public void entityDocumentAdded(Entity ent, Document doc) {
        if (ent.equals(selectedEntity)) {
            refreshDetailView();
        }
    }

    /**
     * Entity Removed. Take it out of the list and refresh detail view
     * if it was selected.
     * @param ent unused here
     */
    public void entityRemoved(Entity ent) {
        entities.removeElement(ent);
        if (ent.equals(selectedEntity)) {
            selectedEntity = null;
            refreshDetailView();
        }
        refreshListView();
    }

    /**
     * Unused Event Here
     * @param ent
     * @param doc
     */
    public void entityDocumentRemoved(Entity ent, Document doc) {
    }

    /**
     * Unused Event Here
     * @param s
     */
    public void searchAdded(Search s) {
    }

    /**
     * Unused Event Here
     * @param s
     */
    public void searchRemoved(Search s) {
    }

    /**
     * Unused Event Here
     * @param ent
     * @param s
     */
    public void entitySearchAdded(Entity ent, Search s) {
    }

    /**
     * Unused Event Here
     * @param ent
     * @param s
     */
    public void entitySearchRemoved(Entity ent, Search s) {
    }
}
