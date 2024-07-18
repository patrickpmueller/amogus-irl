import {useEffect, useState} from "react";
import {socket} from "../connection";
import {deaths, playerID, playerlist} from "../gameEnv";
import {PlayerID} from "../types";
import './Meeting.css';
import {settings} from "../settings";

export default function MeetingComponent() {
  const [meetingProgress, setMeetingProgress] = useState(settings.meeting.duration);

  let voted = false;

  useEffect(() => {
    if (meetingProgress > 0) {
      console.log(meetingProgress);
      setTimeout(() => {setMeetingProgress(meetingProgress - .066)}, 66);
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
    <div className="wrapper" id="meetingWrapper">
      <div id="title">Who is the Impostor?</div>
        <div className="wrapper" id="playersBox">
          {voteOptions}
          <div className="button voteOption" id="skip" onClick={vote}>
          Skip
        </div>
        </div>
        <progress 
        id="meetingProgress" 
        max={settings.meeting.duration} 
        value={meetingProgress}>
        </progress>
      </div>
  )
}
