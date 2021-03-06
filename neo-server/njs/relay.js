'use strict';

const LIST_ALL_UUIDS_EXPERT_ACTION = "LIST";
const CONNECT_TO_UUID_EXPERT_ACTION = "CONNECT";
const CONNECT_TO_ALL_UUIDS_EXPERT_ACTION = "CONNECT_ALL";

const CLIENT_WEBSOCKET_PORT = 8080;
const EXPERT_WEBSOCKET_PORT = 7070;
const SUCCESS_CODE = 0;
const ERROR_CODE = 1;
const CLIENT_WEBSOCKET_TIMEOUT_MS = 40000;
const SOURCE_RELAY = "RELAY";

//Crawling stuff
const CRAWLING_SERVER_ADDRESS = "dobby1743.duckdns.org";
const CRAWLING_SOCKET_PORT = 9000;


var XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;
var activeSessionsMap = new Map();
var activeBackendSubscribers = new Array();

const WebSocket = require('ws');
const Delayed = require('delayed');

const wssClient = new WebSocket.Server({ port: CLIENT_WEBSOCKET_PORT });
const wssExpert = new WebSocket.Server({ port: EXPERT_WEBSOCKET_PORT });



wssClient.on('connection',  function (webSocket) {
	webSocket.on('message', onClientIncomingMessage);
	webSocket.on('close', scheduleClientCleanupTimer);
	webSocket.on('error', function (ev) {
    	neoLog('Client Websocket error:' + ev.data);
		scheduleClientCleanupTimer();
	});
});


wssExpert.on('connection', function (webSocket) {
	neoLog("In on connection of expert");
	webSocket.on('message', onExpertIncomingMessage);  
	webSocket.on('error', function (ev) {
    	neoLog('Expert Websocket error:' + ev.data);
	});
	webSocket.on('close', function () {
  		removeExpertFromActiveSession(this);
	});
});


function scheduleClientCleanupTimer() {
	var delayedCleanupTimer = Delayed.delay(clearUser, CLIENT_WEBSOCKET_PORT, this, this);
	if (this.activeSession !== undefined) {
		neoLog("Setting timeout for uuid:" + this.activeSession.userUuid);
		this.activeSession.pendingTimer = delayedCleanupTimer;
	} else {
		neoLog("Active session is undefined");
	}
}

function getActiveSession(uuid) {
    neoLog('getActiveSession called for user uuid:' + uuid);
	var session = activeSessionsMap.get(uuid);
	if (session === undefined) {
		neoLog('Creating new session for user uuid:' + uuid);
		session = createActiveSession(uuid);
		activeSessionsMap.set(uuid, session);
	}
	return session;
}

function clearUser(webSocket) {
  var activeSession = webSocket.activeSession;
  if (activeSession !== undefined) {
  	  neoLog("Clearing User uuid: " + webSocket.activeSession.userUuid);
      //Send an update to the expert -- removing this user
  	  neoLog("Clearing User from sessions map");
      var expertListening = activeSession.expertWebSocketList;
      var uuid = activeSession.userUuid;
      activeSessionsMap.delete(uuid);
      //TODO: Should we send message to ALL experts
      if (expertListening !== undefined) {
        sendUpdatedUUIDListToExperts(expertListening);
      }
     /// remove user's websocket.
      activeSession.userWebSocket = undefined;
      webSocket.activeSession == undefined;
  }  
}


function sendUpdatedUUIDListToExperts(expertWebSocketList) {
	neoLog('sending updated UUID list to experts: socketListLength: ' + expertWebSocketList.length);
    var messageToSend = getUUIDListMessage();
    expertWebSocketList.forEach(function each(socket) {
		sendMessageToExpert(socket, messageToSend)
    });
}

function createActiveSession(uuid) {
	var activeSession = {
		userUuid: uuid,
		userWebSocket: undefined,
		expertWebSocketList: [],
	};
	for (var subscriberIndex = 0; subscriberIndex < activeBackendSubscribers.length; subscriberIndex++) {
    	activeSession.expertWebSocketList.push(activeBackendSubscribers[subscriberIndex]);
	}
	return activeSession;
}

function onClientIncomingMessage(message) {
    // 1. Parse the user UUID.
	
	//Send this message to crawling backend
	sendMessageToCrawlingBackend(message);
    
	var parsedMessage = undefined;
    try {
        parsedMessage = JSON.parse(message);
    } catch (e) {
        neoLog('Unable to parse client message: ' + message + ' exception:' + e);
        return;
    }

    if (parsedMessage.uuid === undefined && this.activeSession === undefined) {
        neoLog('undefined uuid, no active session. Dropping message: ' + message);
        return;
    }

    // 2. get or create ActiveSession.
    if (parsedMessage.uuid !== undefined) {
        neoLog('uuid available = ' + parsedMessage.uuid);
        var session = getActiveSession(parsedMessage.uuid);
        // Set activeSession field on websocket.activeSession.
        this.activeSession = session;
		this.activeSession.userWebSocket = this;
    }

    if (this.activeSession === undefined) {
        neoLog('Error: Active session does not exist for incoming message: ' + message);
        return;
    }

    if (this.activeSession.pendingTimer !== undefined) {
		neoLog("Clearing timeout for uuid:" + this.activeSession.userUuid);
        clearTimeout(this.activeSession.pendingTimer);
        this.activeSession.pendingTimer = undefined;
    }
    // 3. Relay messages to an expert if one exists.
    this.activeSession.expertWebSocketList.forEach(function each(client) {
        if (client !== this && client.readyState === WebSocket.OPEN) {
            client.send(message);
        }
    });
    // NOT DOING 4. Start a thread to serve this request. Use clusters library.
}


function sendMessageToCrawlingBackend(jsonMessage) {
    let xhr = new XMLHttpRequest();
    let url = "http://" + CRAWLING_SERVER_ADDRESS + ":" + CRAWLING_SOCKET_PORT;
    xhr.open("POST", url, true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(jsonMessage);
    //xhr.send(JSON.stringify({
    //  value: value
    //}));
}


/// Expert message processing logic:

function getUUIDList() {
	return Array.from(activeSessionsMap.keys());
}

function getUUIDListMessage() {
    return { uuidList :  getUUIDList(), code : SUCCESS_CODE };	
}

function removeExpertFromActiveSession(expertWebSocket) {
	if (expertWebSocket.activeSession !== undefined) {
		var indexOfExpertInSocketList = expertWebSocket.activeSession.expertWebSocketList.indexOf(expertWebSocket);
		if (indexOfExpertInSocketList >= 0) {
			expertWebSocket.activeSession.expertWebSocketList.splice(indexOfExpertInSocketList, 1);
		}
   }
   expertWebSocket.activeSession = undefined;
}

function addExpertToBroadcastList(expertWebSocket, uuid) {
	
	var activeSession = activeSessionsMap.get(uuid);
	
	if (activeSession === undefined || uuid === undefined) {
		return { response : "ERROR: uuid does not exist: uuid " + uuid, code : ERROR_CODE };
	}
	
	removeExpertFromActiveSession(expertWebSocket);
	activeSession.expertWebSocketList.push(expertWebSocket);
	expertWebSocket.activeSession = activeSession;
	return { response: "SUCCESS: connected to uuid: " + uuid, code : SUCCESS_CODE };
}

function addExpertToAllClientsBroadcastList(expertWebSocket) {
	var numUUIDsActive = 0;
	for(var uuid in activeSessionsMap) {
		numUUIDsActive = numUUIDsActive + 1;
		addExpertToBroadcastList(expertWebSocket, uuid);
	}
	activeBackendSubscribers.push(expertWebSocket);	
	return { response: "SUCCESS: connected to num UUIDs: " + numUUIDsActive, code : SUCCESS_CODE };
}

function processExpertAction(webSocket, parsedMessage) {
	var messageResponse = undefined;
	if (parsedMessage.serverAction == LIST_ALL_UUIDS_EXPERT_ACTION) {
		//send list of all UUIDs to expert
		messageResponse = getUUIDListMessage();
		//messageResponse = { uuidList :  getUUIDList(), code : SUCCESS_CODE };	
	} else if (parsedMessage.serverAction == CONNECT_TO_UUID_EXPERT_ACTION) {
		//Connect to a client and set it for this expert
		messageResponse = addExpertToBroadcastList(webSocket, parsedMessage.uuid);
	} else if (parsedMessage.serverAction == CONNECT_TO_ALL_UUIDS_EXPERT_ACTION) {
		//Connect to a client and set it for this expert
		messageResponse = addExpertToAllClientsBroadcastList(webSocket);
	} else {
		messageResponse = { response : "ERROR: Invalid action", code : ERROR_CODE }; 
        neoLog('invalid action, expert message:' + message);
	}
	sendMessageToExpert(webSocket, messageResponse);
}

function onExpertIncomingMessage(message) {
	//1. Parse to see if special
	//2. Two: List -- return list of all UUIDs
	//3. Connect -- to given UUID

	neoLog("Got message: " + message);
    var parsedMessage = undefined;
    try {
        parsedMessage = JSON.parse(message);
    } catch (e) {
        neoLog('Unable to parse expert message: ' + message + ' exception:' + e);
        return;
    }

	var serverAction = parsedMessage.serverAction;
	if (serverAction != undefined) {
		processExpertAction(this, parsedMessage);
		return;
	}

	//Server action undefined so relay message
    if (this.activeSession === undefined) {
		var errorMessage = 'undefined server action / active session, Dropping message: ' + message;
		sendMessageToExpert(this, { response : errorMessage, code : ERROR_CODE });
		//this.send(JSON.stringify({ response : errorMessage, code : ERROR_CODE }));
        neoLog('undefined server action and undefined active Session for relay message, Dropping message: ' + message);
        return;
    }

    // 3. Relay messages to an expert if one exists.
	var clientToSend = this.activeSession.userWebSocket;
	if (clientToSend !== undefined && clientToSend !== this && clientToSend.readyState === WebSocket.OPEN) {
		clientToSend.send(message);
 	} else {
		neoLog("client is not ready to receive expert message");
	}
}

function neoLog(logMessage) {
    console.log(logMessage);
}

function sendMessageToExpert(webSocket, message) {
	if (webSocket === undefined || webSocket.readyState !== WebSocket.OPEN) {
		neoLog("Error in sendMessageToExpert, webSocket is not ready");
		return;
	}
	message.source = SOURCE_RELAY;
	webSocket.send(JSON.stringify(message));
}

/// Static HTML index.html serving logic below:

const HTML_PORT = 9090;
var connect = require('connect');
var serveStatic = require('serve-static');
connect().use(serveStatic(__dirname)).listen(HTML_PORT, function() {
	    neoLog('Server running on ' + HTML_PORT + ' ...');
});

