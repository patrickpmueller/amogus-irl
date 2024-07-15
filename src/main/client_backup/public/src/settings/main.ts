const settingsForm = $('#settingsForm').get(0) as HTMLFormElement;

const socket = new WebSocket("ws://localhost:8080");
socket.onopen = () => {
    console.log("Connection is open.");
    socket.send(JSON.stringify([{"action": "setup", "playerID": "in_settings"}]));
};
socket.onmessage = (event) => {
    let msg: Message[] = JSON.parse(event.data);
    console.log(msg);

    for (let elem of msg) {
        switch (elem.type) {
            case "settings": 
                let settings: Settings = elem.data;
                $("#maxPlayers").val(settings.maxPlayers);
                $("#tasksPerPlayer").val(settings.tasks.perPlayer);
                $("#taskCount").val(settings.tasks.total);
                $("#impostorCount").val(settings.roles.impostors);
                $("#healerCount").val(settings.roles.healers);
                $("#crewmateCount").val(settings.roles.crewmates);
                break;
        }
    } 
};


settingsForm.onsubmit = (event) => {
    event.preventDefault(); 

    let valid = true 
    let settings: Settings  = {
        maxPlayers: $("#maxPlayers").val() as number,
        tasks: {
            perPlayer: $("#tasksPerPlayer").val() as number,
            total: $("#taskCount").val() as number
        },
        roles: {
            impostors: $("#impostorCount").val() as number,
            healers: $("#healerCount").val() as number,
            crewmates: $("#crewmateCount").val() as number
        }
    }; 

    let maxPlayersElem = $("#maxPlayers").get(0) as HTMLFormElement;
    maxPlayersElem.setCustomValidity("");

    if (settings.maxPlayers < (settings.roles.impostors + settings.roles.healers + settings.roles.crewmates)) {
        maxPlayersElem.setCustomValidity("Max. player count is less than roles");
        valid = false;
    } else {
        maxPlayersElem.setCustomValidity(""); // Reset validity if condition is met
    }
    maxPlayersElem.reportValidity();

    let tppElem = $("#tasksPerPlayer").get(0) as HTMLFormElement;
    tppElem.setCustomValidity("");
    if (settings.tasks.total < settings.tasks.perPlayer) {
        tppElem.setCustomValidity("Tasks per player cannot be smaller than total tasks");
        valid = false;
    } else {
        tppElem.setCustomValidity(""); // Reset validity if condition is met
    }
    tppElem.reportValidity();

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

$('#submit').on("click", (event) => {
    $("#settingsForm").children("input").each((i, e) => e.setCustomValidity(""));
});
$('#settingsForm input').on("input", (event) => {
    $("#settingsForm").children("input").each((i, e) => e.setCustomValidity(""));
});


interface Message {
    type: string;
    data: Settings;
}

interface Settings {
    maxPlayers: number;
    tasks: Tasks;
    roles: Roles;
}

interface Tasks {
    perPlayer: number;
    total: number;
}

interface Roles {
    crewmates: number;
    impostors: number;
    healers: number;
}
