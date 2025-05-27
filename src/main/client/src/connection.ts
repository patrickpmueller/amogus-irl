import { Message, Settings, Role, PlayerRole, TaskID, PlayerID, MessageOut } from './types.ts';
import { changeSettings, settings } from './settings.ts';
import { playerID, updatePlayerlist, setRole, updateTasklist, deaths, playerlist } from './gameEnv.ts';
import { finishGame, to_meeting, to_results, to_role} from './main.tsx';

export default class GameWebSocket {
    private reconnectInterval: number = 3000; // 3 seconds
    private shouldReconnect: boolean = true;
    private websocket: WebSocket | null = null;

    public url: string;
    public connected: boolean = false;

    constructor(url: string) {
        this.url = url;

        this.connect();
    }

    private connect() {
        this.websocket = new WebSocket(this.url);

        this.websocket.onopen = this.handleOpen.bind(this);
        this.websocket.onerror = this.handleError.bind(this);
        this.websocket.onclose = this.handleClose.bind(this);
        this.websocket.onmessage = this.handleMessage.bind(this);
    }

    private handleOpen() {
        console.log("Connection is open");
        this.sendSetup();
        this.connected = true;
    }

    private handleError() {
        this.websocket?.close();
    }

    private handleClose() {
        console.log("Connection is closed");
        this.connected = false;
        if (this.shouldReconnect) {
            setTimeout(() => this.connect(), this.reconnectInterval);
        }
        updatePlayerlist([]);
    }

    private handleMessage(event: MessageEvent) {
        const msgs: Message[] = JSON.parse(event.data);
        console.log(msgs);

        for (const msg of msgs) {
            switch (msg.type) {
                case "settings":
                    changeSettings(msg.data as Settings);
                    break;
                case "playerlist":
                    updatePlayerlist(msg.data as string[]);
                    break;
                case "startGame":
                    for (const newRole of msg.data as PlayerRole[]) {
                        if (newRole.player == playerID) {
                            setRole(newRole.role as Role);
                        }
                    }
                    to_role();
                    break;
                case "tasks":
                    updateTasklist(msg.data as TaskID[]);
                    break;
                case "meeting":
                    deaths.push(msg.data as PlayerID);
                    to_meeting();
                    break;
                case "result":
                    deaths.push(msg.data as PlayerID);
                    to_results(msg.data as PlayerID);
                    break;
                case "endGame":
                    finishGame(msg.data as Role);
                    break;
                case "healed": 
                {
                        const index: number = deaths.indexOf(msg.data as PlayerID, 0);
                        if (index > -1) {
                            deaths.splice(index, 1);
                        }
                        updatePlayerlist(playerlist);
                    }
            }
        }
    }

    public sendSetup() {
        this.send(this.asString({ action: "setup", playerID: playerID }));
    }

    public sendReconnect() {
        this.send(this.asString({action: "reconnect"}));
    }

    public sendStartGame() {
        this.send(this.asString({ action: "startGame" }));
    }

    public sendVote(target: PlayerID) {
        this.send(this.asString({ action: "vote", target: target }));
    }

    public sendSettings() {
        this.send(this.asString({ action: "changeSettings", settings: settings }));
    }

    public sendMeeting(death: PlayerID) {
        this.send(this.asString({ action: "meeting", death: death }));
    }

    public sendDeath() {
        this.send(this.asString({ action: "kill", playerID: playerID }));
    }

    public sendHeal(target: PlayerID) {
        this.send(this.asString({ action: "heal", target: target}));
    }

    public sendTaskCompleted(taskID: TaskID) {
        this.send(this.asString({ action: "taskCompleted", taskID: taskID }));
    }

    public sendTaskUncompleted(taskID: TaskID) {
        this.send(this.asString({ action: "taskUncompleted", taskID: taskID }));
    }

    private asString(raw: MessageOut) {
        return JSON.stringify([raw]);
    }

    public send(data: string) {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            this.websocket.send(data);
        } else {
            console.error('WebSocket is not open. Ready state:', this.websocket?.readyState);
        }
    }

    public close() {
        this.shouldReconnect = false;
        this.websocket?.close();
    }
}

export const socket = new GameWebSocket("ws://" + location.host + ":8080");
