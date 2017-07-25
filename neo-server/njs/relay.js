'use strict';

const CLIENT_SOCKET_PORT = 8080;
const EXPERT_SOCKET_PORT = 7070;

var activeSessionsMap = new Map();


const WebSocket = require('ws');

const wssClient = new WebSocket.Server({ port: CLIENT_WEBSOCKET_PORT });
const wssExpert = new WebSocket.Server({ port: EXPERT_WEBSOCKET_PORT });


wssClient.onconnection = function (websocket) {
	websocket.onmessage = onClientIncomingMessage;
}

wssClient.onerror = function (ev) {
    neoLog('Client Websocket error:' + ev.data);
};

/////////////// OLD
wssClient.on('connection', function connection(websocket) {
 
  websocket.on('message', function incoming(data) {
    // Broadcast to everyone else.
    wssClient.clients.forEach(function each(client) {
      if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    });
    neoLog('received: %s', data);
  });
});
/////////////// OLD

wssExpert.onconnection = function (websocket) {
  websocket.onmessage = onExpertIncomingMessage;  
};

wssExpert.onerror = function (ev) {
    neoLog('Expert Websocket error:' + ev.data);
};

function getActiveSession(uuid) {
    neoLog('getActiveSession called for user uuid:' + uuid);
	var session = activeSessionMap.get(uuid);
	if (session === undefined) {
		neoLog('Creating new session for user uuid:' + uuid);
		session = createActiveSession(uuid);
		activeSessionMap.set(uuid, session);
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
        neoLog('uuid available = ' + uuid);
        var session = getActiveSession(uuid);
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
            client.send(data);
        }
    });

    // NOT DOING 4. Start a thread to serve this request. Use clusters library.
}


/// Expert message processing logic:
//

function onExpertIncomingMessage(message) {
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

