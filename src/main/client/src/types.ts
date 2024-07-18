export interface Message {
  type: MessageType;
  data: Settings | PlayerID[] |  PlayerRole[] | TaskID[] | PlayerID;
}

export type PlayerID = string;
export type MessageType = "settings" | "playerlist" | "startGame" | "tasks" | "meeting" 
  | "result" | "endGame" | "healed";
export type Role = "impostor" | "crewmate" | "healer" | "unset";
export type TaskID = string;
export type Action = "vote" | "setup" | "taskCompleted" | "taskUncompleted" | "meeting" 
    | "kill" | "startGame" | "changeSettings" | "heal" | "reconnect";

export interface MessageOut {
  action: Action;
  playerID?: PlayerID;
  settings?: Settings;
  death?: PlayerID;
  taskID?: TaskID;
  target?: PlayerID;
}

export interface PlayerRole {
  player: PlayerID;
  role: string;
}

export interface Settings {
  maxPlayers: number;
  tasks: Tasks;
  roles: Roles;
  meeting: Meeting;
}

export interface Tasks {
  perPlayer: number;
  total: number;
}

export interface Roles {
  crewmates: number;
  impostors: number;
  healers: number;
}

export interface Meeting {
  duration: number;
}
