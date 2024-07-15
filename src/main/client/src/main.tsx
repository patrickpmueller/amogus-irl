import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import HomeComponent from './Home/Home.tsx';
import SettingsComponent from './Settings/Settings.tsx';
import LobbyComponent from './Lobby/Lobby.tsx';
import RoleComponent from './Role/Role.tsx';
import GameComponent from './Game/Game.tsx';
import MeetingComponent from './Meeting/Meeting.tsx';
import $ from 'jquery';

const reactRoot = ReactDOM.createRoot($("#root").get(0)!);

function to_lobby() {
  reactRoot.render(
    <React.StrictMode>
      <LobbyComponent />
    </React.StrictMode>
  );
};

function to_settings() {
  reactRoot.render(
    <React.StrictMode>
      <SettingsComponent />
    </React.StrictMode>
  );
};

function to_home() {
  reactRoot.render(
    <React.StrictMode>
      <HomeComponent />
    </React.StrictMode>
  );
};

function to_role() {
  reactRoot.render(
    <React.StrictMode>
      <RoleComponent />
    </React.StrictMode>
  );
};

function to_game() {
  reactRoot.render(
    <React.StrictMode>
      <GameComponent />
    </React.StrictMode>
  );
};

function to_meeting() {
  reactRoot.render(
    <React.StrictMode>
      <MeetingComponent />
    </React.StrictMode>
  );
};

to_home()

export { to_lobby, to_settings, to_home, to_role, to_game, to_meeting };
