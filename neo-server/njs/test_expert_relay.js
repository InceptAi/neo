'use strict';

const CLIENT_SOCKET_PORT = 8080;
const EXPERT_SOCKET_PORT = 7070;

const LIST_ALL_UUIDS_EXPERT_ACTION = "LIST";
const CONNECT_TO_UUID_EXPERT_ACTION = "CONNECT";

const WebSocket = require('ws');

const ws = new WebSocket('ws://192.168.1.129:7070');

ws.on('open', function open() {
    ws.send({ serverAction: LIST_ALL_UUIDS_EXPERT_ACTION});
});

ws.on('message', function message(data) {
    console.log('Incoming message:' + data);

});

const wsClient = new WebSocket('ws://192.168.1.129:8080');

ws.on('open', function open() {
});

ws.on('message', function message() {

});


