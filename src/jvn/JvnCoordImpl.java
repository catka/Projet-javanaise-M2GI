/***
 * JAVANAISE Implementation
 * JvnCoordImpl class
 * This class implements the Javanaise central coordinator
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.io.Serializable;


public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN coordinator is managed as a singleton 
	private static JvnRemoteCoord coord = null;
	
	final private int MAX_ID =  999999999;
	private int lastJoiAssigned = 0;
	
	private static int port = 1099;
	private static String registryId = "JvnCoord";
	
	
	
	private Map<String, Integer> aliases;
	private Map<Integer, JvnObject> objects;
	private Map<Integer, List<JvnRemoteServer>> readLocks;
	private Map<Integer, JvnRemoteServer> writeLocks;
	
	//Lock stack for each joi.
	//The top of the stack represents the most restrictive lock on the jvn object
	private Map<Integer, Stack<StackEntry>> locks;
	
	private class StackEntry{
		public JvnRemoteServer js = null;
		public LockStates state = LockStates.NL;
		
		public StackEntry(JvnRemoteServer mJs, LockStates mState) {
			js = mJs;
			state = mState ;
		}
	}
	

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		aliases = new HashMap<String, Integer>();
		objects = new HashMap<Integer, JvnObject>();
		readLocks = new HashMap<Integer, List<JvnRemoteServer>>();
		writeLocks = new HashMap<Integer, JvnRemoteServer>();
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN coordinator instance
    * @throws JvnException
    **/
	public static JvnRemoteCoord jvnGetCoordinator() {
		if (coord == null){
			try {
				coord = new JvnCoordImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return coord;
	}
	
	public static int getJvnCoordPort() {
		return port;
	}

	public static String getJvnCoordRegistryId() {
		return registryId;
	}
	
  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
	  lastJoiAssigned = (lastJoiAssigned  + 1) % MAX_ID;
    return lastJoiAssigned;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    int id = jvnGetObjectId();
    
    objects.put(id, jo);
    aliases.put(jon, id);
    
	readLocks.put(id, new ArrayList<JvnRemoteServer>());
    writeLocks.put(id, js);  
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
	  if(aliases.get(jon) != null) {
		  int id = aliases.get(jon);
		  if(id > 0) {
			  JvnObject object = objects.get(id);
			  return object;
		  }
	  }else {
		  //Not Found
	  }
	  return null;
	  
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	
	LockStates state = LockStates.NL;
	List<JvnRemoteServer> readers = readLocks.get(joi);
	
	if(writeLocks.get(joi) != null) {
		// invalidateWriterForReader must be called before registering new reader
		state = (LockStates)writeLocks.get(joi).jvnInvalidateWriterForReader(joi);
		
		//TODO: then, register new reader and return the object
		
		
	} else {
		if(readers == null) {
			readers = new ArrayList<JvnRemoteServer>();
			
		}
	    if(!readers.contains(js)){
	    	readers.add(js);
	    }
	    readLocks.put(joi, readers);
	    state = LockStates.R;
	}
	//System.out.println("Return state = " + state);
	return state;
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	   
	   
	List<JvnRemoteServer> readers = readLocks.get(joi);
	if(readers.size() > 0) {
		 // invalidateReader
		readers.forEach(r -> {
			try {
				r.jvnInvalidateReader(joi); 
				// TODO: wait for all readers to confirm the unlock
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (JvnException e) {
				e.printStackTrace();
			}
		});
		readers.clear();
	}
    if(writeLocks.get(joi) == null) {
    	writeLocks.put(joi, js);
    	return objects.get(joi);
    } else {
    	writeLocks.get(joi).jvnInvalidateWriter(joi);
    	// TODO: wait for write lock to be released, then register the new remote server and return the object
    }
    return null;
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
	 // to be completed
    }
}

 
