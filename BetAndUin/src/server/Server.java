/* By:
 * 		Ivo Daniel Venhuizen Correia, no 2008110814
 * 		Jo�o Pedro Gaioso Barbosa, no 2008111830
 * 
 * Distributed Systems, October 2010 
 */

package server;

import intraServerCommunication.ChangeStatusLock;
import intraServerCommunication.ConnectionWithServerManager;

import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import bets.Bet;
import bets.BetScheduler;

import common.Constants;

import clientRMI.ServerOperations;


public class Server extends UnicastRemoteObject implements ClientOperations{
	private Server(boolean defaultS, int sPort, int pPort, int rPort, int sStonith, int pStonith, String ip) throws RemoteException{
		super();
		isDefaultServer = defaultS;
		serverPort = sPort;
		partnerPort = pPort;
		rmiPort = rPort;
		partnerStonith = pStonith;
		stonithPort = sStonith;
		
    	/* In here, we initialize the process of exchanging messages between servers. */
        new ConnectionWithServerManager(serverPort, partnerPort, stonithPort, partnerStonith, isDefaultServer, changeStatusLock, ip);
		/* Before going to wait, we have to see whether the manager has concluded its operations
		 * or not yet. Otherwise, we may wait forever if it concluded before we entered here.
		 * This step is used in order not to accept any clients before we know whether we are
		 * the primary server or not.
		 */
	}
	
	private Server(ActiveClients active, BetScheduler bet, GlobalDataBase base) throws RemoteException {
		super();
		activeClients = active; 
		betScheduler = bet;
		database = base;
	}

	private static final long serialVersionUID = 1L; 
	
	/* The object responsible for dealing with the information related to the clients logged in the system. */
    private ActiveClients activeClients  = new ActiveClients();;
    /* The object responsible for creating the matches and setting the results. */
    private BetScheduler betScheduler;
    /* The object responsible for maintaining the clients' database. */
    private GlobalDataBase database = new GlobalDataBase();;
    /* The lock we are going to use when the connection manager wants to inform that server that its status
     * (i.e. primary or secondary server) has changed.
     */
    private ChangeStatusLock changeStatusLock = new ChangeStatusLock();
    /* Variable to know whether we are the default server or not. */
    private boolean isDefaultServer;
    /* The ports of the two servers. */
    private int serverPort, partnerPort;
    /* The RMI Server port. */
    private int rmiPort;
    /* The Stonith simulation port.*/
	private int partnerStonith;
	private int stonithPort;
    
    public static void main(String args[]){
    	/* Reads the properties. */
		Constants.readProperties("properties.conf");
    	boolean defaultS = false;
    	int serverNumber = 0;
    	int sPort = 0, pPort = 0,sRMIPort = 0, pStonith = 0, sStonith = 0;
    	Server server = null;
    	
    	 /* The user has introduced less than three options by the command line, so we can't carry on. */
        if (args.length < 2 || args.length > 3){
        	System.out.println("java -jar [fileName] [serverNumber] [partnerIpAddress] -debugging");
    	    System.exit(0);
        }

        if (args.length == 3 && args[2].equalsIgnoreCase("debugging")){
        	Constants.DEBUGGING_SERVER = true;
        	System.out.println("Debugging flag activated.");
        }
        /* We read from the command line the two port numbers passed. */
        try{
        	serverNumber = Integer.parseInt(args[0]);
        }catch(Exception e){
        	System.out.println("Invalid argument for [serverNumber].");
        	System.exit(-1);
        }
        if (serverNumber == 1){
        	/* We are default server. */
        	defaultS = true;
        	
        	/* Port configurations. */
        	sPort = Constants.FIRST_TCP_SERVER_PORT;
        	pPort = Constants.SECOND_TCP_SERVER_PORT;
        	sRMIPort = Constants.FIRST_RMI_SERVER_PORT;
        	sStonith = Constants.STONITH_FIRST_SERVER_PORT;
        	pStonith = Constants.STONITH_SECOND_SERVER_PORT;
        }
        else if (serverNumber == 2){
        	/* We are default server. */
        	defaultS = false;
        	
        	/* Port configurations. */
        	sPort = Constants.SECOND_TCP_SERVER_PORT;
        	pPort = Constants.FIRST_TCP_SERVER_PORT;
        	sRMIPort = Constants.SECOND_RMI_SERVER_PORT;
        	sStonith = Constants.STONITH_SECOND_SERVER_PORT;
        	pStonith = Constants.STONITH_FIRST_SERVER_PORT;
        }
        else{
        	System.out.println("Invalid argument for [serverNumber].");
        	System.exit(-1);
        }
        
        if (Constants.DEBUGGING_SERVER){
        	System.out.printf("Server: We are server %d, our partner is %d.\n", sPort, pPort);
        }
    	try {
			System.out.println("Initializing the server...");
			
    		server = new Server(defaultS, sPort, pPort, sRMIPort, sStonith, pStonith, args[1]);
			server.run();
		} catch (RemoteException e) {
			System.out.println("Error creating the server: " + e);
			System.exit(-1);
		}
    	
    }
    	
    public void run(){
        try{
    		synchronized(changeStatusLock){
    			try{
            		if (!changeStatusLock.isInitialProcessConcluded()){
            			if (Constants.DEBUGGING_SERVER){
            				System.out.println("Server: We are temporarily sleeping while the primary server is elected...");
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
	    				if (Constants.DEBUGGING_SERVER){
							System.out.println("Server: The server is going to sleep...");
						}
	            		changeStatusLock.wait();
					} catch (InterruptedException e) {
						/*We have been awaken by the connection manager, keep going.*/
						if (Constants.DEBUGGING_SERVER){
							System.out.println("Server: The server has been awakened!");
						}
					}
	    		}
    		}
    		/* We open the socket connection. */
            ServerSocket listenSocket = new ServerSocket(serverPort);
            if (Constants.DEBUGGING_SERVER){
            	System.out.println("Server: Listening at port  " + serverPort);
            	System.out.println("Server: LISTEN SOCKET=" + listenSocket);
            }
    		
    		/* We are the primary server, so we can communicate with clients. */
            
            /* There's the active clients' list to handle, the bets and the database. */
    		betScheduler = new BetScheduler(activeClients, database);
    		database.setBetScheduler(betScheduler);
            
            /* Now, we prepare the connection to handle requests from RMI clients. */
    		try {
    			Server rmiServices = new Server(activeClients, betScheduler, database);
    			Registry registry = LocateRegistry.createRegistry(rmiPort);
    			registry.rebind("BetAndUinServer", rmiServices);
    			
    			if (Constants.DEBUGGING_SERVER){
    				System.out.println("Server: RMI Server ready.");
    			}
    		} catch (RemoteException re) {
    			System.out.println("Exception in Server RMI: " + re);
    		}
            
    		System.out.println("Server ready!");
    		
            while(true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                
                if (Constants.DEBUGGING_SERVER){
                	System.out.println("Server: CLIENT_SOCKET = " + clientSocket);
                }
                new TCPClientThread(this, clientSocket, activeClients, betScheduler, database);
            }
        }catch(IOException e){
        	System.out.println("Listen:" + e.getMessage());
        }
    }

    /* METHODS RELATED TO THE RMI */
    
	@Override
	public String clientLogin(String username, String password, ServerOperations client, boolean isWeb) throws RemoteException {
		ClientInfo clientInfo = database.findClient(username);
    	/* This username hasn't been found on the database. */
    	if (clientInfo == null){
    		return "user not registed";
    	}
    	/* This username has been found on the database. Let's check if the password matches
    	 * with it.
    	 */
    	else{
            if(password.equals(clientInfo.getPassword())){
            	/* However, the user was already validated in some other machine. */
            	if(activeClients.isClientLoggedIn(username) == true){
            		return "log repeated";
            	}
            	/* The validation process can be concluded. */
            	else{
            		activeClients.addClient(username, null, client, isWeb);
            		return "log successful";
            	}
            }
            else {
        		return "log error";
        	}
    	}
	}
	
	@Override
	public String clientRegister(String username, String password, String mail, ServerOperations client, boolean isWeb) throws RemoteException {    	
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
    		activeClients.addClient(username, null, client, isWeb);
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

	public String clientResetCredits(String user) throws RemoteException {
		database.findClient(user).setCredits(Constants.DEFAULT_CREDITS);
		return "Your credits were reseted to "+ Constants.DEFAULT_CREDITS +"Cr.";
	}

	@Override
	public String clientSendMsgAll(String user, String message) throws RemoteException {
		activeClients.sendMessageAll(user + " says to everyone: " + message, null,
				activeClients.getActiveClient(user).getRMIClient());
		return  "Message ["+message+"] delivered!";
	}

	@Override
	public String clientSendMsgUser(String userSender, String userDest, String message) throws RemoteException {
		String answer="";
		ClientListElement element;
		
		if((element = activeClients.getActiveClient(userDest)) != null){
    		/* Checks if client isn't sending a message to himself/herself. */
				if(userDest.equals(userSender)){
					answer = "What's the point of sending messages to yourself?";
					if (element.getRMIClient() != null){
						try{
		    				element.getRMIClient().testUser();
		    				activeClients.sendMessageUser("BetAndUin: " + message, userDest);
						}catch(Exception e){
							
						}
					}
				}	
			
    			/* Now, we have check if this is a RMI Client and is still active. */
				else if (element.getRMIClient() == null){
    				activeClients.sendMessageUser(userSender + " says: " + message, userDest);
        			answer = "Message ["+message+"] delivered!";
    			}
    			/* Is a RMI Client. */
    			else{
    				try{
	    				element.getRMIClient().testUser();
	    				activeClients.sendMessageUser(userSender + " says: " + message, userDest);
	        			answer = "Message [" + message + "] delivered!";
    				}catch(Exception e){
    					answer = userDest + " is offline at the moment.";
    				}
    			}
    	}else if(database.findClient(userDest) != null){
    		answer = "This client if offline at the moment.";
    	} else {
    		answer = "Username not registered.";
    	}		
    	return answer;
	}
	
	@Override
	public String clientShowCredits(String user) throws RemoteException {
		return "" + database.findClient(user).getCredits();
	}

	@Override
	public String clientShowMatches() throws RemoteException {
		return betScheduler.getMatches();
	}

	@Override
	public String clientShowMenu() throws RemoteException {
		return "\nMAIN MENU:" +
				"\n1. Show the current credit of the user: show credits" +
				"\n2. Reset user credits to 100Cr:\n\treset" +
				"\n3. View Current Matches:\n\tshow matches" +
				"\n4. Make a Bet:\n\tbet [match number] [1 x 2] [credits]" +
				"\n5. Show Online Users:\n\tshow users" +
				"\n6. Send messagen to specific user:\n\tsend [user] '[message]'" +
				"\n7. Send message to all users:\n\tsend all '[message]'" + 
				"\n8. Print the menu options:\n\tshow menu" +
				"\n9. Leave the program:\n\texit";
	}

	@Override
	public String clientShowUsers() throws RemoteException {
		return activeClients.getUsersList();
	}
	
	@Override
	public void clientLeave(String username) throws RemoteException {
		System.out.println("We are removing " + username + "...");
		activeClients.removeClient(username);
	}

	@Override
	public void addWebMultiplexer(ServerOperations webMultiplexer)
			throws RemoteException {
		System.out.println("We just added the webMultiplexer.");
		activeClients.addWebMultiplexer(webMultiplexer);
		
	}
}

