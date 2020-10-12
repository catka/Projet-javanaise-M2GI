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
		lockState = LockStates.W;
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
			default:
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
		System.out.println("[LockWrite (before) = " + lockState);
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
					System.out.println("calling LockedWrite = " + lockState);
					lockState = LockStates.W;
					obj = retObj;
				}
			break;
		}
	}

	@Override
	public void jvnUnLock() throws JvnException {
		System.out.println(" Unlocking : " + lockState);
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
			synchronized(this) {
				this.notify();
			}
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
			System.out.println("invalidate Reader (before state = " + lockState + ")");
			switch(lockState) {
				case RC:
					System.out.println("JvnObject was cached, now NL");
					lockState = LockStates.NL;
				break;
				case R:
				case RWC:
					System.out.println("JvnObject lock is: " + lockState + ". Waiting for invalidation Reader");
		            try{
		            	synchronized(this) {
		            		this.wait();
		            	}
		                lockState = LockStates.NL;
		                System.out.println("New state = " + lockState);
		            }catch(InterruptedException e){
		                e.printStackTrace();
		            }
		            System.out.println("Invalidation Reader complete. JvnObject lock is now: " + lockState + ".");
				break;
				default:
					System.out.println("JvnObject lock passes from " + lockState + " to " + lockState.NL);
					lockState = LockStates.NL;
				break;	
			}
	}

	@Override
	 public Serializable jvnInvalidateWriter() throws JvnException {
			
			switch(lockState) {
				case WC:
					System.out.println("JvnObject was cached, now NL");
					lockState = LockStates.NL;
					break;
				case W:
				case RWC:
					System.out.println("JvnObject lock is: " + lockState + ". Waiting for invalidation Writer");
		            try{
		            	synchronized(this) {
		            		this.wait();
		            	}
		                lockState = LockStates.NL;
		                
		            }catch(InterruptedException e){
		                e.printStackTrace();
		            }
		            System.out.println("Invalidation Writer complete. JvnObject lock is now: " + lockState + ".");
		            lockState = LockStates.NL;
				break;
				default:
					System.out.println("JvnObject lock passes from " + lockState + " to " + lockState.NL);
					lockState = LockStates.NL;
				break;
					
			}
		
		return obj;
	}

	@Override
	 public Serializable jvnInvalidateWriterForReader() throws JvnException {
			
			switch(lockState) {
				case W:
					System.out.println("JvnObject lock is: " + lockState + ". Waiting for invalidation WrtierForReader");
		            try{
		            	synchronized(this) {
		            		this.wait();
		            	}
		            }catch(InterruptedException e){
		                e.printStackTrace();
		            }
	                lockState = LockStates.NL;
		            System.out.println("Invalidation WriterForReader complete. JvnObject lock is now: " + lockState + ".");
					lockState = LockStates.RC;
				break;
				default:
					System.out.println("JvnObject lock passes from " + lockState + " to " + lockState.RC);
					lockState = LockStates.RC;
				break;
					
			}
		return obj;
	}
	
	
	public  LockStates jvnGetLockState() {
		return lockState;
	}

}


//class PollStateThread extends Thread {
//	
//	final JvnObjectImpl jo;
//	final boolean waitInvalidWrite;
//	
//	 public PollStateThread(JvnObjectImpl jo, boolean waitInvalidWrite) {
//		 this.jo = jo;
//		 this.waitInvalidWrite = waitInvalidWrite;
//	 }
// 
//    @Override
//    public void run() {
//            while (true) {
//            	LockStates state = jo.jvnGetLockState();
//            	System.out.println("Thread = " + state + " , waitInvalidWrite" + waitInvalidWrite);
//            	if(waitInvalidWrite && state != LockStates.W) {
//            		//Can now invalidate Write lock
//            		break;
//            	}else if(!waitInvalidWrite && (state  != LockStates.R && 
//						            			state != LockStates.W && 
//						            			state != LockStates.RWC )) {
//            		//Can now invalidate Read lock
//            		break;
//            	}
//                
//            }
//            synchronized(jo) {
//            	jo.notify();
//            }
//    }
//}
