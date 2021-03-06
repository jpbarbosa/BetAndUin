/* By:
 * 		Ivo Daniel Venhuizen Correia, no 2008110814
 * 		Jo�o Pedro Gaioso Barbosa, no 2008111830
 * 
 * Distributed Systems, October 2010 
 */

package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import clientRMI.ServerOperations;


/* This class holds all the active clients in the current session.
 * For efficiency, we use two structures for keeping the list:
 *     -> HASHTABLE: When we want to access a specific client (e.g. when
 *             we want to terminate a client thread and we have to remove
 *             it from the linked list with all the clients. The second
 *             situation is when we want to send a message to a single
 *             client).
 *             
 *     ->LINKED LIST: The whole list of the active clients. This structure is
 *             used when we want to send a message to the whole list of clients
 *             (chat option). When we want to perform this action, an hash table
 *             is useless.
 */

public class ActiveClients {
	private Hashtable<String, ClientListElement> clientHash;
	private List <ClientListElement> clientList;
	
	private int noActiveClients;
	
	public ActiveClients(){
		 clientHash = new Hashtable<String, ClientListElement>();
		 clientList = new LinkedList<ClientListElement>();
		 noActiveClients = 0;
	}
	
	public synchronized void addClient(String username, Socket socket, ServerOperations client, boolean isWeb){
		/* This method adds a client to both the hash table and the list.*/
		ClientListElement element = new ClientListElement(username,socket, client, isWeb);
		
		clientList.add(element);
		clientHash.put(username, element);
		
		noActiveClients++;
	}
	
	public synchronized void removeClient(String username){
		/* We have to check it because if the client ends the connection before sending data,
		 * we will have a null element as username.
		 */
		System.out.println("We entered this function.");
		if (username == null || username.equals("")){
			return;
		}
		
		/* Gets the element from the hash table. */
		ClientListElement element = clientHash.get(username);
		
		/* Removes it first from the hash table and then from the list of active
		 * clients, decrementing their number afterwards.
		 */
		
		clientHash.remove(username);
		clientList.remove(element);
		
		noActiveClients--;
		
	}
	
	/* Method to check whether a given client is already logged in or not. */
	public synchronized boolean isClientLoggedIn(String username){
		/* The client couldn't be found in the hash table, so it means it isn't logged in -> false. */
		/* There is already an entry in the active clients' hash table -> true. */
		ClientListElement element = clientHash.get(username);
		
     	if (element != null){
     		if(element.getSocket() == null && element.getRMIClient()!=null){
	    		try{
	    			/*We are not sure if the rmi client is in fact logged in because the connection
	    			 * can be down and the server knows by doing this kind of ping mechanism*/
	    			element.getRMIClient().testUser();
	    		} catch(Exception e){
	    			/* Ping didn't work so the client is down */
	        		return false;
	    		}
     		}
     		return true;
     	}
     	return false;
	}
	
	public synchronized void sendMessageAll(String msg, Socket clientSocket, ServerOperations clientRMI){
		synchronized(clientList){
			/* Sends a message to all the clients. */
			
			ClientListElement element;
			DataOutputStream out;
						
			/* Uses an iterator over the list to send a message to all the active clients. */
			for (int i = 0; i < clientList.size(); i++)
		    {
				String message;
				element = clientList.get(i);
				
				if (!element.isWeb){
					message = msg.replaceAll("<br>", "\n");
				}
				else
					message = msg;
				
				/* If this is the user who sends the message, it won't receive it back. */
				if (element.getSocket() != null && element.getSocket() != clientSocket){	
					try {
						out = new DataOutputStream(element.getSocket().getOutputStream());
						out.writeUTF(message);
					} catch (IOException e) {
						System.out.println("IO from sendMessageAll (ActiveClients): " + e);
					}
				}
				else if(element.getRMIClient() != clientRMI){
					try {
						element.getRMIClient().printUserMessage(message, element.getUsername());
					} catch (Exception e1){
						System.out.println("Check point 1");
						/* This means that the client has logged off and consequently, we can remove it
						 * from the active list.
						 */
						removeClient(element.getUsername());
						/* We have to go back one position. */
						i--;
					}
				}
		    }
		}
	}
	
	/* This method is used when we have a valid login for this client and consequently,
	 * we can use his/her username to send messages from the system to the respective
	 * owner of the account.
	 * It can also be used when we want to send a message concerning a bet. That's why
	 * we need to verify if the client is still logged in or not.
	 */
	public synchronized void sendMessageUser(String message, String username){
		/* Sends a message to a specific user. */
		DataOutputStream out;
		/* Get the element using the hash table. */
		ClientListElement element = clientHash.get(username);
		if (element != null){
			try {
				/* This is a TCP Client. */
				if (element.getSocket() != null){
					out = new DataOutputStream(element.getSocket().getOutputStream());
					out.writeUTF(message);
				}
				/* This is a RMI Client. */
				else{
					element.getRMIClient().printUserMessage(message, element.getUsername());
				}
				
			} catch (IOException e) {
				System.out.println("IO from sendMessageUser (ActiveClients): " + e);
			}
		}
	}
	
	/* This method is used when we still don't have a valid login and we want to send
	 * a message to the client (e.g. the login fails).
	 */
	public synchronized void sendMessageBySocket(String message, Socket clientSocket){
		/* Sends a message to a specific user, using directly the socket connection. */
		DataOutputStream out;
		
		try {
			out = new DataOutputStream(clientSocket.getOutputStream());
			out.writeUTF(message);
		} catch (IOException e) {
			System.out.println("IO from sendMessageBySocket (ActiveClients): " + e);
		}
	}
	
	/*This method returns a String with all the activeUsers*/
	public synchronized String getUsersList(){
		String usersList="";
		
		int i=0;
		
		for (i = 0; i < clientList.size() - 1; i++){
			usersList += clientList.get(i).getUsername()+"\n";
		}
		
		/* We take out the '\n' from the last user. */
		usersList += clientList.get(i).getUsername();
		
		return usersList;
	}
	
	public synchronized ClientListElement getActiveClient(String user){
		return clientHash.get(user);
	}
	
}

/* Class used to insert active elements in the list. */
class ClientListElement{
	String username;
	Socket socket;
	ServerOperations rmiClient;
	boolean isWeb;
	
	public ClientListElement(String user, Socket socketArg, ServerOperations client, boolean isWeb){
		username = user;
		socket = socketArg;
		rmiClient = client;
		this.isWeb = isWeb;
	}

	public String getUsername() {
		return username;
	}

	public Socket getSocket() {
		return socket;
	}
	
	public ServerOperations getRMIClient() {
		return rmiClient;
	}

}

