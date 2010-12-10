<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
    <style type="text/css">
    	body {
			background-color: #000000;
			cursor:default;
		}
    .style1 {
		color: #FFFFFF;
		font-family: Arial, Helvetica, sans-serif;
	}
    </style>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>BetAndUin Chat</title>
    </head>
    
    <%
	if (session.getAttribute("user") == null)
	{
	%>
	    <jsp:forward page="/Pages/Login.jsp"></jsp:forward>
	<%
	} 
	%>
    
    <body>
        <div id="display" style="width: 100%;">
            <h2 style="color:#FFFFFF; font-family: Arial, Helvetica, sans-serif;">BetAndUin chat</h2>
            <div id="messagesboard"  style="overflow: auto; cursor:default; background-color:#FFFFCC;  border: medium solid #FFCC00;  top: 80px; width:100%; height: 400px">
			</div>
        </div>
        <div id="input" style="position: fixed; top: 490px; width: 100%; height: 60px">
            <input id="message" type="text" size="90" style="width:100%; bacbackground-color:#FFFFCC;  border: thin solid #FFCC00"/><br/>
            <span class="style1">Send To</span>: 
            <input style="bacbackground-color:#FFFFCC;  border: thin solid #FFCC00" id="destination" type="text" size="20"/><br/>
            <input type="button" onClick="sendMsg()" value="Send" />
        </div>
    </body>
    <script type="text/javascript" src="comet.js"> </script>
    <script type="text/javascript">

    	// Initiate Comet object
    	var comet = Comet("http://localhost:8080/BetAndUinWeb/");
    	var board = document.getElementById('messagesboard');
    	var charsRead = 0;
    	
    	// Register with Server for COMET callbacks.
    	comet.get("ChatServlet?type=register", function(response) {
    		// updates the message board with the new response.
    		board.innerHTML = response;
    		var sub = response.substring(charsRead);
			charsRead = response.length - charsRead;
			
			/* If it's a BetAndUinChat message, it means a user has entered
			 * or exited the room.
			 */
    		if (charsRead > 15 && sub.substring(0,14) == 'BetAndUinChat:'){
    			parent.onlineUsers.location.reload(true);
    		}

    		charsRead = response.length;

    	});
    
    	function sendMsg() {
    		var msg = document.getElementById('message').value;
    		if(msg==""){
    			alert("Message field is empty!");
    		} else {
	    		var dest = document.getElementById('destination').value;
	    		if (dest == "") {
	    			msg = "allusers\n" + msg;
	    		}
	    		else {
	    			msg = dest + "\n" + msg
	    		}
	    			
	    		comet.post("ChatServlet", msg, function(response) {
	    			// Do Nothing
	    		});
	    		// Clears the value of the message element
	    		document.getElementById('message').value = '';
	    
	    	}
    	}
    	
    	function quitChat() {
    		comet.post("ChatServlet?type=exit", '', function(response) {
    			// Exits browser
    			window.location='about:blank';
    		});
    	}
    	
    	
    	//This makes the browser call the quitChat function before unloading(or closing) the page
    	window.onunload = quitChat;
    </script>
</html>