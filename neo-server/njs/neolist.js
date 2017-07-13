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

function updateActionList(actionList) {
	//JSON object here -- display a list
	//get 'ul' element from the DOM
	let elem = document.
	getElementById('ul'); //get 'ul' element from the DOM
	let viewIds = Object.keys(actionList.viewMap);
	let names = Object.values(actionList.viewMap);
	console.log(viewIds);
	//Delete the existing actionList first
	deleteCurrentActionList();
	for (let key in actionList.viewMap) {	
  		let li = document.createElement('li'); //create 'li' element
  		li.innerHTML = "name: " + actionList.viewMap[key]; //assign your data to each 'li'
		elem.appendChild(li); //append 'li' to the 'ul' element
	}

	let elems = document.getElementsByTagName('li');
	Array.from(elems).forEach((v, i) => v.addEventListener('click', function() {
  		Array.from(elems).forEach((c,k) => {c.style.background = 'transparent'; c.innerHTML = 'name: ' + names[k];});
  		this.innerHTML = 'name: ' + names[i] + ', id: ' + viewIds[i];
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
		webSocket = new WebSocket("ws://localhost:" + WEB_SOCKET_PORT +"/");
		webSocket.onopen = handleSocketOpen;
		webSocket.onmessage = processMessageReceivedFromClient;
		webSocket.onclose = handleSocketClose;
	} else {
 		// The browser doesn't support WebSocket
		alert("WebSocket NOT supported by your Browser!");
	}
}
