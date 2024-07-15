import {socket} from "../connection";
import {deaths, playerlist} from "../gameEnv";
import {PlayerID} from "../types";
import './Meeting.css';

export default function MeetingComponent() {
  let voted = false;

  function vote(ev: React.MouseEvent<HTMLDivElement>) {
    if (voted) {
      alert("Cannot vote twice!");
    }
    const votedPlayer: PlayerID = ev.currentTarget.id;
    if (confirm("Are you sure you want to vote for " + votedPlayer)) {
      socket.sendVote(votedPlayer);
      voted = true;
    }
  }

  let voteOptions: React.JSX.Element[];
  voteOptions = playerlist.map((player, index) => 
    <div 
      key={index} 
      className={"button voteOption" + (deaths.indexOf(player) === -1 ? " dead" : "")} 
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
    </div>
  )
}
