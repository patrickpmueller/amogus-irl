import $ from 'jquery';

const urlParams = new URLSearchParams(window.location.search);
const role = urlParams.get('show');
const playerID = urlParams.get('playerID');
$(() => {
    if (isNull(role)) {
        console.error("No role given");
    } else if (isNull(playerID)) {
        console.error("No playerID given");
    } else {
        $("#role").attr("src", "html-" + role + ".svg");
        $("#role").attr("alt", "Error! couldn't load SVG!\nRole: " + role);
        document.title = "Role: " + role.charAt(0).toUpperCase() + role.slice(1);
        let time = false;
        let forward = setInterval(() => {
            if (time) {
                clearInterval(forward);
                document.location.href = "/game?playerID=" + playerID;
            }
            time = true;
        }, 2000);
    }
});

function isNull(str: null | string) {
    return str == null || str == "null" || str == ""
}
