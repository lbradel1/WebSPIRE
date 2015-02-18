package starspire.models;

import starspire.controllers.StarSpireController;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *  Should this implement runnable?
 * How should it handle sleeping and interruption
 * lets say the layout active state is A
 * A is true do layout
 * A is false sleep for few milli secs and check again
 *
 * Other option is to sleep for a while say few seconds and rely on an interrupt
 * to be raised when the layout needs to start again??
 *
 * @author Patrick Fiaux, Alex Endert
 */
public interface GraphLayout extends Runnable {

    /**
     * Set up the graph to use for this layout.
     * @param fs ForceSpireController this 'view' talks too
     */
    public void setForceSpireController(StarSpireController fs);

    /**
     * Start the layout process.
     */
    public void start();

    /**
     * Stop the layout process. The layout will no longer be active and change
     * position of nodes.
     */
    public void stop();

    /**
     * This tells the layout what the size of the graph should be.
     * @param x Width of the graph (in pixels)
     * @param y Height of the graph (in pixels)
     */
    //public void setGraphSize(int x, int y); //Taken out since size was added to the graph model


    /**
     * This returns a JSON object for the current layout with
     * any information it might need such as size or parameters.
     * Each layout implementing this could have it's own JSONobject format.
     * @return JSONObject representing the graph.
     */
    public JSONObject getJSONOjbect() throws JSONException;

    /**
     * This function returns the set of actions for this layout.
     * This includes simple actions like start, stop, pause
     * but also layout specific actions like options or more.
     * @return AbstractAction[] containing this layout's actions
     */
    public ArrayList<AbstractAction> getActions();
}
