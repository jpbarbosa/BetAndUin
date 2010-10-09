package server;

import java.net.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import clientRMI.RMIClient;
import clientRMI.ServerOperations;


/*TODO: We still have to save the last batch of matches. In case the server goes down,
 * 		the new server will have to read these files.
 */

public class Server extends UnicastRemoteObject implements ClientOperations{
	protected Server() throws RemoteException {
		super();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/*Number of Games per round*/
	static private int nGames = 10; 
	
	/*Set to true if you want the program to display debugging messages.*/
	static private Boolean debugging = true;
	
	/* The object responsible for dealing with the information related to the clients logged in the system. */
    static private ActiveClients activeClients;
    /* The object responsible for creating the matches and setting the results. */
    static private BetScheduler betScheduler;
    /* The object responsible for maintaining the clients' database. */
    static private ClientsStorage database;
    /* The lock we are going to use when the connection manager wants to inform that server that its status
     * (i.e. primary or secondary server) has changed.
     */
    static private ChangeStatusLock changeStatusLock = new ChangeStatusLock();
    /* Variable to know whether we are the default server or not. */
    static private boolean isDefaultServer;
    /* The ports of the two servers. */
    static private int serverPort, partnerPort;
	
    static private int defaultCredits = 100;
	
    public static void main(String args[]){
    	try {
			Server server = new Server();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        /* The user has introduced less than three options by the command line, so we can't carry on. */
        if (args.length < 3){
        	System.out.println("java TCPServer serverPort partnerPort isPrimaryServer (for this last" +
        			"option, type primary or secondary");
    	    System.exit(0);
        }
        
        /* We read from the command line the two port numbers passed. */
        serverPort = Integer.parseInt(args[0]);
        partnerPort = Integer.parseInt(args[1]);
        if (args[2].toLowerCase().equals("primary")){
        	isDefaultServer = true;
        }
        else{
        	isDefaultServer = false;
        }

        if (debugging){
        	System.out.printf("We are server %d, our partner is %d.\n", serverPort, partnerPort);
        }
        
        try{
            
        	/* In here, we initialize the process of exchanging messages between servers. */
            new ConnectionWithServerManager(serverPort, partnerPort, isDefaultServer, changeStatusLock);
    		/* Before going to wait, we have to see whether the manager has concluded its operations
    		 * or not yet. Otherwise, we may wait forever if it concluded before we entered here.
    		 * This step is used in order not to accept any clients before we know whether we are
    		 * the primary server or not.
    		 */
    		synchronized(changeStatusLock){
    			try{
            		if (!changeStatusLock.isInitialProcessConcluded()){
            			if (debugging){
            				System.out.println("We are temporarily sleeping while the primary server is elected...");
            			}
            			/* If this server is elected as secondary server, we won't move from here till
            			 * our status changes once again.
            			 */
            			changeStatusLock.wait();
            		}
				} catch (InterruptedException e) {
					/*We have been awaken by the connection manager, keep going. */
				}
    		}
    		
    		/* Now, we have to check whether we are the primary server or not. */
    		
    		/* We are not the primary server, so we are going to sleep and not attend any clients.
    		 * We will eventually be awaken if our status changes.
    		 */
    		synchronized (changeStatusLock){
    			while (!changeStatusLock.isPrimaryServer()){
	    			try{
	    				if (debugging){
							System.out.println("The server is going to sleep...");
						}
	            		changeStatusLock.wait();
					} catch (InterruptedException e) {
						/*We have been awaken by the connection manager, keep going.*/
						if (debugging){
							System.out.println("The server has been awakened!");
						}
					}
	    		}
    		}
    		   		
    		/* We open the socket connection. */
            ServerSocket listenSocket = new ServerSocket(serverPort);
            if (debugging){
            	System.out.println("Listening at port  " + serverPort);
            	System.out.println("LISTEN SOCKET=" + listenSocket);
            }
    		
    		/* We are the primary server, so we can communicate with clients. */
            
            /* There's the active clients' list to handle, the bets and the database. */
            database = new ClientsStorage();
            activeClients = new ActiveClients();
    		betScheduler = new BetScheduler(activeClients, nGames, database);
    		database.setBetScheduler(betScheduler);
            
            /* Now, we prepare the connection to handle requests from RMI clients. */
    		try {
    			Server rmiServices = new Server();
    			Registry registry = LocateRegistry.createRegistry(12000);
    			registry.rebind("BetAndUinServer", rmiServices);
    			
    			if (debugging){
    				System.out.println("RMI Server ready.");
    			}
    		} catch (RemoteException re) {
    			System.out.println("Exception in Server RMI: " + re);
    		}
            
            
            while(true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                
                if (debugging){
                	System.out.println("CLIENT_SOCKET = " + clientSocket);
                }
                new ConnectionChat(clientSocket, activeClients, betScheduler, database);
            }
        }catch(IOException e){
        	System.out.println("Listen:" + e.getMessage());
        }
    }

    /* METHODS RELATED TO THE RMI */
	@Override
	public String clientLogin(String username, String password, ServerOperations client) throws RemoteException {
		System.out.println("We are here.");
		if (database == null)
			System.out.println("FUCK");
		ClientInfo clientInfo = database.findClient(username);
    	/* This username hasn't been found on the database. */
    	if (clientInfo == null){
    		return "log error";
    	}
    	/* This username has been found on the database. Let's check if the password matches
    	 * with it.
    	 */
    	else{
            if(password.equals(clientInfo.getPassword())){
            	/* However, the user was already validated in some other machine. */
            	if (activeClients.isClientLoggedIn(username)){
            		return "log repeated";
            	}
            	/* The validation process can be concluded. */
            	else{
            		activeClients.addClient(username, null, (RMIClient) client);
            		return "log successful";
            	}
            }
            else {
        		return "log error";
        	}
    	}
	}
	
	@Override
	public String clientRegister(String username, String password, String mail, ServerOperations client) throws RemoteException {    	
    	/* We don't permit that a user registers under the name of 'all',
    	 * because it would interfere with the analysis of the commands
    	 * sent to the server.
    	 */
    	if (username.equals("all")){
    		return "username all";
    	}
    	/* This username hasn't been found in the database, so we can add this new client. */
    	else if (database.findClient(username) == null){
    		/* Creates the register for this new client and adds it to the database. */
    		 database.addClient(username, password, mail);
    		
    		/* Registers in the client as an active client and informs the success of the
    		 * operation.
    		 */
    		activeClients.addClient(username, null, client);
    		return "log successful";
    	}
    	/* This username is already being used. */
    	else{
    		/* Writes to the client. */
    		return "log taken";
    	}
	}

	@Override
	public String clientMakeBet(String user, int gameNumber, String bet, int credits) throws RemoteException {
		/* Variables to save the values inserted by the client. */
    	ClientInfo clientInfo = database.findClient(user);
    	
    	/* If the client is betting more credits than he/she has on his/her account,
    	 * we cannot conclude the bet. Consequently, we have to send a message
    	 * warning the user about it.
    	 */
    	if (clientInfo.getCredits() < credits){
    		return "You don't have enough credits!";
    	}
    	/* The client tried to bet 0 credits. */
    	else if (credits == 0){
    		return "Are you kidding?! You have bet no credits!";
    	}
    	else{
        	synchronized(betScheduler.getManager()){
	        	if((bet.equals("1") || bet.compareToIgnoreCase("x")==0 || bet.equals("2"))
	        			&& betScheduler.isValidGame(gameNumber)){
	        		/* Takes the credits bet from the client's account. */
	        		clientInfo.setCredits(clientInfo.getCredits() - credits);
	        		/* Creates the new bet and saves the new database into file. */
	        		betScheduler.addBet(new Bet(clientInfo.getUsername(),gameNumber,bet,credits));
	        		database.saveObjectToFile("clientsDatabase.bin", database.getClientsDatabase());
	
	        		return "Bet done!";
	        	}
	        	else {
	        		return "Invalid command or the game number that you entered isn't available.";
	        	}
        	}
    	}
	}

	//TODO: Colocar defaultCredits noutro sitio
	public String clientResetCredits(String user) throws RemoteException {
		database.findClient(user).setCredits(defaultCredits);
		return "Your credits were reseted to "+ defaultCredits +"Cr.";
	}

	@Override
	public String clientSendMsgAll(String user, String message) throws RemoteException {
		activeClients.sendMessageAll(user + " says to everyone: " + message, null,
				activeClients.findUser(user).getRMIClient());
		return  "Message ["+message+"] delivered!";
	}

	@Override
	public String clientSendMsgUser(String userSender, String userDest, String message) throws RemoteException {
		String answer="";
		
		if(activeClients.checkUser(userDest)){
    		/* Checks if client isn't sending a message to himself/herself. */
    		if(userDest.equals(userSender)){
    			answer = "What's the point of sending messages to yourself?";
    		} else {
    			activeClients.sendMessageUser(userSender + " says: " + message, userDest);
    			answer = "Message ["+message+"] delivered!";
    		}
    	}else if(database.findClient(userDest) != null){
    		answer = "This client if offline at the moment.";
    	} else {
    		answer = "Username not registered.";
    	}
		
    	return answer;
	}

	public String clientShowCredits(String user) throws RemoteException {
		return "" + database.findClient(user).getCredits();
	}


	public String clientShowMatches() throws RemoteException {
		return betScheduler.getMatches();
	}

	public String clientShowMenu() throws RemoteException {
		return "\nMAIN MENU:" +
				"\n1. Show the current credit of the user: show credits" +
				"\n2. Reset user credits to 100Cr:\n\treset" +
				"\n3. View Current Matches:\n\tshow matches" +
				"\n4. Make a Bet:\n\tbet [match number] [1 x 2] [credits]" +
				"\n5. Show Online Users:\n\tshow users" +
				"\n6. Send messagen to specific user:\n\tsend [user] '[message]'" +
				"\n7. Send message to all users:\n\tsend all '[message]'" + 
				"\n8. Print the menu options:\n\tshow menu";
	}

	public String clientShowUsers() throws RemoteException {
		return activeClients.getUsersList();
	}
}
