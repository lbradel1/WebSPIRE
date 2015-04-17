package starspire.models;

import starspire.StarSpireApp;
import starspire.controllers.StarSpireController;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.json.*;

/**
 * ElasticLayout is what we found to work best for a dynamic layout of our graph.
 *
 * It works by calculating repulsive forces between nodes and then attractive
 * forces from edges and moves nodes by the resulting force.
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class WeightedElasticLayout implements GraphLayout, GraphListener {

    private static final Logger logger = Logger.getLogger(StarSpireApp.class.getName());
    /**
     * Maximum thread sleep time, this is how long the thread will sleep until it
     * checks for changes again.
     */
    private final static int THREAD_SLEEP_TIME = 5000;
    /**
     * max time entire layout should take to make one iteration.
     */
    private final static int MAX_ITERATION_TIME = 65;
    /**
     * max number of iterations
     */
    private final static int MAX_NUM_OF_ITERATIONS = 600;//600;
    /**
     * min time entire layout should take in one iteration
     */
    private final static int MIN_ITERATION_TIME = 10;
    /**
     * higher value spaces documents more based on diameter of node
     */
    private final static int MAX_REPEL_MULTIPLIER = 10;
    /**
     * This delays the start of the cooling process by a number of iterations.
     * The cooling will be COOLING_FACTOR_MAXIMUM until the numOfIterations
     * reaches at least COOLING_START_DELAY iterations
     */
    private final static int COOLING_START_DELAY = 0;//100;//1500; //was 100, but I went to 1500 for LHRD
    /*
     * This is how long it takes for the cooling to reach maximum.
     * After this number of iteration the cooling will reach 0 and stop all nodes.
     */
    private double COOLING_DIVIDER = 100;//200;//800; //was 200, changed for LHRD
    /**
     * This makes sure cooling doesn't get lower than this (because negative cooling
     * would be bad (it would basically make nodes slowly accelerate in the opposite
     * direction).
     */
    private final static double COOLING_FACTOR_MINIMUM = 0.3;
    /**
     * This is a flag to either enable (TRUE) of disable (FALSE) the cooling feature.
     */
    private final static boolean IS_COOLING = true;
    /**
     * This is the maximum amount of distance a node can move per "turn"
     */
    private final static double MAX_DIST_PER_MOVE = 8.0;
    /**
     * Spacing constant. Multiplier for repulsive forces
     */
    private final static double SPACING = 3;
   
    private final static double MIN_VX = 2.0;
    private final static double MIN_VY = 2.0;
    
    private final static double CONVERGENCE_THRESHOLD = 10;
    
    private int width, height;
    private volatile boolean active;
    private volatile boolean stable;
    private StarSpireController controller;
    private Thread runner;
    private volatile boolean idleing;
    private long startTime, stopTime;
    private int sleepInterval;
    private boolean pauseState;
    private ArrayList<AbstractAction> actions;
    private AbstractAction pause;

    /**
     * Default constructor.
     *
     * @param nwidth width of layout
     * @param nheight height of layout
     */
    public WeightedElasticLayout(int nwidth, int nheight) {
        setup(nwidth, nheight, false);
    }

    /**
     * Load constructor that takes a JSONObject containing the layouts data.
     * @param nwidth width of layout
     * @param nheight height of layout
     * @param jsono JSON Object with data
     * @throws JSONException data in incorrect format.
     */
    public WeightedElasticLayout(int nwidth, int nheight, JSONObject jsono) throws JSONException {
        boolean p;
        try {
            String className = jsono.getString("algorithm");
            if (className.equals(this.getClass().getName())) {
            } else {
                System.err.print("JSON layout settings from incompatible class: " + className);
            }
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error loading JSON algorithm", e);
            System.err.println(e);
        }

        try {
            p = jsono.getBoolean("PauseState");
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error loading JSON pause", e);
            System.err.println("PauseSate missing from saved file!"
                    + " Reverting to default values");
            p = false;
        }

        setup(nwidth, nheight, p);

    }

    /**
     * Helper with the constructor to setup new object
     * @param w width of layout
     * @param h height of layout
     */
    private void setup(int w, int h, boolean pause) {
        active = false;
        stable = false;
        idleing = false;
        controller = null;
        pauseState = pause;
        width = w;
        height = h;
        sleepInterval = 100;
        pauseState = pause;

        initThread();
        initActions();
    }

    /**
     * This initializes the runner thread.
     */
    private void initThread() {
        if (runner == null || !runner.isAlive()) {
            runner = new Thread(this, "WeightedElasticLayoutThread");
            stable = false;
            idleing = false;
            active = false;
        } else {
            System.err.println("WeightedElasticLayoutThread Error: Trying to Re-initialize thread");
        }
    }

    /**
     * Initializes the actions for this kind of layout.
     */
    private void initActions() {
        actions = new ArrayList<AbstractAction>();
        pause = new AbstractAction("Pause") {

            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        };
        pause.putValue("KeyEventCode", KeyEvent.VK_PERIOD); //tells the app pause should be the accelerator.
        //pause.putValue("KeyEventCTRL", false); //tells the app not to use a modifier no ctrl or cmd
        actions.add(pause);

        AbstractAction action = new AbstractAction("Reset") {

            public void actionPerformed(ActionEvent e) {
                reset();
            }
        };
        //no keyboard shortcut
        actions.add(action);

        action = new AbstractAction("About WeightedElasticLayout") {

            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,
                        "The WeightedElasticLayout is loosely based on Force Directed Layouts. It does cool stuff with the weights tho.",
                        "About WeightedElasticLayout", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        actions.add(action);
    }

    /**
     * This starts the layout thread, it does not run before this.
     * Note this is a background check it doesn't stop when done it just sleeps
     * until it gets more work.
     */
    public void start() {

        if (controller != null && !runner.isAlive() && !pauseState) {
            active = true;
            runner.start();
        }
    }

    /**
     * This stops the layout if it's running, if it's not running it does nothing...
     *
     */
    public void stop() {
        if (runner.isAlive()) {
            try {
                active = false;
                if (idleing) {
                    runner.interrupt();
                }
                System.out.println("Thread " + Thread.currentThread().getName()
                        + " Ordering thread layout thread to stop.");
                runner.join();
                initThread();
            } catch (InterruptedException ex) {
                System.err.println("Layout Thread failed to stop");
                Logger.getLogger(WeightedElasticLayout.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * pause helper
     */
    private void togglePause() {
        if (pauseState) {
            pause.putValue(AbstractAction.NAME, "Pause");
            pauseState = false;
            start();
        } else {
            pause.putValue(AbstractAction.NAME, "Resume"); //switch the name / action pause resume...
            pauseState = true;
            stop();
        }
    }
    
    private void forceDirectedLayout()
    {
        int numOfIterations = 0;
        
        int numOfNode = 0;
        Iterator<Node> nodeList = controller.getNodeIterator();
        while(nodeList.hasNext())
        {
            numOfNode++;
            nodeList.next();
        }
        
        float rep_force[] = new float[numOfNode*2];
        float att_force[] = new float[numOfNode*2];
        //float node_pos[] = new float[numOfNode*2];
        
        Map<Node, List<Integer>> node_pos = new HashMap<Node, List<Integer>>();
        
        int index = 0;
        double vect[] = new double[2];
        double norm;
        double scalar;
        double averageEdgeLength = 0.01;
        double temp;
        while(numOfIterations < 600)
        {
            //copy the position information;
            numOfIterations++;
            averageEdgeLength += 1.0;//Math.log(numOfIterations);
            index = 0;
            nodeList = controller.getNodeIterator();
            
            nodeList = controller.getNodeIterator();
            while(nodeList.hasNext())
            {
                Node outer_node = nodeList.next();
                rep_force[2*index] = 0;
                rep_force[2*index+1] = 0;
                att_force[2*index] = 0;
                att_force[2*index+1] = 0;
                    
                //calculate the attractive force
                ArrayList<Edge> edgeList = outer_node.getEdgeList();
                Iterator<Edge> edge_iterator = edgeList.iterator();
                int numOfEdgesWeight = 10 * (int) Math.round(edgeList.size() + SPACING);
                while(edge_iterator.hasNext())
                {
                    Edge edge = edge_iterator.next();
                    Node adjNode;
                    if(edge.getNode1() == outer_node)
                        adjNode = edge.getNode2();
                    else
                        adjNode = edge.getNode1();
                    
                    vect[0] = outer_node.getX() - adjNode.getX();
                    vect[1] = outer_node.getY() - adjNode.getY();
                    
                    norm = Math.sqrt(vect[0]*vect[0] + vect[1]*vect[1]);
                    if(norm == 0.0)
                        continue;
                    temp = norm/averageEdgeLength;
                    scalar = Math.log(temp)/Math.log(2.0);
                    scalar = scalar*norm/(averageEdgeLength*averageEdgeLength*averageEdgeLength);
                    if(scalar < 0.0)
                        scalar = 0.0;
                    
                    scalar *= numOfEdgesWeight;
                    
                    vect[0] *= (scalar*100);
                    vect[1] *= (scalar*100);
                    
                    att_force[2*index] -= vect[0];
                    att_force[2*index+1] -= vect[1];
                }
                
                //calculate the repulsive force
                Iterator<Node> inner = controller.getNodeIterator();
                while(inner.hasNext())
                {
                    Node inner_node = inner.next();
                    
                    vect[0] = outer_node.getX() - inner_node.getX();
                    vect[1] = outer_node.getY() - inner_node.getY();
                    norm = Math.sqrt(vect[0]*vect[0] + vect[1]*vect[1]);
                    if(inner_node == outer_node)
                        continue;
                    else if(Math.abs(vect[0]) < 0.01 && Math.abs(vect[1]) < 0.01)
                    {
                        vect[0] = Math.random();
                        vect[1] = Math.random();
                        norm = 0.001;//Math.sqrt(vect[0]*vect[0] + vect[1]*vect[1]);
                    }
                    
                    scalar = 1.0/(norm*norm*norm);
                    vect[0] *= (scalar);
                    vect[1] *= (scalar);
                    
                    rep_force[2*index] += vect[0];
                    rep_force[2*index+1] += vect[1];
                }
                
                index++;
            }
            
            
            //copy back to node information
            index = 0;
            nodeList = controller.getNodeIterator();
            while(nodeList.hasNext())
            {
                 Node outer_node = nodeList.next();
                 if(!controller.isSelected(outer_node)) {
	                 vect[0] = att_force[2*index] + rep_force[2*index];
	                 vect[1] = att_force[2*index+1] + rep_force[2*index+1];
	                 vect[0]*= (averageEdgeLength*averageEdgeLength);
	                 vect[1]*= (averageEdgeLength*averageEdgeLength);
	                 norm = Math.sqrt(vect[0]*vect[0] + vect[1]*vect[1]);
	                 if(norm == 0)
	                     continue;
	                 scalar = Math.min(norm*0.5, 100.0)/norm;
	                 vect[0] *= scalar;//Math.sqrt(norm);
	                 vect[1] *= scalar;//Math.sqrt(norm);
	                 
	                 int pos_x = outer_node.getX();
	                 pos_x += (int)vect[0]*0.09;
	                 int pos_y = outer_node.getY(); 
	                 pos_y += (int)vect[1]*0.09;
	                // System.out.println(pos_x+" "+pos_y);
	               //  outer_node.setX(pos_x);
	               //  outer_node.setY(pos_y);
	                 pos_x = (int)(outer_node.getX()*(1.0 - 1.0/Math.sqrt(numOfIterations)) + pos_x*1.0/Math.sqrt(numOfIterations));
	                 pos_y = (int)(outer_node.getY()*(1.0 - 1.0/Math.sqrt(numOfIterations)) + pos_y*1.0/Math.sqrt(numOfIterations));
	                 if(outer_node.pinned == false)
	                	 controller.moveNode(outer_node, pos_x, pos_y);
	                 
                 }
                 index++;
            }
        }
    }

    /**
     * Layout core logic.
     *
     * This iterates over all the nodes and determines their velocities based
     * on a force directed layout approach, performs collision detection/avoidance
     * and then ultimately their new positions as they move.
     */
    private void doLayout() {
        double maxVelocity = 100;
        double numOfIterations = 0;
        double step = 100;
        double skip = step;
        int counter = 1;
        boolean dividerChanged = false;
        this.COOLING_DIVIDER = 100;
        
        //active = true;
      //  forceDirectedLayout();
       while (maxVelocity > 1 && active && (numOfIterations < MAX_NUM_OF_ITERATIONS)) {
        boolean test = false;
       // while(test) {
            counter++;
            maxVelocity = 0;
            numOfIterations++;
            Iterator<Node> outer = controller.getNodeIterator();
            startTime = System.currentTimeMillis();
            
            double systemMovement = 0;

            /* Iterate over all nodes and calculate velocity */
            while (outer.hasNext()) {
                Node current = outer.next();
                current.resetVelocity();
                
                
                
                /*
                 * If current node is not pinned or selected,
                 * perform force directed layout calculations here.
                 */
                if (!((current == controller.getGraphSelected()) || current.isPinned())) {
                    calculateNodeAttractiveForce(current);
                    calculateNodeRepulsiveForces(current);
                    applyNodeFriction(current);
                    if (IS_COOLING) {
                        calculateNodeCooling(current, numOfIterations);
                    }


                    /* Round down values to avoid jitter at sub pixel level */
                    /*
                    if (Math.abs(current.getVX()) < 1 && Math.abs(current.getVY()) < 1) {
                        current.setVX(0);
                        current.setVY(0);
                    }
                    */

                    //cap the velocity
                    if(current.getVX() >= 0) {
                        current.setVX(Math.min(current.getVX(), MAX_DIST_PER_MOVE));
                    } else {
                        current.setVX(Math.max(current.getVX(), -MAX_DIST_PER_MOVE));
                    }
                    

                    if(current.getVY() >= 0) {
                        current.setVY(Math.min(current.getVY(), MAX_DIST_PER_MOVE));
                    } else {
                        current.setVY(Math.max(current.getVY(), -MAX_DIST_PER_MOVE));
                    }
                    //stop moving if the velocity is low enough
                   
                   /* 
                    if(Math.abs(current.getVX()) < MIN_VX) {
                        current.setVX(0);
                    }
                    if(Math.abs(current.getVY()) < MIN_VY) {
                        current.setVY(0);
                    }
                    */
                    //System.out.println("vy "+ current.getVY() + ", vx " + current.getVX());

                }


            }

            /* Iterate over all nodes move them after all the velocities are set */
            outer = controller.getNodeIterator();
            while (outer.hasNext()) {
                Node current = outer.next();
                int xStart = current.getX();
                int yStart = current.getY();

                /* update node position */
                int xEnd = current.getX() + (int) current.getVX();
                int yEnd = current.getY() + (int) current.getVY();
                int dX = Math.abs(xStart - xEnd);
                int dY = Math.abs(yStart - yEnd);
                
                systemMovement += dX;
                systemMovement += dY;
                
                //if(dX + dY > 6) {
                    controller.moveNode(current, xEnd, yEnd);
                //}
                //controller.moveNode(current, Math.min(current.getX() + (int) current.getVX(),MAX_DIST_PER_MOVE), Math.min(current.getY() + (int) current.getVY(), MAX_DIST_PER_MOVE));

                /* compute maxVelocity for layout iteration */
                maxVelocity = Math.max(
                        maxVelocity,
                        Math.sqrt(Math.pow(current.getVX(), 2) + Math.pow(current.getVY(), 2)));
            }
            
            if(systemMovement < CONVERGENCE_THRESHOLD) {
                //active = false;
                maxVelocity = 1;
                if(!dividerChanged) {
                    this.COOLING_DIVIDER = numOfIterations + 10;
                    dividerChanged = true;
                }
            }

            /* Make animation slower */
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException ex) {
            }

            /* Calculate how long the iteration took */
            stopTime = System.currentTimeMillis();
            long duration = stopTime - startTime;
            if ((duration > MAX_ITERATION_TIME) && (sleepInterval > 5)) {
                sleepInterval -= 5;
            } else if (duration < MIN_ITERATION_TIME) {
                sleepInterval += 5;
            }
            //System.out.println("ssleep interval: " + sleepInterval);
            //System.out.println("Max velocity: " + maxVelocity);
            //System.out.print(".");

            if (numOfIterations > skip) {
                skip += step;
                System.out.print('.');
            }
        }

        /* We've reached a stable layout, hold on for now */
        stable = true;
        //System.out.println("ran in " + numOfIterations + " iterations.");
    }

    /**
     * This helper method calculates the repulsive forces acting on a node from
     * all the other nodes in the graph.
     * BIG O( number of nodes )
     *
     * There is a repulsive force between every nodes depending on the distance
     * separating them and their size.
     * @param current node to calculate forces for.
     */
    private void calculateNodeRepulsiveForces(Node current) {
        Iterator<Node> inner = controller.getNodeIterator();
        int current_radius, n_radius, dx, dy;
        Node n;
        current_radius = (int) Math.sqrt(Math.pow(current.getWidth(), 2) + Math.pow(current.getHeight(), 2));
        while (inner.hasNext()) {
            n = inner.next();
            if (n != current) {
                dx = current.getX() - n.getX();
                dy = current.getY() - n.getY();
                n_radius = (int) Math.sqrt(Math.pow(n.getWidth(), 2) + Math.pow(n.getHeight(), 2));

                //only repel if nodes are connected or within diameter * MAX_REPEL_DISTANCE
              //  if (Math.sqrt(dx * dx + dy * dy) < (Math.max(current_radius, n_radius) * MAX_REPEL_MULTIPLIER)) {
                    double l = (dx * dx + dy * dy);
                    if (l > 0) {
                        current.setVX(current.getVX() + dx * (current_radius * SPACING) / l);
                        current.setVY(current.getVY() + dy * (current_radius * SPACING) / l);
                    }
              //  }
            }
        }
    }

    /**
     * This helper calculates all the attractive forces onto a node.
     * Attractive forces from from the edges pulling nodes towards each other,
     * this will love through the edges of this node.
     *
     * @param current Node to calculate forces for.
     */
    private void calculateNodeAttractiveForce(Node current) {
        //get all the edges of the current node
        ArrayList<Edge> nodeEdgeList = current.getEdgeList();
        //of each of this node's edge do attactive forces
        Iterator<Edge> nodeEdges = nodeEdgeList.iterator();
        ///
        int numOfEdgesWeight = 10 * (int) Math.round(nodeEdgeList.size() + SPACING);
        
        //Loop through edges and find edges containing current node
        while (nodeEdges.hasNext()) {
            Edge e = nodeEdges.next();
            double edgeStrength = e.getStrength();
            Node n;
            int dx, dy;
            if (current == e.getNode1()) {
                n = e.getNode2();
                dx = current.getX() - n.getX();
                dy = current.getY() - n.getY();
            } else {
                n = e.getNode1();
                dx = current.getX() - n.getX();
                dy = current.getY() - n.getY();
            }
            //multiply by the strength of edge
            current.setVX(current.getVX() - dx / numOfEdgesWeight * edgeStrength);
            current.setVY(current.getVY() - dy / numOfEdgesWeight * edgeStrength);
        }
    }

    /**
     * Cools down the movement on the node.
     *
     * @param current Node to cool
     * @param numOfIterations current state of layout process (heat)
     */
    private void calculateNodeCooling(Node current, double numOfIterations) {
        /* Check if cooling should start */
        if (COOLING_START_DELAY < numOfIterations) {
            double coolingFactor = 1 - ((numOfIterations - COOLING_START_DELAY) / COOLING_DIVIDER);
            //double coolingFactor = 1  / COOLING_DIVIDER;

            coolingFactor = Math.max(COOLING_FACTOR_MINIMUM, coolingFactor);

            /* Do some cooling, to stop the mad bouncing */
            double vx = current.getVX() * coolingFactor;
            double vy = current.getVY() * coolingFactor;
          /*  if(vx > MIN_VX) {
                current.setVX(vx);
            }
            else {
                current.setVX(0);
            }
            if(vy > MIN_VY) {
                current.setVY(vy);
            }
            else {
                current.setVY(0);
            }*/
            
            
            current.setVX(current.getVX() * coolingFactor);
            current.setVY(current.getVY() * coolingFactor);
        }

    }

    /**
     * This function slows down a node's velocity based on
     * it's weight. So that the heavier the node the less it moves.
     * @param n node to apply friction for.
     */
    private void applyNodeFriction(Node n) {
        n.setVX(n.getVX() / n.getWeight());
        n.setVY(n.getVY() / n.getWeight());
    }

    /**
     * Set the graph this layout will act on when started.
     * @param fs controller to do layout for
     */
    public void setForceSpireController(StarSpireController fs) {
        stop();
        /* remove self from current graph */
        if (controller != null) {
            controller.removeGraphListener(this);
        }
        controller = fs;
        /* add self to new graph */
        if (controller != null) {
            controller.addGraphListener(this);
        }
    }

    /**
     * A node was added, make sure layout knows it needs to run.
     * If layout thread is sleeping, wake it up.
     * @param n added node
     */
    public void nodeAdded(Node n) {
        markLayoutDirty();
    }

    /**
     * A node was modified, make sure layout knows it needs to run.
     * If layout thread is sleeping, wake it up.
     * @param n Node that moved
     */
    public void nodeModified(Node n,NodeModType t) {
        markLayoutDirty();
    }

    /**
     * A node was moved, make sure layout knows it needs to run.

     * @param n Node that moved
     */
    public void nodeMoved(Node n) {
        markLayoutDirty();
    }

    /**
     * A node was removed, make sure layout knows it needs to run.
     * @param n Node that removed
     */
    public void nodeRemoved(Node n) {
        markLayoutDirty();
    }

    /**
     * not used here
     * @param n Node that was selected
     */
    public void nodeSelected(Node n) {
    }

    /**
     * An edge was added, make sure layout knows it needs to run.
     * @param e edge added from graph
     */
    public void edgeAdded(Edge e) {
        markLayoutDirty();
    }

    /**
     * An edge was modified, make sure layout knows it needs to run.
     * @param e edge modified from graph
     */
    public void edgeModified(Edge e) {
        markLayoutDirty();
    }

    /**
     * An edge was removed, make sure layout knows it needs to run.
     * @param e edge removed from graph
     */
    public void edgeRemoved(Edge e) {
        markLayoutDirty();
    }

    /**
     * Helper for events. Signals the graph it needs to restart calculating
     * forces as the model has changed.
     * Mark graph as 'dirty'
     * If layout thread is sleeping, wake it up.
     */
    private void markLayoutDirty() {
        stable = false;
        if (idleing) {
            runner.interrupt();
        }
    }

    /**
     * Main threading method.
     * Runs layout or sleeps.
     */
    public void run() {
        System.out.println(Thread.currentThread().getName()
                + " layout starting up");
        while (active) {
            if (stable) {
                //graph is stable, sleep and try again later.
                //System.out.println(Thread.currentThread().getName() + " done, sleeping.");
                try {
                    idleing = true;
                    Thread.sleep(THREAD_SLEEP_TIME);
                } catch (InterruptedException ex) {
                    //just stop sleeping early...
                    //System.out.println( Thread.currentThread().getName() + " waking up.");
                }
                idleing = false;
            } else {
                //System.out.println(Thread.currentThread().getName() + " doing layout...");
                try {
                    doLayout();
                } catch(Exception e) {
                    System.out.println("Layout crashed");
                    stable = true;
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + "layout stopping.");
    }

    /**
     * Returns true if the layout is running, false if it's stopped or paused or ...
     *
     * @return true if running false otherwise.
     */
    public boolean isRunning() {
        return runner.isAlive();
    }

    /**
     * Reset the layout thread.
     */
    private void reset() {
        System.out.println("Reseting Layout Thread...");
        stop();
        if (pauseState) {
            pauseState = false;
            pause.putValue(AbstractAction.NAME, "Pause");
        }
        start();
    }

    /**
     * The graph was resize so the algorithm must be adapted to the new size.
     * Then mark dirty.
     *
     * @param w new width
     * @param h new height
     */
    private void setGraphSize(int w, int h) {
        int oldWidth = width;
        int oldHeight = height;
        double relativeWidth, relativeHeight;
        int newX, newY;

        width = w;
        height = h;

        //Move the pinned nodes to adjust to the new size
        ArrayList<Node> pinnedNodes = controller.getGraphPinned();
        if (pinnedNodes != null) {
            for (Node n : pinnedNodes) {
                relativeWidth = (double) n.getX() / oldWidth;
                relativeHeight = (double) n.getY() / oldHeight;
                newX = (int) Math.round(width * relativeWidth);
                newY = (int) Math.round(height * relativeHeight);
                n.setX(newX);
                n.setY(newY);
            }
        }
    }

    /**
     * Returns JSONObject with graph data.
     * Stores size of graph layout.
     * @return Empty JSONObject
     */
    public JSONObject getJSONOjbect() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("algorithm", this.getClass().getName());
        data.put("PauseState", pauseState);
        return data;
    }

    /**
     * This returns the actions for this layout:
     * pause
     * play
     * stop
     * ...
     * @return An array of actions for this object.
     */
    public ArrayList<AbstractAction> getActions() {
        return actions;
    }

    /**
     * A node was opened.
     * @param n node that opened...
     */
    public void nodeOpened(Node n) {
        markLayoutDirty();
    }

    /**
     * A node was closed.
     * @param n node that closed...
     */
    public void nodeClosed(Node n) {
        markLayoutDirty();
    }

    /**
     * The graph size was changed...
     * @param d new graph size.
     */
    public void graphResized(Dimension d) {
        setGraphSize(d.width, d.height);
    }
}
