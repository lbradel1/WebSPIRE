/*
 * Connects to a website from a url and extracts text paragraphs.
 */
package starspire.webscraper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
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
public class HtmlExtractor {
    /**
     * Opens connection to website, downloads HTML, parses HTML and returns 
     * text paragraphs.
     * 
     * @param Url URL of website to scrape
     * @return A string of the website's text
     * @throws IOException 
     */
    public String getWebsiteText(String url) throws IOException   {
        
        if (url == null)
        {
            System.out.println("URL: " + url + " was empty.");
            return("INVALID URL");
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
        
        if (doc != null)    {
        Elements paragraphs = doc.select("p");
        for(Element p : paragraphs)
            holder += p.text();
        }
        
        
        return(holder);
    }
}
