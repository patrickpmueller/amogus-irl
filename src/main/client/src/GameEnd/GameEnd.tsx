import {to_home} from '../main';
import {Role} from '../types';
import './GameEnd.css';

export default function GameEndComponent(props: GameEndComponentProps) {
  const winner = props.winner;

  setTimeout(() => {to_home}, 7500);

  return (
    <img 
      id="gameEndScreen"
      src={"html-game-end-" + winner + ".svg"}
      alt={winner}
      width="100%"
      height="100%" />
  );
}

export interface GameEndComponentProps {
  winner: Role; 
}