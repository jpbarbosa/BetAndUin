package clientRMI;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


import server.ClientOperations;
import servlets.BetServlet;
import servlets.ChatServlet;
import servlets.WebServer;

@SuppressWarnings("serial")
public class Client extends UnicastRemoteObject implements ServerOperations{
	ClientOperations mainServer=null;
	Registry registry = null;

	public Client(Registry registry, ClientOperations mainServer) throws RemoteException {
		super();
		this.registry = registry;
		this.mainServer = mainServer;
	}

	
	@Override
	public void printUserMessage(String msg, String user) throws RemoteException {
		ChatServlet.sendMessage(msg, user);
		//TODO: Complete this.
		/* We are receiving the game results. */
		if (msg.split(" ")[0].equals("=========")){
			BetServlet.sendMessage(msg, user);
		}
	}

	@Override
	public boolean testUser() throws RemoteException {
		return true;
	}
	
	public ClientOperations getMainServer(){
		return mainServer;
	}


	@Override
	public void multiplexer(String msg) throws RemoteException {
		WebServer.multiplexer(msg);
		
	}
}
