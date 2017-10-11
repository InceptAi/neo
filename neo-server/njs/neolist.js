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

//Different class names
const ROLE_SWITCH = "android.widget.Switch";
const ROLE_TOGGLE = "android.widget.ToggleButton";
const ROLE_TEXT_VIEW = "android.widget.TextView";
const ROLE_CHECKED_TEXT_VIEW = "android.widget.CheckedTextView";
const ROLE_CHECK_BOX = "android.widget.CheckBox";
const ROLE_SEEK_BAR = "android.widget.SeekBar";
const ROLE_EDIT_TEXT = "android.widget.EditText";

//Different text types
const ON_TEXT = "ON";
const OFF_TEXT = "OFF";

//Colors
const COLOR_BUTTON_ON = "#66c2ff"; 
const COLOR_BUTTON_OFF = "#b6afaf";
const COLOR_BLACK = "#000000";

//static dimensions
const HEIGHT_CHECK_BOX = 30;
const WIDTH_CHECK_BOX = 30;
 
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

function drawCheckBox(context, x, y, isChecked) {
    //draw outer boxi
	context.save();
    context.lineWidth = "2";
	context.strokeRect(x, y, WIDTH_CHECK_BOX, HEIGHT_CHECK_BOX);
 
    //draw check or x
    if (isChecked) {
		context.font="40px Arial";
        context.fillStyle = COLOR_BUTTON_ON;
        context.fillText("\u2713", x, y + (HEIGHT_CHECK_BOX * 0.8));
        context.fillStyle = COLOR_BLACK;
    }
	context.restore(); 
}



function drawWithText(context, x, y, width, height, text, className, shouldDrawBoundary, isCheckable, isChecked) {
	if (x < 0 || y < 0 || width < 0 || height < 0) {
		return;
	}

	if (shouldDrawBoundary !== undefined && shouldDrawBoundary) {
		context.lineWidth = "1";
		context.beginPath();
		context.rect(x, y, width, height);
		context.stroke();
	}

	if (className === ROLE_SWITCH || className === ROLE_TOGGLE) {
		if (text.toLowerCase() === ON_TEXT.toLowerCase()) {
			context.fillStyle = COLOR_BUTTON_ON;
		} else {
			context.fillStyle = COLOR_BUTTON_OFF;
		}
		context.fillRect(x, y, width, height);
		context.fillStyle = COLOR_BLACK;	
	} else if (className === ROLE_CHECKED_TEXT_VIEW) {
		let yOffset = (height - HEIGHT_CHECK_BOX)/2;
		if (yOffset < 0) {
			yOffset = 0;
		}
		drawCheckBox(context, x + width - WIDTH_CHECK_BOX, y + yOffset, isChecked);
	} else if (className === ROLE_CHECK_BOX) {
		drawCheckBox(context, x, y, isChecked);
	}


	if (text !== "") {
		//context.font="30px Georgia";
		//context.fillText(text, x + 50, y + 50, width);
		context.fillText(text, x + 10, y + (height/2), width);
	}
	
}

function findViewIdOfClickedView(viewList, xClick, yClick) {
	for (let key in viewList.viewMap) {
		let leftX = viewList.viewMap[key].leftX;
		let rightX = viewList.viewMap[key].rightX;
		let bottomY = viewList.viewMap[key].bottomY;
		let topY = viewList.viewMap[key].topY;
		if (viewList.viewMap[key].isParentOfClickableView !== undefined &&
			viewList.viewMap[key].isParentOfClickableView == true) {
			//Don't count clicks on stuff which is just a parent of clickable stuff
			continue;
		}
		if (leftX == undefined || rightX == undefined || 
			bottomY == undefined || topY == undefined) {
			console.log("undefined coordinates, bailing");
			continue;		
		}
		if (yClick > topY && yClick < bottomY && xClick > leftX && xClick < rightX) {
			return key;
        }
	}
	return "";
}

function recreateNode(el, withChildren) {
  if (withChildren) {
    el.parentNode.replaceChild(el.cloneNode(true), el);
  }
  else {
    var newEl = el.cloneNode(false);
    while (el.hasChildNodes()) newEl.appendChild(el.firstChild);
    el.parentNode.replaceChild(newEl, el);
  }
}

function getCursorPosition(canvas, event) {
    var rect = canvas.getBoundingClientRect();
    var x = event.clientX - rect.left;
    var y = event.clientY - rect.top;
    console.log("x: " + x + " y: " + y);
}

function handleMouseClickOnCanvas(evt) {
	//let xMouseCoordinate = evt.pageX - evt.target.canvasLeft;
	//let yMouseCoordinate = evt.pageY - evt.target.canvasTop;
	let rect = evt.target.getBoundingClientRect();
	let xMouseCoordinate = evt.clientX - rect.left;
	let yMouseCoordinate = evt.clientY - rect.top;
	console.log(xMouseCoordinate, yMouseCoordinate, evt.clientX, evt.clientY, rect.left, rect.top);
	//alert(xMouseCoordinate + " , " + yMouseCoordinate + " -- " +  evt.clientX + " , " + evt.clientY + " -- " + rect.left + " , "  + rect.top);
	//find view Id of clicked event
	let clickedViewId = findViewIdOfClickedView(evt.target.viewList, xMouseCoordinate, yMouseCoordinate);
	if (clickedViewId !== "") {
		//alert("sending message to client");
		console.log("Sending message to client for viewId", clickedViewId);
		sendMessageToClient(clickedViewId, evt.target.viewList.viewMap[clickedViewId].finalText);
		}
}

function updateView(viewList) {
	//JSON object here -- display a list
	//get 'ul' element from the DOM
	let remoteViewCanvas = document.getElementById("remoteView");
	// Remove event listener for `click` events.
	//recreateNode(remoteViewCanvas);

	let canvasLeft = remoteViewCanvas.offsetLeft;
    let canvasTop = remoteViewCanvas.offsetTop;
	let ctx = remoteViewCanvas.getContext("2d");
	console.log(viewList.viewMap);

	//Delete the existing view
	ctx.clearRect(0, 0, remoteViewCanvas.width, remoteViewCanvas.height);
	ctx.canvas.width = viewList.rootWidth;
	ctx.canvas.height = viewList.rootHeight;
	//https://stackoverflow.com/questions/1664785/resize-html5-canvas-to-fit-window

	// Add event listener for `click` events.
	remoteViewCanvas.removeEventListener('click', handleMouseClickOnCanvas);
	remoteViewCanvas.addEventListener('click', handleMouseClickOnCanvas);
	remoteViewCanvas.viewList = viewList;
	remoteViewCanvas.canvasLeft = canvasLeft;
	remoteViewCanvas.canvasTop = canvasTop;

	//Iterate over all the views and draw rect with text
	for (let key in viewList.viewMap) {
		processView(ctx, viewList.viewMap[key]);
	}
}


function processView(ctx, viewInfo) {
	let leftX = viewInfo.leftX;
	let rightX = viewInfo.rightX;
	let bottomY = viewInfo.bottomY;
	let topY = viewInfo.topY;

	if (leftX == undefined || rightX == undefined || 
		bottomY == undefined || topY == undefined) {
		console.log("undefined coordinates, bailing");
		return;		
	}

	let text = "";
	if (viewInfo.text !== "null") {
		text = viewInfo.text;
	} else if (viewInfo.contentDescription !== "null") {
		text = viewInfo.contentDescription;
	}
	
	//let shouldDrawBoundary = viewInfo.isParentOfClickableView === undefined ? false : viewInfo.isParentOfClickableView;
    let shouldDrawBoundary = true;	
	drawWithText(ctx, leftX, topY, rightX - leftX, bottomY - topY, text, viewInfo.className, shouldDrawBoundary, viewInfo.isCheckable, viewInfo.isChecked);
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




































