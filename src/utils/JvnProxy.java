package utils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jvn.JvnException;
import jvn.JvnObject;
import jvn.JvnObjectImpl;
import jvn.JvnServerImpl;

public class JvnProxy implements InvocationHandler {
	private JvnObject obj;
	
	private JvnProxy(JvnObject obj) { 
		this.obj = obj;
	}
	
	
	public static Object newInstance(Object className, String reference) {
		JvnObject jo = null;
		try {
			// initialize JVN
			JvnServerImpl js = JvnServerImpl.jvnGetServer();
			
			// look up the IRC object in the JVN server
			// if not found, create it, and register it in the JVN server
			jo = js.jvnLookupObject(reference);
			try {
				if (jo == null) {
					jo = js.jvnCreateObject((Serializable) Class.forName((String) className).getDeclaredConstructor().newInstance());
					// after creation, I have a write lock on the object
					jo.jvnUnLock();
					js.jvnRegisterObject(reference, jo);
				}
			
				return Proxy.newProxyInstance(
					Class.forName((String) className).getClass().getClassLoader(),
					Class.forName((String) className).getClass().getInterfaces(),
					
					new JvnProxy(jo)
				);
			} catch (ClassNotFoundException  e) {
				e.printStackTrace();
				return null;
			} catch (InstantiationException | SecurityException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (JvnException e) {
			e.printStackTrace();
		}
		
		return new JvnProxy(jo);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		try {
			if(method.isAnnotationPresent(ActionType.class)) {
				ActionType type = method.getAnnotation(ActionType.class);
				System.out.println("Action de type = " + type.name());
				
				if(type.name().equals("READ")) {
					System.out.println("Locking object for reading");
					obj.jvnLockRead();
					result = method.invoke(obj.jvnGetSharedObject(), args);
					obj.jvnUnLock();
				} else if (type.name().equals("WRITE")) {
					System.out.println("Locking object for writing");
					obj.jvnLockWrite();
					result = method.invoke(obj.jvnGetSharedObject(), args);
					obj.jvnUnLock();
				} else if (type.name().equals("DEBUG")) {
					JvnObjectImpl objImpl = (JvnObjectImpl) obj;
					return objImpl.jvnGetLockState();
				} else {
					result = method.invoke(obj.jvnGetSharedObject(), args);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return result;
	}

}
