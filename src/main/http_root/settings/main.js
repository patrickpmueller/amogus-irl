$(() => {
    const socket = new WebSocket("ws://localhost:8080");

    const validateForm = (event) => {
        event.preventDefault(); 

        let valid = true 
        let settings = {
            "maxPlayers": +$("#maxPlayers").val(),
            "tasks": {
                "perPlayer": +$("#tasksPerPlayer").val(),
                "total": +$("#taskCount").val()
            },
            "roles": {
                "impostors": +$("#impostorCount").val(),
                "healers": +$("#healerCount").val(),
                "crewmates": +$("#crewmateCount").val()
            }
        }; 

        let maxPlayersElem = $("#maxPlayers").get(0)
        maxPlayersElem.setCustomValidity("");
        if (settings.maxPlayers < (settings.roles.impostors + settings.roles.healers + settings.roles.crewmates)) {
            maxPlayersElem.setCustomValidity("Max. player count is less than roles");
            valid = false
        } else {
            maxPlayersElem.setCustomValidity(""); // Reset validity if condition is met
        }
        maxPlayersElem.reportValidity()

        let tppElem = $("#tasksPerPlayer").get(0)
        tppElem.setCustomValidity("")
        if (settings.tasks.total < settings.tasks.perPlayer) {
            tppElem.setCustomValidity("Tasks per player cannot be smaller than total tasks")
            valid = false
        } else {
            tppElem.setCustomValidity(""); // Reset validity if condition is met
        }
        tppElem.reportValidity()
        
        if (valid) {
            let packet = [
                {
                    "action": "changeSettings",
                    "settings": settings
                }
            ];
            console.log(JSON.stringify(packet));
            socket.send(JSON.stringify(packet));
        }
    };

    $('#settingsForm').submit(validateForm);
    $('#submit').on("click", (event) => {
        $("#settingsForm").children("input").each((i, e) => e.setCustomValidity(""));
    });
    $('#settingsForm input').on("input", (event) => {
        $("#settingsForm").children("input").each((i, e) => e.setCustomValidity(""));
    });

    socket.onopen = () => {
        console.log("Connection is open.");
        socket.send(JSON.stringify([{"action": "setup", "playerID": "in_settings"}]));
    };

    socket.onmessage = (event) => {
        let msg = JSON.parse(event.data);
        console.log(msg)

        for (let elem of msg) {
            switch (elem.type) {
                case "settings": 
                    let settings = elem.data
                    $("#maxPlayers").val(settings.maxPlayers)
                    $("#tasksPerPlayer").val(settings.tasks.perPlayer)
                    $("#taskCount").val(settings.tasks.total)
                    $("#impostorCount").val(settings.roles.impostors)
                    $("#healerCount").val(settings.roles.healers)
                    $("#crewmateCount").val(settings.roles.crewmates)
            }
        } 
    };



});
