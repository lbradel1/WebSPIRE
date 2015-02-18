/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package starspire.views;

import starspire.models.DocumentLink;
import starspire.models.Entity;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * This class is used to display a dialog with linking info
 * inside the graph.
 *
 * It has the options to approve or disapprove the link and additionally shows
 * data about the link and ways to customize it.
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class GraphLinkDialog extends JInternalFrame {

    private final static int SIZE_X = 250;
    private final static int SIZE_Y = 300;
    public final static String RESULT_ACCEPT = "Accept";
    public final static String RESULT_REJECT = "Reject";
    private static int nextDialogEventID = 0;
    private int xOffset,yOffset;
    private ArrayList<ActionListener> listeners;
    private DefaultListModel upList;
    private DefaultListModel downList;

    /**
     * Constructor.
     * Takes a document link to display and represent until approved or canceled
     * @param docLink link this frame displays
     */
    public GraphLinkDialog(DocumentLink docLink) {
        super("Link", true, false, false, false);
        listeners = new ArrayList<ActionListener>();

        setLayout(new BorderLayout());
        setSize(SIZE_X, SIZE_Y);

        upList = new DefaultListModel();
        downList = new DefaultListModel();

        initComponents();

        //load entities to upWeight
        for(Entity e : docLink.getEntitiesUpweight()) {
            upList.addElement(e);
        }
    }

    /**
     * Set up helper, builds the content of the frame here and add all the
     * components.
     */
    private void initComponents() {
        setTitle("Document Link");

        //entities in the middle
        Box center = Box.createVerticalBox();
        this.add(center, BorderLayout.CENTER);

        /*
         * Up Weight UI
         */
        JLabel up = new JLabel("Entities Upweighted:");

        JList upjlist = new JList(upList);
        upjlist.setVisibleRowCount(5);
        JScrollPane scrollPane = new JScrollPane(upjlist);
        scrollPane.setPreferredSize(new Dimension(SIZE_X, 50));
        center.add(up);
        center.add(scrollPane);

        /*
         * Down Wheigh UI
         */
//        JLabel down = new JLabel("Entities Downweighted:");
//        JList downJList = new JList(downList);
//        downJList.setVisibleRowCount(5);
//        scrollPane = new JScrollPane(downJList);
//        scrollPane.setPreferredSize(new Dimension(SIZE_X, 50));
//        center.add(down);
//        center.add(scrollPane);

        /*
         * Choices UI
         */
        Box buttonGroup = Box.createHorizontalBox();
        this.add(buttonGroup, BorderLayout.SOUTH);
        JButton accept, reject;
        accept = new JButton("OK");
        accept.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireActionEvent(RESULT_ACCEPT);
                //System.out.println(RESULT_ACCEPT);
            }
        });
        reject = new JButton("Cancel");
        reject.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireActionEvent(RESULT_REJECT);
                //System.out.println(RESULT_REJECT);
            }
        });
        buttonGroup.add(accept);
        buttonGroup.add(reject);

    }

    /**
     * This adds a listener to this dialog. Usually the GraphView
     * so that it can be notified when the dialog is accepted or rejected.
     * @param a ActionListener to add to the dialog.
     */
    public void addActionListener(ActionListener a) {
        listeners.add(a);
    }

    /**
     * This removes an action listener. Probably not needed but better to have it.
     * @param a removes the given listener form the list.
     */
    public void removeActionListener(ActionListener a) {
        listeners.remove(a);
    }

    /**
     * This function is a simple helper to fire an event, just pass it the
     * ActionEvent object.
     * @param ae
     */
    private void fireActionEvent(String eventCmd) {
        ActionEvent ae = new ActionEvent(this, nextDialogEventID++, eventCmd);
        for (ActionListener al : listeners) {
            al.actionPerformed(ae);
        }
    }
}
