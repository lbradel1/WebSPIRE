/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starspire.models;

/**
 * This interface describes the requirements to listen to a data model.
 *
 * @author Pat
 */
public interface DataListener {
    /*
     * Document Events
     */
    
    public final static int OTHER = 0;
    public final static int ADDEDNOTE = 1;
    public final static int REMOVEDNOTE = 2;
    public final static int HIGHLIGHT = 3;
    public final static int NOTE = 4;
    public final static int LINK = 5;
    public final static int SEARCH = 6;
    public final static int INCREASED = 7;
    

    /**
     * This event is triggered when a document is added to the DataModel.
     * This happens after loading, loading does not trigger events.
     * @param doc Document that was added.
     */
    public void documentAdded(Document doc);

    /**
     * This event is triggered when a document is modified.
     * This is a general event and could include many different modifications.
     * @param doc Document that was modified.
     * @param type Int giving the type of the modification that's happened
     */
    public void documentModified(Document doc, int type);

    /**
     * This event is triggered when a document is removed.
     *
     * @param doc Document that was removed.
     */
    public void documentRemoved(Document doc);

    /*
     * Entity Events
     */
    /**
     * This event is triggered when an entity is added to the
     * data model. When an entity is added it will usually not have any
     * documents linked to it (unless maybe an undo is called and un-deletes an
     * entity)
     * @param ent
     */
    public void entityAdded(Entity ent);

    /**
     * This event is called when an entity is modified in general.
     * @param ent
     * @param type gives information as to what caused the entity modification
     */
    public void entityModified(Entity ent, int type);

    /**
     * This event is called every time a document is added to an entity.
     * @param ent Entity to which a document is added
     * @param doc Document that was just added to the entity.
     */
    public void entityDocumentAdded(Entity ent, Document doc);
    
    /**
     * This event is called when an entity is removed from a document
     * @param ent Entity being removed
     * @param doc Document from which entity was removed
     */
    public void entityDocumentRemoved(Entity ent, Document doc);

    /**
     * This event is called when an entity is removed.
     * It likely causes many doc modified as side effects of removing an entity.
     * @param ent Entity that's being removed.
     */
    public void entityRemoved(Entity ent);
    
    /**
     * This event is called when a search is added/performed.
     * @param s Search being performed.
     */
    public void searchAdded(Search s);
    
    /**
     * This event is called when a search is removed/deleted.
     * @param s Search being removed/deleted.
     */
    public void searchRemoved(Search s);
    
    public void entitySearchAdded(Entity ent, Search s);

    public void entitySearchRemoved(Entity ent, Search s);
}
