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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.MalformedURLException;



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
		//startCoordinator();
		System.out.println("getting coordinator");
		coordinator = getCoordinator();
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
		if(getCoordinator() != null) {
			try {
				getCoordinator().jvnTerminate(this);
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
	public JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		// Allocate an id to the jvnObject
		int joi = -1;
		JvnObject jo = null;
		if(getCoordinator() != null) {
			try {
				joi = getCoordinator().jvnGetObjectId();
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
		if(getCoordinator() != null) {
			System.out.println("Registering obj '" + jon + "'");
			try {
				getCoordinator().jvnRegisterObject(jon, jo, this);
				cache.put(jo.jvnGetObjectId(), jo); //Caching the object
			}catch(RemoteException re) {
				System.out.println(re);
			}
			
		}else {
			System.out.println("Error: Coordinator is null");
		}
	}
	
	/**
	* Provide the reference of a JVN object being given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
    /*
     *  
     */
		JvnObject jo = null;
		if(getCoordinator() != null) {
			System.out.println("Looking up object (name = " + jon + ") ");
			try {
				jo = getCoordinator().jvnLookupObject(jon, this);
				if(jo != null) {
					cache.put(jo.jvnGetObjectId(), jo); // caching the object
				} else {
					System.out.println("Object is null!");
				}
			}catch(RemoteException re) {
				System.out.println(re.getMessage());
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
		if(getCoordinator() != null) {
			System.out.println("Asking for a Read Lock of id = " + joi);
			try {
				return getCoordinator().jvnLockRead(joi, this);
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
	   if(getCoordinator() != null) {
			System.out.println("Asking for a Write Lock of id = " + joi);
			try {
				return getCoordinator().jvnLockWrite(joi, this);
			}catch(RemoteException re) {
				System.out.println(re.getMessage());
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
	  // Asks the jvnObject who has the lock to invalidate it.
	   // The invalidate call is from the coordinator
	   System.out.println("Invalidating Reader : id = " + joi + ". Waiting for JvnObject confirmation");
	   cache.get(joi).jvnInvalidateReader();
	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
	   // Asks the jvnObject who has the lock to invalidate it.
	   // The invalidate call is from the coordinator
	  
	  if(cache != null && cache.containsKey(joi)) {
		  System.out.println("[Serv:jvnInvalidateWriter] JvnObject lock state " + ((JvnObjectImpl)cache.get(joi)).jvnGetLockState());
		   return cache.get(joi).jvnInvalidateWriter();
	   }else {
		   throw new JvnException("Attempt to invalidate Writer on a non-cached jvnObject");
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
	   // Asks the jvnObject who has the lock to invalidate it.
	   // The invalidate call is from the coordinator
	   if(cache != null && cache.containsKey(joi)) {
		   return cache.get(joi).jvnInvalidateWriterForReader();
	   }else {
		   throw new  JvnException("Attempt to invalidate Writer For Reader on a non-cached jvnObject");
	   }
	 };


	 // Managing coordinator from server to prevent coordinator failure
	 public JvnRemoteCoord getCoordinator() {
		 try {
			 if(coordinator != null) {
				 return coordinator;
			 }
			return (JvnRemoteCoord) Naming.lookup(JvnCoordImpl.getJvnCoordRegistryId());
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			final int MAX_RETRY = 3;
			int retry = 0;
			while(coordinator == null && retry < MAX_RETRY) {
				coordinator = (JvnRemoteCoord) registerCoordinator();
				retry++;
			}
			return coordinator;
		}
	}
	 
	 public JvnRemoteCoord registerCoordinator() {
		 try {
			 	java.rmi.registry.LocateRegistry.createRegistry(JvnCoordImpl.getJvnCoordPort());
			 	JvnRemoteCoord coordinator = JvnCoordImpl.jvnGetCoordinator();
				Naming.rebind(JvnCoordImpl.getJvnCoordRegistryId(), coordinator);
	            System.out.println("Binded Coordinator!");
	            return coordinator;
	        } catch (Exception e) {
	            System.err.println("Server exception: " + e.toString());
	            e.printStackTrace();
	            System.exit(e.hashCode());
	        }
		 
		 return null;
	 }
}

 
