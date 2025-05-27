import {playerID, role} from "../gameEnv";
import $ from 'jquery';
import './Role.css';
import {useEffect} from "react";
import { to_game } from "../main";

export default function RoleComponent() {

  useEffect(() => {
    if (role == "unset") {
      console.error("No role set");
    } else if (playerID == "") {
      console.error("No playerID set");
    } else {
      $("#role").attr("src", "/html-" + role + ".svg");
      $("#role").attr("alt", "Error! couldn't load SVG! Role: " + role);
      document.title = "Role: " + role.charAt(0).toUpperCase() + role.slice(1);
      let time = false;
      const forward = setInterval(() => {
        if (time) {
          clearInterval(forward);
          to_game();
        }
        time = true;
      }, 2000);
    }
  });

  return (
    <div className="wrapper">
      <img
        id="role"
        src="/html-amogus.svg"
        alt="Error! Couldn't load SVG!"
        height="100%"
        width="100%"
      />
    </div>
  );
}
