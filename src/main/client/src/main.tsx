import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import HomeComponent from './Home/Home';
import SettingsComponent from './Settings/Settings';
import LobbyComponent from './Lobby/Lobby';
import RoleComponent from './Role/Role';
import GameComponent from './Game/Game';
import MeetingComponent from './Meeting/Meeting';
import GameEndComponent from './GameEnd/GameEnd';
import $ from 'jquery';
import ResultsComponent from './Results/Results';
import {PlayerID, Role} from './types';
import {deaths, setPlayerID, setRole} from './gameEnv';



const reactRoot = ReactDOM.createRoot($("#root").get(0)!);
const debug = false;


let gameFinished = false;
let winners: Role;
let currentScreen: GameScreen;
type GameScreen = "home" | "lobby" | "settings" | "role" | "results" | "game" | "meeting"
  | "gameEnd"

export function finishGame(w: Role) {
  winners = w;
  gameFinished = true;
  deaths.forEach((_: PlayerID, i: number) => {
    delete deaths[i];
  });
  setPlayerID("");
  setRole("unset");
  setTimeout(() => {
  if (currentScreen !== "results") {
      to_gameEnd(w);
  }
  });
}

function to_lobby() {
  currentScreen = "lobby";
  reactRoot.render(
    <React.StrictMode>
      <LobbyComponent />
      </React.StrictMode>
  );
}

function to_settings() {
  currentScreen = "settings";
  reactRoot.render(
    <React.StrictMode>
      <SettingsComponent />
      </React.StrictMode>
  );
}

function to_home() {
  currentScreen = "home";
  reactRoot.render(
    <React.StrictMode>
      <HomeComponent />
      </React.StrictMode>
  );
}

function to_role() {
  currentScreen = "role";
  reactRoot.render(
    <React.StrictMode>
      <RoleComponent />
      </React.StrictMode>
  );
}

function to_game() {
  currentScreen = "game";
  reactRoot.render(
    <React.StrictMode>
      <GameComponent />
      </React.StrictMode>
  );
}

function to_meeting() {
  currentScreen = "meeting";
  reactRoot.render(
    <MeetingComponent />
  );
}

function to_results(winner: PlayerID) {
  currentScreen = "results";
  reactRoot.render(
    <React.StrictMode>
      <ResultsComponent winner={winner} />
      </React.StrictMode>
  );

  setTimeout(() => {
    if (gameFinished) {
      to_gameEnd(winners);
    } else {
      to_game();
    }
  }, winner === "skip" ? 10000 : (winner.length + 10) * 150 + 1000);
}

function to_gameEnd(winners: Role) {
  currentScreen = "gameEnd";
  reactRoot.render(
    <React.StrictMode>
      <GameEndComponent winner={winners} />
      </React.StrictMode>
  );

  setTimeout(() => window.location.reload(), 5000)
}

if (debug) {
  reactRoot.render(
    <React.StrictMode>
      <ResultsComponent winner={"Player1234123"} />
      </React.StrictMode>
  );
} else {
  to_home()
}


export { to_lobby, to_settings, to_home, to_role, to_game, to_meeting, to_results, to_gameEnd};
