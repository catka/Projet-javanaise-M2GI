package irc;

import utils.ActionType;

public interface ISentence {
	@ActionType(name="WRITE")
	public void write(String text);
	
	@ActionType(name="READ")
	public String read();
}