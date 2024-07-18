import './Game.css';
import {socket} from '../connection';
import {deaths, playerID, playerlist, playerlistHooks, role, tasklist} from '../gameEnv';
import {useEffect, useState} from 'react';
import {PlayerID} from '../types';

export default function GameComponent() {
  const [playerlistElem, setPlayerlistElem] = 
    useState([<div key="err">{"Error while loading playerlist"}</div>]);

  function updatePlayerlist() {
    let local: React.JSX.Element[];
    if (!socket.connected) {
      local = [<div key="nc">{"Not Connected"}</div>];
    } else {
      local = [<div key="-1">{"Players: "}</div>]
      setPlayerlistElem(local.concat(playerlist.filter(player => player !== "")
        .map((player, index) => player == playerID ?
          <li key={index} 
              id={player} 
              className={"player" + (deaths.indexOf(player) === -1 ? "" : " death")}>
            <b>{player}</b>
          </li> :
          <li key={index} 
              id={player} 
              className={"player" + (deaths.indexOf(player) === -1 ? "" : " death")}>
            {player}
          </li>)));
    }   
  }
  useEffect(updatePlayerlist, []);
  playerlistHooks.push(updatePlayerlist);


  function reportBody() {
    const death: string = window.prompt("Enter playerID of dead body")!;
    if (playerlist.includes(death)) {
      socket.sendMeeting(death);
    } else {
      window.alert("Player not found");
    }
  }

  function playerKilled() {
    if (window.confirm("Are you sure that you want to be marked as dead?")) {
      socket.sendDeath();
      deaths.push(playerID);
      updatePlayerlist();
    }
  }

  function roleSpecificFunction() {
    if (role === "healer") {
      socket.sendHeal(window.prompt("Who is being healed?") as PlayerID);
    }
  }

  function completeTask(ev: React.MouseEvent<HTMLLIElement, MouseEvent>) {
    if (ev.currentTarget.className.length <= 4) {
      ev.currentTarget.className += " complete";
      socket.sendTaskCompleted(ev.currentTarget.id);
    } else {
      ev.currentTarget.className = "task";
      socket.sendTaskUncompleted(ev.currentTarget.id);
    }
  }
  
  let tasklistElem: React.JSX.Element[] = [<>{"Tasks: "}</>];
  if (!socket.connected) {
    tasklistElem = [<>{"Not Connected"}</>];
  } else {
    tasklistElem = tasklistElem.concat(tasklist.map((task, index) => 
      <li className="task" id={task} key={index} onClick={completeTask}>Task: {task}</li>));
  }
  return (
    <div id="game-wrapper" className="wrapper">
      <button id="role-specifics" className="button" onClick={roleSpecificFunction}>
        Role Specific
      </button>
      <ul className="list" id="tasklist">
        {tasklistElem}  
      </ul>
      <ul className="list" id="game-playerlist">
        {playerlistElem} 
      </ul>
      <button id="report" className="button" onClick={reportBody}>
        Report dead body
      </button>
      <button id="death" className="button" onClick={playerKilled}>
        I was killed
      </button>
    </div>
  )
}
