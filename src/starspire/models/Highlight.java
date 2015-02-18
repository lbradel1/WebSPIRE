package starspire.models;

import java.awt.Color;
import org.json.*;

/**
 * Highlights are stored as a pair of integers delimiting the start and end
 * caret position of a highlight.
 * @author Pat, Lauren
 */
public class Highlight {
    /**
     * Start character index of this highlight
     */
    public int start;
    /**
     * End character index of this highlight
     */
    public int end;
    
    public Color hc;
    
    /**
     * Default Constructor
     * @param s start
     * @param e end
     */
    public Highlight(int s, int e) {
	start = s;
	end = e;
        hc = new Color(230,230,51);
    }
    
    public Highlight(int s, int e, Color color) {
        start = s;
        end = e;
        hc = color;
    }
 
    /**
     * Load from JSONArray
     * @param highlight JSONArray where 0 is start and 1 is end
     * @throws JSONException not in the correct format
     */
    public Highlight(JSONArray highlight) throws JSONException {
	start = highlight.getInt(0);
	end = highlight.getInt(1);
    }

    /**
     * This gets a JSONAarray to make saving faster and easier
     * @return JSONArray where 0 is start and 1 is end
     * @throws JSONException something went wrong
     */
    public JSONArray getJSONObject() throws JSONException {
	JSONArray a = new JSONArray();
	a.put(0,start);
	a.put(1,end);
	return a;
    }

}
