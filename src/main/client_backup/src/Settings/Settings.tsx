import { to_home } from '../main';
import './Settings.css';
import { settings } from '../settings.ts';
import { socket } from '../connection.ts';
import $ from 'jquery';

function SettingsComponent() {
  function submitForm(e: React.SyntheticEvent) {
    e.preventDefault();
    resetFormValidity();
    let valid = true;

    const maxPlayersElem = $("#maxPlayers").get(0) as HTMLFormElement;
    if (settings.maxPlayers < (settings.roles.impostors + settings.roles.healers + settings.roles.crewmates)) {
      maxPlayersElem.setCustomValidity("Max. player count is less than roles");
      valid = false;
    }
    maxPlayersElem.reportValidity();

    const tppElem = $("#tasks\\.perPlayer").get(0) as HTMLFormElement;
    if (settings.tasks.total < settings.tasks.perPlayer) {
      tppElem.setCustomValidity("Tasks per player cannot be smaller than total tasks");
      valid = false;
    }
    tppElem.reportValidity();

    if (valid) {
      socket.sendSettings();     
    }
  }

  function resetFormValidity() {
    $("#settingsForm").children("input").each((_, e) => e.setCustomValidity(""));
  }

  function updateSettings(e: React.FormEvent<HTMLInputElement>) {
    resetFormValidity();
    const target = e.currentTarget.id;

    switch (target) {
      case "maxPlayers": 
        settings.maxPlayers = parseInt(e.currentTarget.value);
        break;
      case "tasks.perPlayer":
        settings.tasks.perPlayer = parseInt(e.currentTarget.value);
        break;
      case "tasks.total":
        settings.tasks.total = parseInt(e.currentTarget.value);
        break;
      case "roles.crewmates":
        settings.roles.crewmates = parseInt(e.currentTarget.value);
        break;
      case "roles.impostors":
        settings.roles.impostors = parseInt(e.currentTarget.value);
        break;
      case "roles.healers":
        settings.roles.healers = parseInt(e.currentTarget.value);
        break;
    }
  }

  // Return react component
  return (
    <div className="wrapper">
      <button onClick={to_home} className="button" id="homeButton">Back to home</button>
      <form id="settingsForm" onSubmit={submitForm}>

        <label htmlFor="maxPlayers">Maximum Number of Players:</label> 
        <br />
        <input type="number" name="maxPlayers" id="maxPlayers" defaultValue={settings.maxPlayers} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <label htmlFor="tasks.perPlayer">Tasks per Player:</label> 
        <br />
        <input type="number" name="tasks.perPlayer" id="tasks.perPlayer" defaultValue={settings.tasks.perPlayer} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <label htmlFor="tasks.total">Task Count:</label> 
        <br />
        <input type="number" name="tasks.total" id="tasks.total" defaultValue={settings.tasks.total} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <label htmlFor="roles.impostors">Impostor Count:</label> 
        <br />
        <input type="number" name="roles.impostors" id="roles.impostors" defaultValue={settings.roles.impostors} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <label htmlFor="roles.crewmates">Crewmate Count:</label> 
        <br />
        <input type="number" name="roles.crewmates" id="roles.crewmates" defaultValue={settings.roles.crewmates} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <label htmlFor="roles.healers">Healer Count:</label> 
        <br />
        <input type="number" name="roles.healers" id="roles.healers" defaultValue={settings.roles.healers} onChange={(e) => updateSettings(e)} /> 
        <br /> 
        <br />

        <input type="submit" className="button" value="Save Settings" id="submit" />
      </form>
    </div>

  );
}

export default SettingsComponent;
