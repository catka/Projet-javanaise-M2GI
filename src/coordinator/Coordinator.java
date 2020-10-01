package coordinator;


import java.rmi.Naming;

import jvn.JvnCoordImpl;
import jvn.JvnRemoteCoord;

public class Coordinator {

	public static void main(String[] args) {
		 try {
			 	java.rmi.registry.LocateRegistry.createRegistry(JvnCoordImpl.getJvnCoordPort());
			 	JvnRemoteCoord coordinator = JvnCoordImpl.jvnGetCoordinator();
				Naming.rebind(JvnCoordImpl.getJvnCoordRegistryId(), coordinator);
	            System.out.println("Binded Coordinator!");
	        } catch (Exception e) {
	            System.err.println("Server exception: " + e.toString());
	            e.printStackTrace();
	            System.exit(e.hashCode());
	        }
	}

}
