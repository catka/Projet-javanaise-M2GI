package jvn;

public enum LockStates {
	NL, // No Lock
	RC, //Read lock cached
	WC, //Write lock cached
	R, //Read lock taken
	W, //Write lock taken
	RWC //Write lock cached & read taken
}
