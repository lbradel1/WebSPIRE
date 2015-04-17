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
     * @throws ParseException 
     */
    public List<Article> parseSERP(String jsonString) throws ParseException
        {
            System.out.println("Parsing JSON from Bing servers...");
            
            List<Article> articles = new ArrayList();
            JSONParser parser = new JSONParser();
            
            Object obj = parser.parse(jsonString);
        
            JSONObject rootNode = (JSONObject) obj;
            JSONObject defaultNode = (JSONObject) rootNode.get("d");
            
            
            JSONArray results = (JSONArray) defaultNode.get("results");
            Iterator<JSONObject> iterator = results.iterator();
            JSONObject e;
            int i = 0;
            while(iterator.hasNext())   {
                e = iterator.next();
                articles.add(new Article());
                articles.get(i).setTitle((String) e.get("Title"));
                articles.get(i).setDescription((String) e.get("Description"));
                articles.get(i).setUrl((String) e.get("Url"));
                i++;
            }
            
            System.out.println("Server returned " + i + " results");
            
            return(articles);
        }
}

        

