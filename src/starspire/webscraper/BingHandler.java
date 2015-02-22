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
    private DataModel data;
    
    public BingHandler(DataModel d)     {
        this.data = d;
    }
    /**
     * Returns a list of articles given a search string from Yahoo news.
     * @param query
     * @return
     * @throws ParseException
     * @throws IOException 
     */
        public List<Document> getDocuments(String query) {
            String json = this.getBingJson(query);
            
            SERPParser serpParser = new SERPParser();
            HtmlExtractor htmlExtractor = new HtmlExtractor();
            
            List<Document> documents;
            documents = serpParser.parseSERP(json);
            System.out.println("Extracting text from URL HTML");
            
            String content = "";
            
            List<Integer> deletionIndices = new ArrayList<Integer>();
            
            for(Document d : documents)   {
                if (!"".equals(content = htmlExtractor.getWebsiteText(d.getUrl())) )    
                    d.setContent(content);
                else
                    deletionIndices.add(documents.indexOf(d));
            }
            
            for(Integer i : deletionIndices)    {
                documents.remove(i);
            }
            
            return(documents);
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
                    "https://api.datamarket.azure.com/Bing/Search/News?Query=%27" + query + "%27&$top=50&$format=json");
            

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
    

