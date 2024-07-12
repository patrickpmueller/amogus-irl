$(() => {
    var playerlist = [];
    var playername = "";

    socket = new WebSocket("ws://localhost:8080");

    socket.onopen = function(event) {
        console.log("Connection is open.");
        let setup = [{"action": "setup", "playerID": "" }];
        socket.send(JSON.stringify(setup));
    };

    socket.onmessage = function(event) {
        console.log(event.data);
        let msg = JSON.parse(event.data);

        for (let elem of msg) {
            switch (elem.type) {
                case "playerlist": 
                    let playerlist_str = "Players:\n"
                    for (let player of elem.data) {
                        if (player != "") {
                            playerlist_str += "<li class='player'>" + player + "</li>"
                            playerlist.push(player)
                        }
                    }
                    document.getElementById("playerlist").innerHTML = playerlist_str
                    break;
                case "startGame":
                    for (let player of playerlist) {
                        if (player == playername) {
                            let role = elem.roles[player];
                            document.location.href = "/game/role?show=" + role + 
                                "&playerID=" + playername;
                        } 
                    }
            }
        } 
    };
    
    $("#join").on("click", (event) => {
        playername = $("#name").get(0).value;
        let setup = [{"action": "setup", "playerID": playername }];
        socket.send(JSON.stringify(setup));
    });


    var gameStarting = false;
    var gameStartingInterval
    document.getElementById("start").addEventListener("click", function() {
        const button = this;
        if (gameStarting) {
            clearInterval(gameStartingInterval)
            gameStarting = false;
            button.textContent = "Start Game!"
            return
        }
        gameStarting = true;
        let count = 5;
        button.textContent = count;
        gameStartingInterval = setInterval(function() {
            count--;
            if (count > 0) {
                button.textContent = count;
            } else {
                clearInterval(gameStartingInterval);
                button.textContent = "Starting Game..."
                socket.send('[{"action":"startGame"}]')
            }
        }, 1000);
    });
});

