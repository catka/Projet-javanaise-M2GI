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
import java.util.ArrayList;
import java.util.Random;


public class Irc {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	ISentence       sentence;
	public TextField textLockState;
	

	//Debug
	public TextArea debugTimeEllapsedText;


  /**
  * main method
  * create a JVN object named IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
		
		
		
	   try {
		   ISentence jo = (ISentence) JvnProxy.newInstance(Sentence.class, "IRC");
		
		   // create the graphical part of the Chat application
		   Irc mIrc = new Irc(jo);
		   if(argv != null && argv.length > 0) {
				//[0] Index
				mIrc.getFrame().setTitle("Client pour test Burst n* " + argv[0]);
				mIrc.startBurst(5000);
			}
	   
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
		
		frame.setLayout(new GridLayout(1,6));
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
		
		
		Button burst_button = new Button("Burst (5 sec)");
		burst_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startBurst(5000);
			}
		});
		
		frame.add(burst_button);
		
		
		debugTimeEllapsedText=new TextArea(10,300);
		debugTimeEllapsedText.setEditable(false);
		frame.add(debugTimeEllapsedText);
		
		
		
		frame.setSize(545,300);
		
		
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
	
	
	public Frame getFrame() { return this.frame; }
	
	/**
	 * For test purposes
	 * @param ms: Time in ms to spend testing the system
	 */
	public void startBurst(int ms) {
		StringBuilder sb = new StringBuilder();
		
		long ts = System.currentTimeMillis();
		
		double mean = -1.0;
		long min = -1;
		long max = -1;
		int cnt = 0;
		ArrayList<Long> mValues = new ArrayList<Long>();
		
		
		while( (System.currentTimeMillis() - ts) < 5000) {
			boolean readOrWrite = (new Random()).nextBoolean();
			long timeEllapsed = (readOrWrite?performRead():performWrite());
			mValues.add(timeEllapsed);
			mean = mValues.stream().mapToLong(val -> val).average().orElse(0.0);
			min = (min < 0)?timeEllapsed: Math.min(min, timeEllapsed);
			max = (max < 0)?timeEllapsed: Math.max(max, timeEllapsed);
			
			try{Thread.sleep(10);}catch(InterruptedException ie) {}
			sb.append("(ms) " + (readOrWrite?"READ":"WRITE")  + timeEllapsed + "\n");
			debugTimeEllapsedText.setText(sb.toString());
			cnt++;
		}
		System.out.println("MAX:" + max + ", MIN: " + min + ", MEAN: " + mean);
	}
	
	/**
	 * This method is for test purposes
	 * 
	 * @return time ms ellapsed to write 
	 */
	public long performWrite() {
		long ts = System.currentTimeMillis();
		this.textLockState.setText(this.sentence.getLockState().toString());
	   // get the value to be written from the buffer
		String s = this.data.getText();
		// invoke the method
		this.sentence.write(s);
		this.textLockState.setText(this.sentence.getLockState().toString());
		return System.currentTimeMillis() - ts;
	}
	
	/**
	 * This method is for test purposes
	 * @return time ms ellapsed to read
	 */
	public long performRead() {
		long ts = System.currentTimeMillis();
		this.textLockState.setText(this.sentence.getLockState().toString());
		// invoke the method
		String s = this.sentence.read();
		// display the read value
		this.data.setText(s);
		this.text.append(s+"\n");
		this.textLockState.setText(this.sentence.getLockState().toString());
		return System.currentTimeMillis() - ts;
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



