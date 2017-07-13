const WEB_SOCKET_PORT = 8080;
const RELAY_PORT = 9090;

const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: WEB_SOCKET_PORT });

// Broadcast to all.
wss.broadcast = function broadcast(data) {
  wss.clients.forEach(function each(client) {
    if (client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  });
};

wss.on('connection', function connection(ws) {
  ws.on('message', function incoming(data) {
    // Broadcast to everyone else.
    wss.clients.forEach(function each(client) {
      if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(data);
      }
    });
    console.log('received: %s', data);
  });
});

var connect = require('connect');
var serveStatic = require('serve-static');
connect().use(serveStatic(__dirname)).listen(RELAY_PORT, function(){
	    console.log('Server running on ' + RELAY_PORT + ' ...');
});
