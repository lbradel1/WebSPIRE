/*
 * Convenient for holding informatino about an article.
 */
package starspire.webscraper;

/**
 *Contains information about an article
 * 
 * @author Nathan Wycoff
 */
public class Article extends Object{
    private String title;
    private String description;
    private String url;
    private String content;
    
    //Setting Operations
    /**
     * Sets the articles title
     * @param newTitle Title to be set
     */
    public void setTitle(String newTitle)  {
        this.title = newTitle;
    }
    /**
     * Sets the articles description
     * Summary is called "abstract" by the server, but cannot be typed as such 
     * in java as it is a protected word.
     * 
     * @param newSummary description to be set
     */
    public void setDescription(String newSummary)   {
        this.description = newSummary;
    }
    /**
     * Sets the articles URL
     * @param newUrl URL to be set
     */
    public void setUrl(String newUrl)  {
        this.url = newUrl;
    }
    /**
     * Sets the articles content
     * @param newContent String to make new content
     */
    public void setContent(String newContent)    {
        this.content = newContent;
    }
    
    //Getting Operations
    /**
     * Returns the current title of the article
     * @return the current title
     */
    public String getTitle()    {
        return(this.title);
    }
    /**
     * Returns the current description of the article
     * Summary is called "abstract" by the server, but cannot be typed as such 
     * in java as it is a protected word.
     * 
     * @return the current description
     */
    public String getDescription()  {
        return(this.description);
    }
    /**
     * Returns the current URL of the article
     * @return the current URL
     */
    public String getUrl()  {
        return(this.url);
    }
    /**
     * Returns the article's entire body as a String of paragraphs.
     * @return whole body.
     */
    public String getContent()    {
        return(this.content);
    }
       
}
