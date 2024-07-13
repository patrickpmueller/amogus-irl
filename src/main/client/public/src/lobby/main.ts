import $ from "jquery";

let playerlist: string[] = [];
let playername: string = "";

let socket = new WebSocket("ws://localhost:8080");

socket.onopen = () => {
    console.log("Connected to server");
    socket.send(JSON.stringify([{"action": "setup", "playerID": ""}]));
};

socket.onmessage = (event) => {
    console.log(event.data);
    let msg: Message[] = JSON.parse(event.data);

    for (let elem of msg) {
        switch (elem.type) {
            case "playerlist": 
                let playerlist_str = "Players:\n";
                for (let player of (elem as PlayerlistMessage).data) {
                    if (player != "") {
                        playerlist_str += "<li class='player'>" + player + "</li>"
                        playerlist.push(player)
                    }
                }
                $("#playerlist").html(playerlist_str);
                break;
            case "startGame":
                let role = (elem as RoleMessage).data;
                document.location.href = "/game/role?show=" + role + 
                    "&playerID=" + playername;
        }
    } 
};

$("#join").on("click", (event) => {
    playername = $("#name").val() as string;
    let setup = [{"action": "setup", "playerID": playername }];
    socket.send(JSON.stringify(setup));
});

$(() => {
    let gameStarting = false;
    let gameStartingInterval: NodeJS.Timeout;
    $("#start").on("click", function() {
        const button = this;
        if (gameStarting) {
            clearInterval(gameStartingInterval)
            gameStarting = false;
            button.textContent = "Start Game!"
            return
        }
        gameStarting = true;
        let count = 5;
        button.textContent = count.toString();
        gameStartingInterval = setInterval(function() {
            count--;
            if (count > 0) {
                button.textContent = count.toString();
            } else {
                clearInterval(gameStartingInterval);
                button.textContent = "Starting Game..."
                socket.send('[{"action":"startGame"}]')
            }
        }, 1000);
    });
});

interface Message {
    type: string;
    data: string[] | Role[];
}

interface PlayerlistMessage extends Message {
    data: string[];
}

interface RoleMessage extends Message {
    data: Role[];
}

interface Role {
    player: string;
    role: string;
}
