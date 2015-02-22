package starspire.webscraper;


import org.apache.commons.codec.binary.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.parser.ParseException;
import starspire.models.DataModel;
import starspire.models.Document;

/*
 * Allows the use of Microsoft Azure Market Bing API
 */

/**
 *
 * @author 2EEZY4WEEZY
 */
public class BingHandler {
    /**
     * Stores a list of articles into the hidden docs of the Datamodel.
     * @param data 
     */
    public void storeArticles(DataModel data,List<Article> articles)   {
        Document doc = null;
        for(Article a : articles)   {
            doc = new Document(a.getContent(), a.getTitle(), a.getUrl());
            data.addHiddenDocument(doc);
        }
    }
    
    /**
     * Returns a list of articles given a search string from Yahoo news.
     * @param query
     * @return
     * @throws ParseException
     * @throws IOException 
     */
        public List<Article> getArticles(String query) throws ParseException, IOException{
            String json = this.getBingJson(query);
            
            SERPParser serpParser = new SERPParser();
            HtmlExtractor htmlExtractor = new HtmlExtractor();
            
            List<Article> articles;
            articles = serpParser.parseSERP(json);
            System.out.println("Extracting text from URL HTML");
            
            String content = "";
            
            List<Integer> deletionIndices = new ArrayList<Integer>();
            
            for(Article a : articles)   {
                if ((content = htmlExtractor.getWebsiteText(a.getUrl())) != "" )    
                    a.setContent(content);
                else
                    deletionIndices.add(articles.indexOf(a));
            }
            
            for(Integer i : deletionIndices)    {
                articles.remove(i);
            }
            
            return(articles);
        }
        
        
        
    public String getBingJson(String query)    {
        String ret = "";
        query = query.replaceAll("\\s+", "%20");
        String accountKey = "owiQTgpl8LRi4KfsVvAPuRtdon0QKq1fcTsBv/JD+O8=";
        byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
        String accountKeyEnc = new String(accountKeyBytes);
        URL url;
        try {
            url = new URL(
                    "https://api.datamarket.azure.com/Bing/Search/Web?Query=%27" + query + "%27&$top=50&$format=json");
            

            System.out.println("Opening connection to Bing servers...");
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);     
        
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuilder sb = new StringBuilder();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }

            ret = sb.toString();
            
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }    
        
            if(ret!="") {
                System.out.println("SERP JSON download successful.");
            }
            else    {
                System.out.println("FATAL ERROR: Bing servers returned empty string. Exiting...");
                System.exit(-1);
            }
            return ret;
        }
    }
    

