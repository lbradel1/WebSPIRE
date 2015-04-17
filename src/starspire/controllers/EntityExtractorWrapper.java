package starspire.controllers;

import edu.vt.entityextractor.*;
import java.util.*;

/**
 * EntytiExtractorWrapper is the single class way that we interface with the EntityExtractor
 * from Christopher Andrews.
 * @author Patrick Fiaux, Alex Endert
 */
public class EntityExtractorWrapper {

    private static EntityExtractor e = null;

    /**
     * Constructor to create an EntityExtractorWrapper
     */
//    public EntityExtractorWrapper() {
//        setup();
//    }
    /**
     * Helper for the constructor of the EntityExtractor
     */
    public synchronized static void setup() {
        if (e == null) {
            e = new EntityExtractor();
            e.loadKnownAltRecognizers();
        }
    }

    /**
     * This method will extract all entities from a given string.
     * @param s String from which entities are extracted
     * @return ArrayList<String> all the entities extracted
     */
    public synchronized static ArrayList<String> extractEntities(String s) {
        ArrayList<String> entityList = new ArrayList<String>();

        if (e == null) {
            setup();
        }

        List<Entity> ents = e.extractEntities(s);

        for (Entity ent : ents) {
            //System.out.println(ent.getText());
            entityList.add(ent.getText());
        }

        return entityList;
    }
}