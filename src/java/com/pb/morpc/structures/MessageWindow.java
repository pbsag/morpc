/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.pb.morpc.structures;

/**
 * @author jim
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class MessageWindow extends JFrame implements java.io.Serializable {
//public class MessageWindow {
	public static JLabel jMsg1 = new JLabel();
	public static JLabel jMsg2 = new JLabel();
	public static JLabel jMsg3 = new JLabel();


	public MessageWindow ( String heading ) {

		
		super ( heading );
		
		Container cp = getContentPane();
		cp.setLayout (new GridLayout(3,1));
		jMsg1.setText ("");
		cp.add (jMsg1);
		jMsg2.setText ("");
		cp.add (jMsg2);
		jMsg3.setText ("");
		cp.add (jMsg3);


		this.addWindowListener (
			new WindowAdapter() {
				public void windowClosing (WindowEvent e) {
					System.exit(0);
				}
			} );

		this.setSize (700,100);
		this.setVisible(true);


	}


	public void setMessage1 (String message) {
		jMsg1.setText (message);
	}
	
	
	public void setMessage2 (String message) {
		jMsg2.setText ("        " + message);
	}
	
	
	public void setMessage3 (String message) {
		jMsg3.setText ("        " + message);
	}

//	public void setVisible (boolean flag) {//
//		this.setVisible (flag);
//	}

}
