import './Home.css'
import { to_lobby, to_settings } from '../main';

export default function HomeComponent() {
  return (
    <div id="home-wrapper" className="wrapper">
      <button id="lobby" className="button" onClick={to_lobby}>Lobby</button>
      <button id="settings" className="button" onClick={to_settings}>Settings</button>
    </div>
  )
}
