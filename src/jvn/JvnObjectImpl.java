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
	
	public JvnObjectImpl(Serializable i, int joi, LockStates state) {
		obj = i;
		id = joi;
		lockState = state;
	}
	
	public JvnObjectImpl(Serializable o, int joi) {
		obj = o;
		id = joi;
	}

	@Override
	public void jvnLockRead() throws JvnException {
		switch(lockState) {
			case RC:
				lockState = LockStates.R;
			break;
			case W:
			case WC:
				lockState = LockStates.RWC;
			break;
			case RWC:
			break;
			case NL:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				Serializable retObj = js.jvnLockRead(id); //Returns the up-to-date jvnObject
				if(retObj != null) {
					obj = retObj;
					lockState = LockStates.R;
				}else {
					throw new JvnException("Failed to retrieve up-to-date JVN Object state while requesting a Read Lock");
				}
			break;
		}
	}

	@Override
	public void jvnLockWrite() throws JvnException {
		switch(lockState) {
			case W:
			case WC:
			case RWC:
				lockState = LockStates.W;
			break;
			case NL:
			case RC:
			default:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				Serializable retObj = js.jvnLockWrite(id);
				if(retObj != null) {
					lockState = LockStates.W;
					obj = retObj;
				}
			break;
		}
	}

	@Override
	public void jvnUnLock() throws JvnException {
		try {
			switch(lockState) {
				case W:
				case RWC:
					lockState = LockStates.WC;
				break;
				case R:
					lockState = LockStates.RC;
				break;
				default:
					lockState = LockStates.NL;
				break;
			}
			this.notify();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int jvnGetObjectId() throws JvnException {
		return id;
	}

	@Override
	public Serializable jvnGetSharedObject() throws JvnException {
		return this.obj;
	}

	@Override
	public void jvnInvalidateReader() throws JvnException {
		try {
			switch(lockState) {
				case R:
				case RWC:
					this.wait();
					lockState = LockStates.NL;
				break;
				default:
					lockState = LockStates.NL;
				break;
					
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Serializable jvnInvalidateWriter() throws JvnException {
		try {
			switch(lockState) {
				case W:
					this.wait();
					lockState = LockStates.NL;
				break;
				default:
					lockState = LockStates.NL;
				break;
					
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

	@Override
	public Serializable jvnInvalidateWriterForReader() throws JvnException {
		try {
			switch(lockState) {
				case W:
					this.wait();
					lockState = LockStates.RC;
				break;
				default:
					lockState = LockStates.RC;
				break;
					
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}

}
