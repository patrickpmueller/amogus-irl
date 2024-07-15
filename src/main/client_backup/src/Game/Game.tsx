import './Game.css';
import {socket} from '../connection';
import {playerID, playerlist, playerlistHooks, tasklist} from '../gameEnv';
import {useEffect, useState} from 'react';

export default function GameComponent() {
  const [playerlistElem, setPlayerlistElem] = 
    useState([<>{"Error while loading playerlist"}</>]);

  function updatePlayerlist() {
    let local: React.JSX.Element[];
    if (!socket.connected) {
      local = [<>{"Not Connected"}</>];
    } else {
      local = [<>{"Players: "}</>]
      setPlayerlistElem(local.concat(playerlist.filter(player => player !== "")
        .map((player, index) => player == playerID ?
          <li key={index} id={player} className='player'><b>{player}</b></li> :
          <li key={index} id={player} className="player">{player}</li>)));
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
