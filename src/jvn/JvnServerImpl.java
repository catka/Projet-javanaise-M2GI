/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Implementation of a Jvn server
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.io.*;



public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;
	
	/* Objects in cache */
	private Map<Integer, JvnObject> cache;
	
	private JvnRemoteCoord coordinator = null;
	
	

  /**
  * Constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
		cache = new HashMap<Integer, JvnObject>();
		coordinator = (JvnRemoteCoord) Naming.lookup(JvnCoordImpl.getJvnCoordRegistryId());
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
		//Inform the coordinator that the  JnvServer is terminating
		if(coordinator != null) {
			try {
				coordinator.jvnTerminate(this);
			}catch(RemoteException re) {
				System.out.println(re);
			}
		}
	} 
	
	/**
	* creation of a JVN object
	* 
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		//Alllocate an id to the jvnObject
		int joi = -1;
		JvnObject jo = null;
		if(coordinator != null) {
			try {
				joi = coordinator.jvnGetObjectId();
				jo = new JvnObjectImpl(o, joi);
			}catch(RemoteException re) {
				throw new JvnException(re.getMessage());
			}
		}else {
			throw new JvnException("Can't fetch coordinator");
		}
		return jo;
	}
	
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
		// to be completed
		if(coordinator != null) {
			System.out.println("Registering obj '" + jon + "'");
			try {
				coordinator.jvnRegisterObject(jon, jo, this);
				cache.put(jo.jvnGetObjectId(), jo); //Caching the object
			}catch(RemoteException re) {
				System.out.println(re);
			}
			
		}else {
			System.out.println("Error: Coordinator is null");
		}
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
    /*
     *  
     */
		JvnObject jo = null;
		if(coordinator != null) {
			System.out.println("Looking up object (name = " + jon + ") ");
			try {
				jo = coordinator.jvnLookupObject(jon, this);
				if(jo != null) {
					cache.put(jo.jvnGetObjectId(), jo); // caching the object
				}
			}catch(RemoteException re) {
				
				System.out.println(re);
			}
			
		}else {
			System.out.println("Error: Coordinator is null");
		}
		return jo;
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
		if(coordinator != null) {
			System.out.println("Asking for a Read Lock of id = " + joi);
			try {
				

				return coordinator.jvnLockRead(joi, this);
			}catch(RemoteException re) {
				System.out.println(re);
			}
			
		}else {
			System.out.println("Error: Coordinator is null");
		}
		return null;
	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
	   if(coordinator != null) {
			System.out.println("Asking for a Write Lock of id = " + joi);
			try {
				return coordinator.jvnLockWrite(joi, this);
			}catch(RemoteException re) {
				System.out.println(re);
			}
			
		}else {
			System.out.println("Error: Coordinator is null");
		}
		return null;
	}	

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
		// to be completed 
	  //We ask the jvnObject who has the lock to invalidate it.
	   //The invalidate call is from the coordinator
	   System.out.println("Invalidating Reader : id = " + joi + ". Waiting for JvnObject confirmation");
	   cache.get(joi).jvnInvalidateReader();
	   //System.out.println("New state for id = " + joi + " = " + newState);
		//return newState;

	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
	   //We ask the jvnObject who has the lock to invalidate it.
	   //The invalidate call is from the coordinator
	  if(cache != null && cache.containsKey(joi)) {
		   return cache.get(joi).jvnInvalidateWriterForReader();
	   }else {
		   throw new  JvnException("Attempt to invalidate Writer on a non-cached jvnObject");
	   }
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException { 
		//We ask the jvnObject who has the lock to invalidate it.
	   //The invalidate call is from the coordinator
	   if(cache != null && cache.containsKey(joi)) {
		   return cache.get(joi).jvnInvalidateWriterForReader();
	   }else {
		   throw new  JvnException("Attempt to invalidate Writer For Reader on a non-cached jvnObject");
	   }
	   
	 };

}

 
