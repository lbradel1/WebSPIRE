package starspire.webscraper;


import org.apache.commons.codec.binary.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import starspire.models.DataModel;
import starspire.models.Document;

/*
 * Allows the use of Microsoft Azure Market Bing API
 */

/**
 *
 * @author 2EEZY4WEEZY
 */
public class WebHandler {
    /**
     * Stores a list of articles into the hidden docs of the Datamodel.
     * @param data 
     */
    public void retrieveBingArticles(DataModel data,String query)   {
        
        List<Article> articles = this.getBingArticles(query);
        
        Document doc = null;
        for(Article a : articles)   {
            doc = new Document(a.getContent(), a.getTitle(), a.getUrl());
            data.addDocument(doc);
        }
    }
    private String currentURL = "";

    /**
     * Stores a list of articles into the hidden docs of the Datamodel.
     *
     * @param data
     */
    public void retrieveIEEEArticles(DataModel data, String query) {

        List<Article> articles = this.getIEEEArticles(query);
        
        starspire.models.Document doc = null;
        for (Article a : articles) {
            doc = new starspire.models.Document(a.getContent(), a.getTitle());
            data.addDocument(doc);
        }
    }

    /**
     * Use this main to test IEEE services.
     *
     * @param args the command line arguments
     */
    private List<Article> getIEEEArticles(String query) {

        System.out.println("Opening Connection to IEEE servers...");
        ArrayList<String> urls = this.getIEEELinksHtml(this.getIEEEPageHtml(query));

        System.out.println("Retrieving IEEE Abstract HTML...");;
        List<Article> ret = this.getIEEEContent(urls);

        return(ret);

    }
    
    /**
     * Returns a list of articles given a search string from Yahoo news.
     * @param query
     * @return
     * @throws ParseException
     * @throws IOException 
     */
        private List<Article> getBingArticles(String query){
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
            
            BingHtmlExtractor htmlExtractor = new BingHtmlExtractor(urls);
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
        
     /**
     * This method returns a string containing the SERP HTML
     *
     * @param urlToRead url of SERP
     * @return A String representation of the URL's HTML
     */
    private String getIEEEPageHtml(String urlToRead) {
        try {
            urlToRead = URLEncoder.encode(urlToRead, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            System.out.println("URL Encoding Failure");
            ex.printStackTrace();
        }
        currentURL = "http://ieeexplore.ieee.org/search/searchresult.jsp?newsearch=true&queryText=" + urlToRead;
        
        
        
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(currentURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (IOException e) {
        }
        return (result);
    }

    /**
     * Parses html from a SERP page to get the links embedded therein.
     *
     * @param html html to parse
     * @return An arraylist of Strings, each string being a URL.
     */
    private ArrayList<String> getIEEELinksHtml(String html) {

        org.jsoup.nodes.Document doc = Jsoup.parse(html, currentURL);
        Elements hTags = doc.select("h3");

        ArrayList<Elements> aTags = new ArrayList<Elements>();



        for (Element e : hTags) {
            aTags.add(e.getElementsByTag("a"));
        }


        ArrayList<String> ret = new ArrayList<String>();

        for (Elements e : aTags) {
            for (Element e1 : e) {
                ret.add(e1.attr("abs:href"));
            }
        }

        Iterator it = ret.iterator();

        while (it.hasNext()) {
            String myString = (String) it.next();
            if (!myString.contains("articleDetails")) {
                it.remove();
            }
        }
        return (ret);
    }

    private ArrayList<Article> getIEEEContent(ArrayList<String> urls) {
        IEEEHtmlExtractor extractor = new IEEEHtmlExtractor(urls);

        ExecutorService executorService = Executors.newCachedThreadPool();

        ArrayList<Future> futures = new ArrayList<Future>();

        for (String url : urls) {
            futures.add(executorService.submit(extractor));
        }
        
        ArrayList<Article> ret = new ArrayList<Article>();
        
        for (Future f : futures) {
            try {
                ret.add((Article) f.get());
            } catch (InterruptedException ex) {
                System.out.println("Error in Futures");
            } catch (ExecutionException ex) {
                System.out.println("Error in Futures");
            }
        }
        return (ret);
    }
                
    }