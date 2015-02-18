package starspire.controllers;

import starspire.models.Entity;
import starspire.models.Document;
import starspire.models.DataModel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the parsing handler for extracting jigsaw files.
 * @author Patrick Fiaux, Alex Andert
 */
public class JigsawSAXHandler extends DefaultHandler {

    private final static int STEP = 5;
    private final static boolean USE_ENTITIES = true;
    private boolean bdocid = false;
    private boolean bdocdate = false;
    private boolean bdocsource = false;
    private boolean bdoctext = false;
    private boolean bdocentity = false;
    private boolean bdocyear = false;
    private String docTitle = "default";
    private String docContent = "default";
    private String docYear = "";
    private ArrayList<String> entityStringList = new ArrayList<String>();
    private Parser parser;
    private DataModel data;
    private int documentCount;
    private int skip;
    //newline constant for parsing
    private String newline = System.getProperty("line.separator");

    /**
     * Default Constructor
     * @param d DataModel to add documents and entities to...
     */
    public JigsawSAXHandler(DataModel d) {
        super();
        data = d;
        parser = new Parser();
        documentCount = 0;
        skip = STEP;
    }

    /**
     * Callback at the start of an element here we figure out if it's something
     * we need to track.
     * @param uri
     * @param localName
     * @param qName
     * @param attributes
     * @throws SAXException
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        //System.out.println("start element: " + qName);

        if (qName.equalsIgnoreCase("document")) {
            //NORTHING
        } else if (qName.equalsIgnoreCase("documents")) {
            //Nothing
        } else if (qName.equalsIgnoreCase("docID")) {
            bdocid = true;
        } else if (qName.equalsIgnoreCase("docDate")) {
            bdocdate = true;
        } else if (qName.equalsIgnoreCase("docSource")) {
            bdocsource = true;
        } else if (qName.equalsIgnoreCase("docText")) {
            bdoctext = true;
            docContent = "";
        } else if (qName.equalsIgnoreCase("year")) {
            bdocyear = true;
        } else {
            //has to be an entity
            bdocentity = true;
        }
    }

    /**
     * callback at closing tags. Here if it's a document we take all the stuff
     * we got so for and stick it in the model.
     * @param uri
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String uri, String localName,
            String qName) throws SAXException {
        //Parser parse = parser;//new Parser();

        if (qName.equalsIgnoreCase("docText")) {
            bdoctext = false;
            //System.out.println("content: " + docContent);
        } else if (qName.equalsIgnoreCase("document")) {

            //if (docYear.contains("2011") || docYear.contains("2010")) {
            if (true) { //just use them all
                //Document doc = data.addDocument(docContent, docTitle);
                
                Document doc = new Document(docContent, docTitle);
                if(docTitle.compareTo("chinchilladreamin3") == 0) {
                    System.out.println(docContent);
                }

                //add all the entities
                for (String ent : entityStringList) {
                    if (USE_ENTITIES) {
                        //add only the entities that are actually in the text of the document
                        //System.out.print("Jigsaw import linking " + entityStringList.size() + " entities.");
                        if (docContent.contains(ent) && !parser.isStopWord(ent)) {
                            Entity e = data.getEntity(ent);
                            if (!doc.hasEntity(e)) {
                                //adds Entity  to list in Doc, Doc to list in Entity
                                //TODO: if already in list, does nothing right now. Should add weight?
                                //System.out.println("linking");
                                doc.addEntity(e);
                                
                                
                                //data.link(e, doc);
                                System.out.print(".");
                            }
                        } else {
                            //this documentContent does not contain the entity, do nothing?
                        }
                    }
                }
                
                data.addHiddenDocument(doc);
                //data.removeDocument(doc);
                //data.addDocument(doc);
                
                
                entityStringList.clear(); //reset for next set of entities
                
                documentCount++;
                
                if (documentCount >= skip) {
                    skip += STEP;
                    System.out.print(".");
                }
                System.out.println("next doc!");
                
                /*try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JigsawSAXHandler.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            }
        }
        
    }

    /**
     * Callback from element content. Here we check if we're tracking something
     * if we are we do what we need with it.
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        if (bdocid) {
            //System.out.println("docTitle: " + new String(ch, start, length));
            docTitle = new String(ch, start, length);
            System.out.println("Jigsaw import found doc: " + docTitle);
            bdocid = false;
        } else if (bdoctext) {
            //System.out.println("docContent: " + new String(ch, start, length));
            //docContent += newline;
            //docContent += "";
            String tempContent = new String(ch, start, length);
            tempContent = tempContent.replaceAll(newline, "     ");
            //tempContent = tempContent.replaceAll(newline, "\n");
            docContent += tempContent;
        } else if (bdocdate) {
            //just skip this field
            bdocdate = false;
        } else if (bdocsource) {
            //just skip this field
            bdocsource = false;
        } else if (bdocyear) {
            //this is the year for the document
            docYear = new String(ch, start, length);
            bdocyear = false;
        } else if (bdocentity) {
            String ent = new String(ch, start, length);

            //remove ending punctuation from jigsaw files
            ent = parser.removeStartEndPunctuation(ent);
            //System.out.println("Jigsaw import found entity: " + ent);
            entityStringList.add(ent);
            bdocentity = false;
        }
    }
}
