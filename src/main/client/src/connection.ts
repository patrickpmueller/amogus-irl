import { Message, Settings, Role, PlayerRole, TaskID, PlayerID, MessageOut } from './types.ts';
import { changeSettings, settings } from './settings.ts';
import { playerID, updatePlayerlist, setRole, updateTasklist, deaths } from './gameEnv.ts';
import {to_meeting, to_role} from './main.tsx';

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

    private handleOpen(_: Event) {
        console.log("Connection is open");
        this.sendSetup();
        this.connected = true;
    }

    private handleError(_: Event) {
        this.websocket?.close();
    }

    private handleClose(_: CloseEvent) {
        console.log("Connection is closed");
        this.connected = false;
        if (this.shouldReconnect) {
            setTimeout(() => this.connect(), this.reconnectInterval);
        }
    }

    private handleMessage(event: MessageEvent) {
        let msgs: Message[] = JSON.parse(event.data);
        console.log(msgs);

        for (let msg of msgs) {
            switch (msg.type) {
                case "settings":
                    let newSettings = msg.data as Settings;
                    changeSettings(newSettings);
                    break;
                case "playerlist":
                    let newPlayerlist = msg.data as string[];
                    updatePlayerlist(newPlayerlist);
                    break;
                case "startGame":
                    let roles = msg.data as PlayerRole[];
                    for (let newRole of roles) {
                        if (newRole.player == playerID) {
                            setRole(newRole.role as Role);
                        }
                    }
                    to_role();
                    break;
                case "tasks":
                    let newTasks = msg.data as TaskID[];
                    updateTasklist(newTasks);
                    break;
                case "meeting":
                    let death = msg.data as PlayerID;
                    deaths.push(death);
                    to_meeting();
                    break;
            }
        }
    }

    public sendSetup() {
        this.send(this.asString({ action: "setup", playerID: playerID }));
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

export const socket = new GameWebSocket("ws://localhost:8080");
