/*
 * Connects to a website from a url and extracts text paragraphs.
 */
package starspire.webscraper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Connects to a website from a url and extracts text paragraphs.
 * 
 * @author Nathan Wycoff
 */
public class IEEEHtmlExtractor implements Callable {
    private List<String> urls;
    private int i;
    
    protected IEEEHtmlExtractor(List<String> u) {
        this.urls = u;
        this.i = -1;
    }
        
    public Article call()  {
        i++;
        try {
            return(getWebsiteText(this.urls.get(this.i)));
        } catch (IOException ex) {
            System.out.println("IOException when reaching url at " + urls.get(this.i));
            return(null);
        } 
    }
    /**
     * Opens connection to website, downloads HTML, parses HTML and returns 
     * text paragraphs.
     * 
     * @param Url URL of website to scrape
     * @return A string of the website's text
     * @throws IOException 
     */
    private Article getWebsiteText(String url) throws IOException   {
        
        if (url == null)
        {
            System.out.println("URL: " + url + " was empty.");
            return(null);
        }
        
        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(10*1000).get();
        } catch (HttpStatusException e)   {
            if(e.getStatusCode() == 400)    {
                System.out.println("Bad request from HtmlExtractor (HTTP Status code 400 for sever at " + url + ")");
            }
            else if(e.getStatusCode() == 403)    {
                System.out.println("The server at " + url + " is in a bad mood (HTTP Status code 403)");
            }
            else if(e.getStatusCode() == 404)    {
                System.out.println("This is why everyone uses Google... (HTTP Status code 404 for sever at " + url + ")");
            }
            else
                System.out.println("Non-200 HTTP Response Code: " + e.getStatusCode() + " For server at " + url);
        } catch (UnsupportedMimeTypeException e)    {
            System.out.println("The server at " + url + " sent a pdf/powerpoint/non-html file and was ignored");
        } catch (IllegalArgumentException e)  {
            System.out.println("IllegalArgumentException!!: ");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            System.out.println(url);
        } catch (SocketTimeoutException e)  {
            System.out.println("Server at " + url + " timed out.");
        } catch (ConnectException e) {
            System.out.println("Server at " + url + " is terrible. (Connection Refused)");
        }
        String holder = "";
        
        Elements paragraphs = null;
        Elements titles     = null;
        
        
        if (doc == null)    {
            System.out.println("Anormal return from IEEE callable");
            return(null);
        }
        
        paragraphs = doc.getElementsByClass("article");
        titles = doc.select("h1");
        
        
        String content = paragraphs.get(0).text();
        String title   = titles.get(0).text();
        
        Article ret = new Article();
        ret.setContent(content);
        ret.setTitle(title);
        
        
        
        return(ret);
        
    }
}
