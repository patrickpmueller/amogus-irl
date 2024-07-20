import {useEffect, useState} from "react";
import {socket} from "../connection";
import {deaths, playerID, playerlist} from "../gameEnv";
import {PlayerID} from "../types";
import './Meeting.css';
import {settings} from "../settings";

export default function MeetingComponent() {
  const [meetingProgress, setMeetingProgress] = useState(settings.meeting.duration * 1000);

  let voted = false;

  useEffect(() => {
    let lastRun = Date.now();
    if (meetingProgress > 0) {
      setTimeout(() => {
        const now = Date.now();
        setMeetingProgress(meetingProgress - (now - lastRun));
        console.log(`Now - Last Run: ${now - lastRun}, Meeting Progress: ${meetingProgress}`);
        lastRun = now;
      }, 60);
    } 
  }, [meetingProgress]);

  function vote(ev: React.MouseEvent<HTMLDivElement>) {
    if (voted) {
      alert("Cannot vote twice!");
      return;
    }
    if (meetingProgress <= 0) {
      alert("Meeting time is over!");
      return;
    }
    const votedPlayer: PlayerID = ev.currentTarget.id;
    if (confirm("Are you sure you want to vote for " + votedPlayer)) {
      socket.sendVote(votedPlayer);
      voted = true;
    }
  }

  const voteOptions: React.JSX.Element[] = playerlist.map((player, index) => 
    <div 
      key={index} 
      className={"button voteOption" +
        (deaths.indexOf(player) === -1 ? "" : " dead") + 
        (player === playerID ? " me" : "")} 
      id={player}
      onClick={vote}>
        {player} 
      </div>);

  return (
    <div id={deaths.indexOf(playerID) === -1 ? "meeting-wrapper" : "meeting-wrapper-death"} className="wrapper">
      <div id="title">Who is the Impostor?</div>
        <div className="wrapper" id="playersBox">
          {voteOptions}
          <div className="button voteOption" id="skip" onClick={vote}>
          Skip
        </div>
        </div>
        <progress 
        id="meetingProgress" 
        max={settings.meeting.duration * 1000} 
        value={meetingProgress}>
        </progress>
      </div>
  )
}
