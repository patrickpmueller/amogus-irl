import { playerID, playerlist, playerlistHooks, setPlayerID } from '../gameEnv';
import './Lobby.css';
import {to_game, to_home} from '../main';
import { socket } from '../connection';
import $ from 'jquery';
import {useEffect, useState} from 'react';

export default function LobbyComponent() {
  const [playerlistElem, setPlayerlistElem] = 
    useState([<div key="err">{"Error while loading playerlist"}</div>]);

  function updatePlayerlist() {
    let local: React.JSX.Element[];
    if (!socket.connected) {
      local = [<div key="nc">{"Not Connected"}</div>];
    } else {
      local = [<div key="-1">{"Players: "}</div>]
      local = local.concat(playerlist.filter(player => player !== "")
        .map((player, index) => player == playerID ?
          <li key={index} id={player} className='player'><b>{player}</b></li> :
          <li key={index} id={player} className="player">{player}</li>));
    }    
    setPlayerlistElem(local);
  }
  useEffect(updatePlayerlist, []);
  playerlistHooks.push(updatePlayerlist);

  let gameStarting = false;
  let intervalId: number;

  return (
    <div id="lobby-wrapper" className="wrapper">
      <button id="back" className="secondaryButton" onClick={to_home}>
        Back to Home Screen
      </button>
      <button id="rejoin" className="secondaryButton" onClick={() => {
          socket.sendReconnect();
          to_game();
        }}>
        Rejoin game
      </button>
      <ul id="lobby-playerlist" className="list">
        {playerlistElem}  
      </ul>
      <div className="nameBox" id="playerNameBox">
        <label htmlFor="name">Nickname: </label>
        <input id="name" />
        <button className="button" id="join" onClick={
          () => {
            setPlayerID($("#name").val()?.toString() as string);
            socket.sendSetup();
          }
        }>
          Join!
        </button>
      </div>
      <button id="start" className="button" onClick={(event) => {
        const button = event.currentTarget;
        if (gameStarting) {
          clearInterval(intervalId)
          gameStarting = false;
          button.textContent = "Start Game!"
          return
        }
        gameStarting = true;
        let count = 5;
        button.textContent = count.toString();
        intervalId = setInterval(function() {
          count--;
          if (count > 0) {
            button.textContent = count.toString();
          } else {
            clearInterval(intervalId);
            button.textContent = "Starting Game..."
            socket.sendStartGame();
          }
        }, 1000);
      }}>
        Start Game!
      </button>
    </div>
  );
}
