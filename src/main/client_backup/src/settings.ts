import { Settings } from './types.ts';

var settings: Settings = { 
  maxPlayers: 0, 
  tasks: { 
    total: 0, 
    perPlayer: 0
  },
  roles: { 
    crewmates: 0,
    impostors: 0,
    healers: 0
  }
};

function changeSettings(newSettings: Settings) {
  settings = newSettings;
}

export { changeSettings, settings };
