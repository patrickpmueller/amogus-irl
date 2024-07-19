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

let gameFinishedInterval: number;
let gameFinished = false;
let winners: Role;
export function finishGame(w: Role) {
  winners = w;
  gameFinished = true;
  deaths.forEach((_: PlayerID, i: number) => {
    delete deaths[i];
  });
  setPlayerID("");
  setRole("unset");
  gameFinishedInterval = setInterval(() => to_gameEnd(winners), 1500);
}

function to_lobby() {
  reactRoot.render(
    <React.StrictMode>
      <LobbyComponent />
      </React.StrictMode>
  );
}

function to_settings() {
  reactRoot.render(
    <React.StrictMode>
      <SettingsComponent />
      </React.StrictMode>
  );
}

function to_home() {
  reactRoot.render(
    <React.StrictMode>
      <HomeComponent />
      </React.StrictMode>
  );
}

function to_role() {
  reactRoot.render(
    <React.StrictMode>
      <RoleComponent />
      </React.StrictMode>
  );
}

function to_game() {
  reactRoot.render(
    <React.StrictMode>
      <GameComponent />
      </React.StrictMode>
  );
}

function to_meeting() {
  reactRoot.render(
    <React.StrictMode>
      <MeetingComponent />
      </React.StrictMode>
  );
}

function to_results(winner: PlayerID) {
  reactRoot.render(
    <React.StrictMode>
      <ResultsComponent winner={winner} />
      </React.StrictMode>
  );

  setTimeout(() => {
    if (gameFinished) {
      clearInterval(gameFinishedInterval);
      to_gameEnd(winners);
    } else {
      to_game();
    }
  }, winner === "skip" ? 8000 : (winner.length + 10) * 150 + 1000);
}

function to_gameEnd(winners: Role) {
  reactRoot.render(
    <React.StrictMode>
      <GameEndComponent winner={winners} />
      </React.StrictMode>
  );

  setTimeout(window.location.reload, 7500)
}

to_home()

export { to_lobby, to_settings, to_home, to_role, to_game, to_meeting, to_results, to_gameEnd};
