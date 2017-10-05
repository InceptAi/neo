const WEB_SOCKET_PORT = 7070;
const SOURCE_RELAY = "RELAY";
const LIST_ACTION = "LIST";
const CONNECT_ACTION = "CONNECT";
const SOURCE_EXPERT = "EXPERT";
const EXPERT_SPECIAL_ACTION_HOME = "home";
const EXPERT_SPECIAL_ACTION_END_SESSION = "end";
const EXPERT_SPECIAL_ACTION_GO_BACK = "back";
const EXPERT_SPECIAL_ACTION_SHOW_SETTINGS = "settings";
const EXPERT_SPECIAL_ACTION_REFRESH = "refresh";
const EXPERT_SPECIAL_ACTION_SCROLL_UP = "scrollup";
const EXPERT_SPECIAL_ACTION_SCROLL_DOWN = "scrolldown";
const SERVER_ADDRESS = "dobby1743.duckdns.org";

let webSocket;
var lastSelectedUUID;

function handleSocketOpen() {
	alert("Socket is open");
}

function handleSocketClose() {
	alert("Socket is closed");
}

function processMessageReceivedFromClient(eventInfo) {
	var mesgRecv = eventInfo.data;
	console.log("received msg = " + mesgRecv);
	//convert mesg to json
	var actionList = undefined;
	if (mesgRecv !== undefined) {
		try {
			actionList = JSON.parse(mesgRecv);
		} catch (e) {
			console.log('Error parsing json:' + e);
			return;
		}
		if (actionList !== undefined) {
			updateView(actionList);
			//updateActionList(actionList);
			if (actionList.viewMap !== undefined) {
				console.log(Object.values(actionList.viewMap));
			}
		}
	}
	//console.log(Object.values(actionList.viewMap));
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

function deleteList(listName) {
  	let currentList = document.getElementById(listName); //create 'li' element
  	if (currentList != null) {
		while (currentList.firstChild) {
    		currentList.removeChild(currentList.firstChild);
		}
  	}
}

function deleteCurrentActionList() {
  	let currentList = document.getElementById('settingsList'); //create 'li' element
  	if (currentList != null) {
		while (currentList.firstChild) {
    		currentList.removeChild(currentList.firstChild);
		}
  	}
}

function deleteCurrentUUIDList() {
  	let currentList = document.getElementById('uuidList'); //create 'li' element
  	if (currentList != null) {
		while (currentList.firstChild) {
    		currentList.removeChild(currentList.firstChild);
		}
  	}
}

function getInnerHtmlItem(name) {
	return `<a href="#">` + name + `</a>`;
}

function drawRectangleWithText(context, x, y, width, height, text) {
	context.lineWidth = "1";
	context.beginPath();
	context.rect(x, y, width, height);
	context.stroke();
	context.fillText(text, x, y, width);
}

function findViewIdOfClickedView(viewList, xClick, yClick) {
	for (let key in viewList.viewMap) {
		let leftX = viewList.viewMap[key].leftX;
		let rightX = viewList.viewMap[key].rightX;
		let bottomY = viewList.viewMap[key].bottomY;
		let topY = viewList.viewMap[key].topY;
		if (leftX == undefined || rightX == undefined || 
			bottomY == undefined || topY == undefined) {
			console.log("undefined coordinates, bailing");
			continue;		
		}
		if (yClick > topY && yClick < bottomY && xClick > leftX && xClick < rightX) {
            //alert('clicked an element');
			return key;
        }
	}
	return "";
}

function handleMouseClickOnCanvas(event, viewList) {
	let xMouseCoordinate = event.pageX - canvasLeft;
	let yMouseCoordinate = event.pageY - canvasTop;
	console.log(xMouseCoordinate, yMouseCoordinate);
	//find view Id of clicked event
	let clickedViewId = findViewIdOfClickedView(viewList, xMouseCoordinate, yMouseCoordinate);
	if (clickedViewId !== "") {
		//alert("sending message to client");
		console.log("Sending message to client for viewId", clickedViewId);
		sendMessageToClient(clickedViewId, viewList.viewMap[clickedViewId].finalText);
	}
}

function updateView(viewList) {
	//JSON object here -- display a list
	//get 'ul' element from the DOM
	let remoteViewCanvas = document.getElementById("remoteView");
	let canvasLeft = remoteViewCanvas.offsetLeft;
    let canvasTop = remoteViewCanvas.offsetTop;
	let ctx = remoteViewCanvas.getContext("2d");
	console.log(viewList.viewMap);


	//Delete the existing view
	ctx.clearRect(0, 0, remoteViewCanvas.width, remoteViewCanvas.height);

	// Add event listener for `click` events.
	remoteViewCanvas.removeEventListener('click', handleMouseClickOnCanvas);
	remoteViewCanvas.addEventListener('click', handleMouseClickOnCanvas);
	remoteViewCanvas.addEventListener('click', function handleMouseClickOnCanvas(event) {
    	let xMouseCoordinate = event.pageX - canvasLeft;
		let yMouseCoordinate = event.pageY - canvasTop;
    	console.log(xMouseCoordinate, yMouseCoordinate);
	    //find view Id of clicked event
		let clickedViewId = findViewIdOfClickedView(viewList, xMouseCoordinate, yMouseCoordinate);
		if (clickedViewId !== "") {
			//alert("sending message to client");
			console.log("Sending message to client for viewId", clickedViewId);
			sendMessageToClient(clickedViewId, viewList.viewMap[clickedViewId].finalText);
		}
	});

	//Iterate over all the views and draw rect with text
	for (let key in viewList.viewMap) {
		let leftX = viewList.viewMap[key].leftX;
		let rightX = viewList.viewMap[key].rightX;
		let bottomY = viewList.viewMap[key].bottomY;
		let topY = viewList.viewMap[key].topY;
		if (leftX == undefined || rightX == undefined || 
			bottomY == undefined || topY == undefined) {
			console.log("undefined coordinates, bailing");
			continue;		
		}
		let text = "NA";
		if (viewList.viewMap[key].text !== "null") {
			text = viewList.viewMap[key].text;
		} else if (viewList.viewMap[key].contentDescription !== "null") {
			text = viewList.viewMap[key].contentDescription;
		}
		viewList.viewMap[key].finalText = text;
		drawRectangleWithText(ctx, leftX, topY, rightX - leftX, bottomY - topY, text);
	}
}




function updateActionList(actionList) {
	//JSON object here -- display a list
	//get 'ul' element from the DOM
	let elem = document.getElementById('settingsList'); //get 'ul' element from the DOM
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
		li.neoName = name;
		li.neoViewId = viewMapTemp.get(name);
		li.addEventListener('click', function() {
			sendMessageToClient(this.neoViewId, this.neoName);
		});
		elem.appendChild(li); //append 'li' to the 'ul' element
	}
}

function registerEventListenerForCanvas() {

}

function initializeWebSocket() {
	if ("WebSocket" in window) {
		alert("WebSocket is supported by your Browser!, initializing webSocket");
		// Let us open a web socket
		if (webSocket != undefined) {
			return;
		}
		webSocket = new WebSocket("ws://" + SERVER_ADDRESS + ":" + WEB_SOCKET_PORT +"/");
		webSocket.onopen = handleSocketOpen;
		webSocket.onmessage = processMessage;
		webSocket.onclose = handleSocketClose;
	} else {
 		// The browser doesn't support WebSocket
		alert("WebSocket NOT supported by your Browser!");
	}
}

function processMessage(eventInfo) {
    console.log('message received: ' + eventInfo.data);
    var parsedMessage = undefined;
    try {
       parsedMessage = JSON.parse(eventInfo.data);
    } catch (e) {
       console.log('Error parsing json:' + e);
       return;
    }
    if (parsedMessage.source == SOURCE_RELAY) {
       if (parsedMessage.uuidList !== undefined) {
           populateUuidList(parsedMessage.uuidList);
       }		 
    } else {
		processMessageReceivedFromClient(eventInfo);
	}
}

function populateUuidList(uuidList) {
	deleteCurrentUUIDList();
    var elem = document.getElementById('uuidList'); //get 'ul' element from the DOM
    var lastUUIDgone = true;
    uuidList.forEach(function (item) {
      console.log("list item: " + item);
      var li = document.createElement('li');
      li.innerHTML = getInnerHtmlItem(item);
	  li.id = item
      li.uuid = item;
	  if (lastSelectedUUID !== undefined && li.id == lastSelectedUUID) {
		li.style.backgroundColor = 'lightgreen';
        lastUUIDgone = false;
	  }
      elem.appendChild(li);
      li.addEventListener('click', function() {
		console.log('onClick'+ this.uuid);
		connectToUuid(this.uuid);
		sendRefreshCommand();
		if (lastSelectedUUID !== undefined) {
			var lastSelectedItem = document.getElementById(lastSelectedUUID)
			if (lastSelectedItem !== undefined) {
				lastSelectedItem.style.backgroundColor = 'transparent';
			}
		}
		this.style.backgroundColor = 'lightgreen';
		lastSelectedUUID = this.uuid;
      });
   });

   if (lastUUIDgone === true) {
       lastSelectedUUID = undefined;
	   deleteCurrentActionList();
   }
}

function endExpertSession() {
	sendCommand(EXPERT_SPECIAL_ACTION_END_SESSION);
}

function sendBackButtonCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_GO_BACK);
}

function sendHomeButtonCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_HOME);
}

function sendShowSettingsButtonCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_SHOW_SETTINGS);
}

function sendRefreshCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_REFRESH);
}

function sendScrollUpCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_SCROLL_UP);
}


function sendScrollDownCommand() {
	sendCommand(EXPERT_SPECIAL_ACTION_SCROLL_DOWN);
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


//Fetch uuid stuff

function fetchUuidList() {
   sendMessageToRelay(webSocket, { serverAction: LIST_ACTION});
}

function connectToUuid(uuid) {
   sendMessageToRelay(webSocket, {serverAction: CONNECT_ACTION, uuid: uuid});
}

function sendMessageToRelay(webSocket, message) {
   message.source = SOURCE_EXPERT;
   if (webSocket === undefined || webSocket.readyState !== WebSocket.OPEN) {
      console.log('Error in sending message to relay server.');
   } else {
      webSocket.send(JSON.stringify(message));
   }
}




































