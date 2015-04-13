/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package starspire.webscraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import starspire.models.DataModel;

/**
 *
 * @author 2EEZY4WEEZY
 */
public class FormSubmitter1 {

    private String currentURL = "";

    /**
     * Stores a list of articles into the hidden docs of the Datamodel.
     *
     * @param data
     */
    public void storeArticles(DataModel data, String query) {

        List<Article> articles = this.getIEEEArticles(query);
        
        starspire.models.Document doc = null;
        for (Article a : articles) {
            doc = new starspire.models.Document(a.getContent(), a.getTitle(), a.getUrl());
            data.addDocument(doc);
        }
    }

    /**
     * Use this main to test IEEE services.
     *
     * @param args the command line arguments
     */
    private List<Article> getIEEEArticles(String urlToRead) {


        FormSubmitter1 fs = new FormSubmitter1();
        ArrayList<String> urls = fs.getIEEELinksHtml(fs.getIEEEPageHtml(urlToRead));

        List<Article> ret = fs.getIEEEContent(urls);

        return(ret);

    }

    /**
     * This method returns a string containing the SERP HTML
     *
     * @param urlToRead url of SERP
     * @return A String representation of the URL's HTML
     */
    private String getIEEEPageHtml(String urlToRead) {
        currentURL = urlToRead;
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(urlToRead);
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

        Document doc = Jsoup.parse(html, currentURL);
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
                Logger.getLogger(FormSubmitter1.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(FormSubmitter1.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return (ret);
    }
    /**
     * Returns a list of articles given a search string from Yahoo news.
     *
     * @param query
     * @return
     * @throws ParseException
     * @throws IOException
     */
    /*
     public List<Article> getIEEEArticles(String query){
     String mainPageHtml = this.getIEEEPageHtml(query);
        
     ArrayList<String> urls = this.getIEEELinksHtml(query);
        
        
     List<Article> articles = null;
        
        
        
     System.out.println("Extracting text from URL HTML");
        
     String content = "";
        
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
     */
}
