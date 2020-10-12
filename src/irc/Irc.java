/***
 * Irc class : simple implementation of a chat using JAVANAISE 
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*; 


import jvn.*;
import utils.JvnProxy;

import java.io.*;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	ISentence       sentence;
	public TextField textLockState;
	


  /**
  * main method
  * create a JVN object named IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
		   ISentence jo = (ISentence) JvnProxy.newInstance(Sentence.class, "IRC");
		
		   // create the graphical part of the Chat application
		   new Irc(jo);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.toString());
	   }
	}

  /**
   * IRC Constructor
   @param jo the JVN object representing the Chat
   **/
	public Irc(ISentence jo) {
		sentence = jo;
		frame=new Frame();
		frame.setLayout(new GridLayout(1,1));
		text=new TextArea(10,60);
		text.setEditable(false);
		text.setForeground(Color.red);
		text.setBackground(Color.black); 
		frame.add(text);
		data=new TextField(40);
		textLockState = new TextField("LockState", 20);
		frame.add(data);
		Button read_button = new Button("read");
		read_button.addActionListener(new readListener(this));
		frame.add(read_button);
		Button write_button = new Button("write");
		write_button.addActionListener(new writeListener(this));
		frame.add(write_button);
		Button crash_button = new Button("crash client");
		crash_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// Terminate the  client without noticing the coordinator
				// In order to simulate client crash
				frame.dispose();
				System.exit(0);
			}
		});
		frame.add(crash_button);
		frame.add(textLockState);
		textLockState.setText(sentence.getLockState().toString());
		
		frame.setSize(545,201);
		
		
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				
				try {
					JvnServerImpl.jvnGetServer().jvnTerminate();
				}catch(JvnException je) {
					je.printStackTrace();
				}
				super.windowClosing(arg0);
				frame.dispose();
				System.exit(0);
			}
			
		});
	}
}


 /**
  * Internal class to manage user events (read) on the CHAT application
  **/
 class readListener implements ActionListener {
	Irc irc;
  
	public readListener (Irc i) {
		irc = i;
	}
   
 /**
  * Management of user events
  **/
	public void actionPerformed (ActionEvent e) {
		irc.textLockState.setText(irc.sentence.getLockState().toString());
		// invoke the method
		String s = irc.sentence.read();
		// display the read value
		irc.data.setText(s);
		irc.text.append(s+"\n");
		irc.textLockState.setText(irc.sentence.getLockState().toString());
	}
}

 /**
  * Internal class to manage user events (write) on the CHAT application
  **/
 class writeListener implements ActionListener {
	Irc irc;
  
	public writeListener (Irc i) {
        	irc = i;
	}
  
  /**
    * Management of user events
   **/
	public void actionPerformed (ActionEvent e) {
		irc.textLockState.setText(irc.sentence.getLockState().toString());
	   // get the value to be written from the buffer
		String s = irc.data.getText();
	
		// invoke the method
		irc.sentence.write(s);
		irc.textLockState.setText(irc.sentence.getLockState().toString());
		
	}
}



