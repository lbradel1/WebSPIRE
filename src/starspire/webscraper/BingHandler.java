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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            data.addDocument(doc);
        }
    }
    
    /**
     * Returns a list of articles given a search string from Yahoo news.
     * @param query
     * @return
     * @throws ParseException
     * @throws IOException 
     */
        public List<Article> getArticles(String query){
            String json = this.getBingJson(query);
            
            SERPParser serpParser = new SERPParser();
            
            List<Article> articles = null;
            try {
                articles = serpParser.parseSERP(json);
            } catch (ParseException ex) {
                System.out.println("ParseException Detected");
            }
                System.out.println("Extracting text from URL HTML");
            
            String content = "";
            List<String> urls = new ArrayList<String>();
            
            Iterator<Article> iter = articles.iterator();
            
            while(iter.hasNext())   {
                Article current = iter.next();
                urls.add(current.getUrl());
            }
            
            HtmlExtractor htmlExtractor = new HtmlExtractor(urls);
            iter = articles.iterator();
            ExecutorService executorService = Executors.newCachedThreadPool();
            
            List<Future> futures = new ArrayList<Future>();
            
            while(iter.hasNext())   {
                Article current = iter.next();
                futures.add(executorService.submit(htmlExtractor));
            }
            
            iter = articles.iterator();
            
            try {
                
                for (int i = 0; iter.hasNext(); i++) {
                    Article current = iter.next();
                
                    content = (String)futures.get(i).get();
                    if(content != null && content != "" && !content.isEmpty())
                        current.setContent(content);
                    else    {
                        iter.remove();
                    }
                }
            } catch (InterruptedException e)    {
                System.out.println(e.toString());
            } catch (ExecutionException e)  {
                System.out.println(e.toString());
            } catch (CancellationException e )  {
                System.out.println(e.toString());
            }
            
            
            System.out.println(Integer.toString(articles.size()) + " good documents extracted.");
            
            return(articles);
        }
        
        
        
    public String getBingJson(String query)    {
        String ret = "";
        query = query.replaceAll("\\s+", "%20");
        String accountKey = "owiQTgpl8LRi4KfsVvAPuRtdon0QKq1fcTsBv/JD+O8=";//PLACE ACCOUNT KEY HERE
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
    
        public boolean testIntegrity(DataModel data, List<Article> articles)   {
            List<String> missingDocs;
            List<String> docs = new ArrayList<String>();
            
            
            Iterator it = data.documentIterator();
            
            while(it.hasNext()) {
                Document current = (Document)it.next();
                try{
                    docs.add(current.getName());
                
                
                if (current.getContent().isEmpty() || current.getContent() == "")   {
                    System.out.println("FAILURE in document title " + current.getName());
                    return(false);
                }
                } catch (NullPointerException e)  {
                    //System.out.println("Exception thrown for document number " it.)
                }
            }
            
            for(Article a : articles)  {
                if(!docs.contains(a.getTitle()))    {
                    System.out.println("Could not find " + a.getTitle());
                    return(false);
                }
            }
            
            
            return(true);
        }
                
    }