package irc;

import jvn.LockStates;
import utils.ActionType;

public interface ISentence {
	@ActionType(name="WRITE")
	public void write(String text);
	
	@ActionType(name="READ")
	public String read();
	
	@ActionType(name="DEBUG")
	public LockStates getLockState();
}