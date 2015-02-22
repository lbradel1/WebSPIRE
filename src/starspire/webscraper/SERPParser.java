/*
 * Implementation of SERP JSON parser for StarSPIRE. Creates a list of articles
 * ready for URL HTML parsing.
 */
package starspire.webscraper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import starspire.models.Document;

/**
 * Implementation of SERP JSON parser for StarSPIRE. Creates a list of articles
 * ready for URL HTML parsing.
 * 
 * @author Nathan Wycoff
 */
public class SERPParser {

    /**
     * Parses SERP JSON and stores the data in an Article list for further
     * processing.
     * 
     * @param jsonString string to be parsed
     * @return list of articles with title, summary and URL set.
     */
    public List<Document> parseSERP(String jsonString)
        {
            System.out.println("Parsing JSON from Bing servers...");
            
            List<Document> documents = new ArrayList();
            JSONParser parser = new JSONParser();
            
            Object obj;
            
            try {
            obj = parser.parse(jsonString);
            } catch (ParseException e)    {
                System.out.println("Parsing failure");
                return null;
            }
            JSONObject rootNode = (JSONObject) obj;
            JSONObject defaultNode = (JSONObject) rootNode.get("d");
            
            
            JSONArray results = (JSONArray) defaultNode.get("results");
            Iterator<JSONObject> iterator = results.iterator();
            JSONObject e;
            int i = 0;
            while(iterator.hasNext())   {
                e = iterator.next();
                documents.add(new Document());
                documents.get(i).setName((String) e.get("Title"));
                documents.get(i).setUrl((String) e.get("Url"));
                i++;
            }
            
            System.out.println("Server returned " + i + " results");
            
            return(documents);
        }
}

        

