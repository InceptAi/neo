'use strict';

const LIST_ALL_UUIDS_EXPERT_ACTION = "LIST";
const CONNECT_TO_UUID_EXPERT_ACTION = "CONNECT";

const CLIENT_WEBSOCKET_PORT = 8080;
const EXPERT_WEBSOCKET_PORT = 7070;
const SUCCESS_CODE = 0;
const ERROR_CODE = 1;

var activeSessionsMap = new Map();


const WebSocket = require('ws');

const wssClient = new WebSocket.Server({ port: CLIENT_WEBSOCKET_PORT });
const wssExpert = new WebSocket.Server({ port: EXPERT_WEBSOCKET_PORT });


wssClient.on('connection',  function (websocket) {
	websocket.on('message', onClientIncomingMessage);
});

/*
wssClient.on('connection', function connection(websocket) {
	websocket.on('message', function incoming(data) {
	// Broadcast to everyone else.
-    wssClient.clients.forEach(function each(client) {
-      if (client !== ws && client.readyState === WebSocket.OPEN) {
-        client.send(data);
-      }
-    });
-    neoLog('received: %s', data);
-  });
-});
*/

wssClient.on('error', function (ev) {
    neoLog('Client Websocket error:' + ev.data);
});

wssExpert.on('connection', function (websocket) {
  neoLog("In on connection of expert");
  websocket.on('message', onExpertIncomingMessage);  
});

wssExpert.on('error', function (ev) {
    neoLog('Expert Websocket error:' + ev.data);
});

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

function createActiveSession(uuid) {
	var activeSession = {
		userUuid: uuid,
		userWebSocket: undefined,
		expertWebSocketList: [],
	};
	return activeSession;
}

function onClientIncomingMessage(message) {
    // 1. Parse the user UUID.
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
    }

    if (this.activeSession === undefined) {
        neoLog('Error: Active session does not exist for incoming message: ' + message);
        return;
    }

    // 3. Relay messages to an expert if one exists.
    this.activeSession.expertWebSocketList.forEach(function each(client) {
        if (client !== this && client.readyState === WebSocket.OPEN) {
            client.send(message);
        }
    });

    // NOT DOING 4. Start a thread to serve this request. Use clusters library.
}


/// Expert message processing logic:

function getUUIDList() {
	return Array.from(activeSessionsMap.keys());
}

function addExpertToBroadcastList(expertWebsocket, uuid) {
	var activeSession = activeSessionsMap.get(uuid);
	if (activeSession === undefined || uuid === undefined) {
		return { response : "ERROR: uuid does not exist: uuid " + uuid, code : ERROR_CODE };
	}
	activeSession.expertWebSocketList.push(expertWebsocket);
	expertWebsocket.activeSession = activeSession;
	return { response: "SUCCESS: connected to uuid: " + uuid, code : SUCCESS_CODE };
}

function processExpertAction(websocket, parsedMessage) {
	var messageResponse = undefined;
	if (parsedMessage.serverAction == LIST_ALL_UUIDS_EXPERT_ACTION) {
		//send list of all UUIDs to expert
		messageResponse = { uuidList :  getUUIDList(), code : SUCCESS_CODE };	
	} else if (parsedMessage.serverAction == CONNECT_TO_UUID_EXPERT_ACTION) {
		//Connect to a client and set it for this expert
		messageResponse = addExpertToBroadcastList(websocket, parsedMessage.uuid);
	} else {
		messageResponse = { response : "ERROR: Invalid action", code : ERROR_CODE }; 
        neoLog('invalid action, expert message:' + message);
	}
	websocket.send(JSON.stringify(messageResponse));
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
		this.send({ response : errorMessage, code : ERROR_CODE })
        neoLog('undefined server action and undefined active Session for relay message, Dropping message: ' + message);
        return;
    }

    // 3. Relay messages to an expert if one exists.
	var clientToSend = this.activeSession.userWebSocket;
	if (clientToSend !== this && clientToSend.readyState === WebSocket.OPEN) {
		clientToSend.send(data);
 	} else {
		neoLog("client is not ready to receive expert message");
	}
}

function neoLog(logMessage) {
    console.log(logMessage);
}

/// Static HTML index.html serving logic below:

const HTML_PORT = 9090;
var connect = require('connect');
var serveStatic = require('serve-static');
connect().use(serveStatic(__dirname)).listen(HTML_PORT, function() {
	    neoLog('Server running on ' + HTML_PORT + ' ...');
});

