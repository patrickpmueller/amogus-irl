import { PlayerID, Role, TaskID } from "./types";

var playerlist: PlayerID[] = [];
var playerID: PlayerID = ""
var playerlistHooks: ((newPlayerlist: PlayerID[]) => void)[] = []; 
var role: Role = "unset";
var tasklist: TaskID[] = [];
var deaths: PlayerID[] = [];

function setRole(newRole: Role) {
  role = newRole;
}

function setPlayerID(newPlayerID: PlayerID) {
  if (newPlayerID === "skip") {
    newPlayerID = "Skip";
  }
  playerID = newPlayerID;
}

function updateTasklist(newTasklist: TaskID[]) {
  tasklist = newTasklist;
}

function updatePlayerlist(newPlayerlist: PlayerID[]) {
  playerlist = newPlayerlist;
  for (let hook of playerlistHooks) {
    hook(newPlayerlist);
  }
}

export { playerID, playerlist, playerlistHooks, updatePlayerlist, role };
export { updateTasklist, setRole, setPlayerID, tasklist, deaths };
