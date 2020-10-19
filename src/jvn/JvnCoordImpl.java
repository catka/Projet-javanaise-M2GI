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
import java.util.Arrays;
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
	
	
	private Map<Integer, JvnRemoteServer> serverIndexes;
	final int MAX_SERVER_ID = 50;
	int lastAvailableServerID = 0;
	
	
	
	private Map<String, Integer> aliases;
	private Map<Integer, Serializable> objects; //Contains last validated state of the object
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
		objects = new HashMap<Integer, Serializable>();
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
	
	public synchronized Integer getJvnServerId(JvnRemoteServer js) {
		
		for(Map.Entry<Integer, JvnRemoteServer> entry : serverIndexes.entrySet()) {
			if(entry.getValue().equals(js)) {
				return entry.getKey();
			}
		}
		int tryCnt = 0;
		while(serverIndexes.containsKey(lastAvailableServerID)) {
			++lastAvailableServerID;
			++tryCnt;
			if(tryCnt >= MAX_SERVER_ID)return -1; //No slot available
		}
		serverIndexes.put(lastAvailableServerID, js);
		return lastAvailableServerID;
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
  public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    //int id = jvnGetObjectId();
    
    objects.put(jo.jvnGetObjectId(), jo.jvnGetSharedObject());
    aliases.put(jon, jo.jvnGetObjectId());
    
	readLocks.put(jo.jvnGetObjectId(), new ArrayList<JvnRemoteServer>());
    writeLocks.put(jo.jvnGetObjectId(), js);  
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
	  System.out.println("jvnLooupObject");
	  if(aliases.get(jon) != null) {
		  int id = aliases.get(jon);
			System.out.println("id = " + id);
		  if(id > 0) {
			  //JvnObject object = objects.get(id);
			  
			  LockStates objState = LockStates.NL;
			  if(writeLocks != null && writeLocks.containsKey(id) && writeLocks.get(id) != null) {
				  objState = LockStates.W;
			  }
			  else if(readLocks != null && readLocks.containsKey(id) && readLocks.get(id).size() > 0) {
				  objState = LockStates.R;
			  }
			  //Recompile the JvnObject with the validated lock state in coord
			  
			  JvnObject jo = new JvnObjectImpl(objects.get(id), id, LockStates.NL); //Init lock state to NL
			  return jo;
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
   public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	 System.out.println("[lockRead] Readerslock size = " + readLocks.get(joi));
	
	LockStates state = LockStates.NL;
	List<JvnRemoteServer> readers = readLocks.get(joi);
	
	if(readers == null)readers = new ArrayList<JvnRemoteServer>();
	
	if(objects.containsKey(joi)) {
		System.out.println("Contains the object = " + joi);
	}else {
		System.out.println(" Not contained = " + joi);
	}
	
	if(writeLocks.get(joi) != null && writeLocks.size() > 0) {
		// invalidateWriterForReader must be called before registering new reader
		System.out.println("WriteLocks = " + writeLocks.get(joi) + " vs Server = " + js);
		JvnRemoteServer jsWithWriteLock = writeLocks.get(joi);
		System.out.println("Calling invalidateWfR");
		
		int retryCnt = 0;
		final int MAX_RETRY = 3;
		while(retryCnt <  MAX_RETRY) {
			try {
				Serializable  jo = jsWithWriteLock.jvnInvalidateWriterForReader(joi);
				objects.put(joi, jo); //up-to-date Jvn Object state
				break;
			}catch(RemoteException re) {
				System.out.println("Failed to fetch server for invalidation. Retrying " + retryCnt + "/" + MAX_RETRY);
			}
			++retryCnt;
		}
		if(retryCnt >= MAX_RETRY) {
			System.out.println("Failed to fetch server. Server lost.");
		}
		
		writeLocks.remove(joi);//remove write locks in coord
		//Register new reader and return the object
		if(!readers.contains(js)) {
			readers.add(js);
		}
		if(!readers.contains(jsWithWriteLock)) {
			readers.add(jsWithWriteLock);
		}
		
		
		
	} else {
	    if(!readers.contains(js)){
	    	readers.add(js);
	    }
	    readLocks.put(joi, readers);
	    //state = LockStates.R;
	}
	
	System.out.println("[lockRead:after] Readerslock size = " + readLocks.get(joi));
	return objects.get(joi);
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	   System.out.println("[lockWrite] Readerslock size = " + readLocks.get(joi).size() + ", writeLock = " + writeLocks.containsKey(joi));
	List<JvnRemoteServer> readers = readLocks.get(joi);
	System.out.println("LockWrite called");
	System.out.println("Already have " + readers.size() + " readers on " + joi);
	System.out.println("Already have writer on " + joi + " : " +  writeLocks.containsKey(joi)  );
	
	// Invalidate current readers
	List<JvnRemoteServer> processedServ = new ArrayList<JvnRemoteServer>();
	if(writeLocks != null && writeLocks.containsKey(joi) && !writeLocks.get(joi).equals(js)){
		//A server has a write lock
		System.out.println("Calling invalidateWriter");
		int retryCnt = 0;
		final int MAX_RETRY = 3;
		while(retryCnt <  MAX_RETRY) {
			try {
				Serializable retObj = writeLocks.get(joi).jvnInvalidateWriter(joi);
				objects.put(joi, retObj); //Get the last version of JvnObject
			}catch(RemoteException re) {
				System.out.println("Failed to fetch server for invalidation. Retrying " + retryCnt + "/" + MAX_RETRY);
			}
			++retryCnt;
		}
    	
	}
	
	
	if(readers.size() > 0) {
		readers.forEach(r -> {
			if(!r.equals(js) ) {
				//Invalidate reads from other servers,
				try {
					System.out.println("(1)Calling invalidateReader, has writeLock in coord = " + writeLocks.containsKey(joi));
					
					
					System.out.println("Calling invalidateReader");
					r.jvnInvalidateReader(joi);
					
					processedServ.add(r);
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (JvnException e) {
					e.printStackTrace();
				}
			}else {
				System.out.println("The calling serve of jvnLockWrite has a Read lock on " + joi);
			}
		});
		readers.clear();
	}
    System.out.println("Add write lock in coord for joi = " + joi);
    writeLocks.put(joi, js);
    System.out.println("[lockWrite:after] Readerslock size = " + readLocks.get(joi).size() + " writeLock = " + writeLocks.containsKey(joi));
    return objects.get(joi);
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public synchronized void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
    	System.out.println("A server has called jvnTerminate");
    	//Get the last JVN Object state that are write-locked by the terminating server
    	
    	Object obj = null;
    	boolean hasWriteLock = false;
    	List<Integer> listWriteLocks = new ArrayList<Integer>();
    	for(Map.Entry<Integer, JvnRemoteServer> e: writeLocks.entrySet()) {
    		if(e.getValue().equals(js) ) {
    			//Equals
    			System.out.println("Server terminating has write lock");
    			Serializable lastObj = js.jvnInvalidateWriter(e.getKey());
    			System.out.println("Last version fetched");
    			objects.put(e.getKey(), lastObj);
    			listWriteLocks.add(e.getKey());
    			
    		}
    	}
    	for(Integer val : listWriteLocks)writeLocks.remove(val);
    	
    	//Remove the read locks of the terminating server
    	for(Map.Entry<Integer, List<JvnRemoteServer> > e : readLocks.entrySet()) {
    		if(e.getValue() != null) {
    			if(e.getValue().contains(js)) {
    				e.getValue().remove(js);
    			}
    		}
    		
    	}
    	
    	
    	
    }
}

 
