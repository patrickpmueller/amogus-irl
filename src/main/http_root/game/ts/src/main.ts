const urlParams: URLSearchParams = new URLSearchParams(window.location.search);
const playerID: string = urlParams.get('playerID')!;

let playerlist: string[] = [];
let socket: WebSocket = new WebSocket("ws://localhost:8080")
let role: string;

socket.onopen = (event) => {
	console.log("Connection is open.");
	let setup = [{"action": "setup", "playerID": playerID }];
	socket.send(JSON.stringify(setup));
};

socket.onmessage = (event) => {
	console.log(event.data);
	let msg: any[] = JSON.parse(event.data);

	for (let elem of msg) {
		switch (elem.type) {
			case "playerlist": 
				playerlist = elem.data;
				let playerlist_str: string = "Players:\n";
				for (let player of playerlist) {
					playerlist_str += "<li class='player'>" + player + "</li>";
				}
				$("#playerlist").html(playerlist_str);
				break;
			case "roles":
				for (let player in elem.data) {
					if (player == playerID) {
						role = elem.data[player];
					}
				}
				break;
			case "meeting":
				console.log("Start meeting!");
				break;
		}
	} 
};


function reportBody() {
	let death: string = window.prompt("Enter playerID of dead body")!;
	if (playerlist.includes(death)) {
		socket.send(JSON.stringify([{"action": "meeting", "death": death}]));
	} else {
		window.alert("Player not found");
	}
}

function playerKilled() {
	if (window.confirm("Are you sure that you have been killed?")) {
		socket.send(JSON.stringify([{"action": "kill", "player": playerID}]));
	}
}