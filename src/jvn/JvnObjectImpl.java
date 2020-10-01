package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private LockStates lockState = LockStates.NL;
	private Serializable obj = null;
	private int id = 0;
	
	
	
	public JvnObjectImpl(Serializable o) {
		obj = o;
	}

	@Override
	public void jvnLockRead() throws JvnException {
		switch(lockState) {
			case RC:
				lockState = LockStates.R;
			break;
			case W:
				lockState = LockStates.RWC;
			break;
			default:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				Serializable retObj = js.jvnLockRead(id);
				
				
				
				if(retObj != null) {
					lockState = LockStates.R;
					obj = retObj;
				}
			break;
		}
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		Serializable retObj = js.jvnLockRead(id);
		if(retObj != null) {
			lockState = LockStates.W;
			obj = retObj;
		}
	}

	@Override
	public void jvnUnLock() throws JvnException {
		lockState = LockStates.NL;
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		return id;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		return obj;
	}

	@Override
	public void jvnInvalidateReader() throws JvnException {
		lockState = LockStates.NL;
	}

	@Override
	public Serializable jvnInvalidateWriter() throws JvnException {
		lockState = LockStates.NL;
		return obj;
	}

	@Override
	public Serializable jvnInvalidateWriterForReader() throws JvnException {
		lockState = LockStates.R;
		return obj;
	}

}
