import './Results.css';
import { PlayerID } from '../types';
import $ from 'jquery';
import {useEffect} from 'react';

interface ResultsProps {
  winner: PlayerID;
}


export default function ResultsComponent({winner}: ResultsProps) {
  winner = winner.substring(0, 16)

  useEffect(() =>  {
    if (winner === "skip") {
      const elem1 = $("#skipped-tag1").get(0) as HTMLSpanElement;
      const elem2 = $("#skipped-tag2").get(0) as HTMLSpanElement;
      new TextType(elem1, "No player was ejected.", 3300, 50);
      setTimeout(() => new TextType(elem2, "Meeting was skipped.", 3000, 50), 4300)
    } else {
      const elem = $("#ejected-tag").get(0) as HTMLSpanElement;
      new TextType(elem, "Ejected: " + winner.trim());
    }
  });

  return (
    <div className="wrapper" id="results-wrapper">
      {winner === "skip" ?
        <div id="skip-div">
          <span className="wrap" id="skipped-tag1"></span>
          <span className="wrap" id="skipped-tag2"></span>
        </div> :
        <>
          <img
            id="ejected"
            src="/html-ejected.svg"
            alt="Error! Couldn't load SVG!"
            width="calc(100vw - 2rem)"
            height="calc(100vh - 3rem - 6em)"/>
          <span className="wrap" id="ejected-tag"></span>
        </>
      }
    </div>
  );
}


class TextType {
  private el: HTMLElement;
  private currentText: string;
  private fullText: string;
  private totalTime: number;
  private jitter: number;

  public constructor(el: HTMLElement, fullText: string, totalTime?: number, jitter?: number) {
    this.el = el;
    this.currentText = '';
    this.fullText = fullText;
    totalTime = totalTime || fullText.length * 150;
    jitter = jitter || totalTime / this.fullText.length / 3;
    this.jitter = Math.min(Math.max(totalTime / fullText.length - 12, 0), jitter);
    this.totalTime = totalTime - this.jitter / 2 * fullText.length;
    this.tick();
  }

  private tick() {
    this.currentText = this.fullText.substring(0, this.currentText.length + 1);

    this.el.innerHTML = this.currentText;

    const delta = this.totalTime / this.fullText.length + this.jitter * Math.random();

    if (this.currentText === this.fullText) {
      return;
    }

    setTimeout(() => {
      this.tick();
    }, delta);

  }
}
