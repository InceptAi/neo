const WEB_SOCKET_PORT = 8080;

let webSocket;

function handleSocketOpen() {
	alert("Socket is open");
}

function handleSocketClose() {
	alert("Socket is closed");
}

function processMessageReceivedFromClient(eventInfo) {
	let mesgRecv = eventInfo.data;
	console.log("received msg = " + mesgRecv);
	//convert mesg to json
	actionList = JSON.parse(mesgRecv);
	updateActionList(actionList);
	console.log(Object.values(actionList.viewMap));
	//console.log(actionList);
}

function sendMessageToClient(viewId, actionName) {
	//Send message to Client
	if (webSocket == undefined) {
		console.log("Websocket is null, cannot send data");
		return;
	}
	messageToSend = { viewId: viewId, actionName: actionName };
	messageToSendString = JSON.stringify(messageToSend);
	webSocket.send(messageToSendString);
}

function handleClickEvent(action) {
}

function deleteCurrentActionList() {
  	let currentList = document.getElementById('ul'); //create 'li' element
  	if (currentList != null) {
		while (currentList.firstChild) {
    	currentList.removeChild(currentList.firstChild);
		}
  	}
}

function getInnerHtmlItem(name) {
	return `<a href="#">` + name + `</a>`;
}

function updateActionList(actionList) {
	//JSON object here -- display a list
	//get 'ul' element from the DOM
	let elem = document.
	getElementById('ul'); //get 'ul' element from the DOM
	console.log(actionList.viewMap);

	//Delete the existing actionList first
	deleteCurrentActionList();

	//Create a temp map with names as keys
	let viewMapTemp = new Map();
	for (let key in actionList.viewMap) {	
		viewMapTemp.set(actionList.viewMap[key], key);
	}

	//Get the keys of the temp map
	let names = Array.from(viewMapTemp.keys());

	//Sort by names
	names.sort();

	//populate viewIds
	let viewIds = [];
	for (let name of names) {
		let viewId = viewMapTemp.get(name);
		viewIds.push(viewId);
	}
	console.log(names);
	console.log(viewIds);
	//Print the map
	for (let name of names) {
  		let li = document.createElement('li'); //create 'li' element
		li.innerHTML = getInnerHtmlItem(name);
		elem.appendChild(li); //append 'li' to the 'ul' element
	}

	let elems = document.getElementsByTagName('li');
	Array.from(elems).forEach((v, i) => v.addEventListener('click', function() {
  		Array.from(elems).forEach((c,k) => {c.style.background = 'transparent'; c.innerHTML = getInnerHtmlItem(names[k]);});
  		this.innerHTML = getInnerHtmlItem(names[i] + ', id: ' + viewIds[i]);
  		this.style.background = '#E3F6CE';
		sendMessageToClient(viewIds[i], names[i]);
	}));
}

function initializeWebSocket() {
	if ("WebSocket" in window) {
		alert("WebSocket is supported by your Browser!, initializing websocket");
		// Let us open a web socket
		if (webSocket != undefined) {
			return;
		}
		webSocket = new WebSocket("ws://0.0.0.0:" + WEB_SOCKET_PORT +"/");
		webSocket.onopen = handleSocketOpen;
		webSocket.onmessage = processMessageReceivedFromClient;
		webSocket.onclose = handleSocketClose;
	} else {
 		// The browser doesn't support WebSocket
		alert("WebSocket NOT supported by your Browser!");
	}
}

function endExpertSession() {
	sendCommand("end");
}

function sendBackButtonCommand() {
	sendCommand("back");
}

function sendCommand(command) {
	//Send message to Client
	if (webSocket == undefined) {
		console.log("Websocket is null, cannot send data");
		return;
	}
	messageToSend = { actionName: command };
	messageToSendString = JSON.stringify(messageToSend);
	webSocket.send(messageToSendString);
}
