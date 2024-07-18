import { PlayerID, Role, TaskID } from "./types";

let playerlist: PlayerID[] = [];
let playerID: PlayerID = ""
const playerlistHooks: ((newPlayerlist: PlayerID[]) => void)[] = []; 
let role: Role = "unset";
let tasklist: TaskID[] = [];
const deaths: PlayerID[] = [];

function setRole(newRole: Role) {
  role = newRole;
}

function setPlayerID(newPlayerID: PlayerID) {
  if (newPlayerID === "skip") {
    newPlayerID = "Skip";
  } else if (newPlayerID === "emergency") {
    newPlayerID = "Emergency";
  }
  playerID = newPlayerID;
}

function updateTasklist(newTasklist: TaskID[]) {
  tasklist = newTasklist;
}

function updatePlayerlist(newPlayerlist: PlayerID[]) {
  playerlist = newPlayerlist;
  for (const hook of playerlistHooks) {
    hook(newPlayerlist);
  }
}

export { playerID, playerlist, playerlistHooks, updatePlayerlist, role };
export { updateTasklist, setRole, setPlayerID, tasklist, deaths };
