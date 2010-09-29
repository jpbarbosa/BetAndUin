package server;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
	Hashtable<String, ClientListElement> clientHash;
	List <ClientListElement> clientList;
	
	int noActiveClients;
	
	public ActiveClients(){
		 clientHash = new Hashtable<String, ClientListElement>();
		 clientList = new LinkedList<ClientListElement>();
		 noActiveClients = 0;
	}
	
	public synchronized void addClient(String username, Socket socket){
		/* This method adds a client to both the hash table and the list.*/
		ClientListElement element = new ClientListElement(username,socket);
		
		clientList.add(element);
		clientHash.put(username, element);
		
		noActiveClients++;
	}
	
	public synchronized void removeClient(String username){
		/* This method removes a client. */
		
		/* We have to check it because if the client ends the connection before sending data,
		 * we will have a null element as username.
		 */
		
		if (username.equals("")){
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
	public synchronized Boolean isClientLoggedIn(String username){
		ClientListElement element;
		element = clientHash.get(username);
		
		if (element == null){
			/* The client couldn't be found in the hash table, so it means it isn't logged in. */
			return false;
		}
		else{
			/* There is already an entry in the active clients' hash table. */
			return true;
		}
	}
	
	public synchronized void sendMessageAll(String message, Socket clientSocket){
		/* Sends a message to all the clients. */
		
		ClientListElement element;
		ListIterator <ClientListElement> iterator = clientList.listIterator();
		DataOutputStream out;
		
		/* Uses an iterator over the list to send a message to all the active clients. */
		while(iterator.hasNext())
	    {
			element = (ClientListElement) iterator.next();
			/* If this is the user who sends the message, it won't receive it back. */
			if (element.getSocket() != clientSocket){	
				try {
					out = new DataOutputStream(element.getSocket().getOutputStream());
					out.writeUTF(message);
				} catch (IOException e) {
					System.out.println("IO from sendMessageAll (ActiveClients): " + e);
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
				out = new DataOutputStream(element.getSocket().getOutputStream());
				out.writeUTF(message);
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
	public String getUsersList(){
		String usersList="";
		
		int i=0;
		while(i<clientList.size() - 1){
			usersList+=clientList.get(i).getUsername()+"\n";
			i++;
		}
		usersList+=clientList.get(i).getUsername();
		
		return usersList;
	}
	
	/**@return true if user exists in clientList*/
	public boolean checkUser(String user){
		int i=0;
		while(i<clientList.size()){
			if(clientList.get(i).getUsername().equals(user))
				return true;
			i++;
		}
		return false;
	}
}

/* Class used to insert active elements in the list. */
class ClientListElement{
	String username;
	Socket socket;
	
	public ClientListElement(String user, Socket s){
		username = user;
		socket = s;
	}

	public String getUsername() {
		return username;
	}

	public Socket getSocket() {
		return socket;
	}

}

