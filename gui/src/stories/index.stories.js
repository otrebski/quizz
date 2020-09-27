import React from 'react';

import { storiesOf } from '@storybook/react';
import { MemoryRouter } from "react-router-dom"; // our router
import { linkTo } from '@storybook/addon-links';
import Step from '../components/Step'
import HistoryStep from '../components/HistoryStep'
import Quizz from '../components/Quizz'

import Welcome from '@storybook/react/demo';
import Feedback from '../components/Feedback';
import 'bootstrap/dist/css/bootstrap.css';

const answers = [
  { id: "id1", text: "Yes" },
  { id: "id2", text: "No" },
  { id: "id3", text: "Maybe" }
]
const historyAnswers = [
  { id: "id1", text: "Yes" },
  { id: "id2", text: "No", selected: true },
  { id: "id3", text: "Maybe" }
]

const quizzState = JSON.parse(`{
    "path": "electricity;checkLocal;buildingFuses",
    "currentStep": {
      "id": "buildingFuses",
      "question": "Are fuses outside your apartment ok?",
      "answers": [
        {
          "id": "noPower",
          "text": "yes",
          "selected": null
        },
        {
          "id": "buildingFusesBroken",
          "text": "no",
          "selected": null
        }
      ],
      "success": null
    },
    "history": [
      {
        "id": "electricity",
        "question": "What kind of issue?",
        "answers": [
          {
            "id": "whereIsOutage",
            "text": "Does your neighborhood has power",
            "selected": false
          },
          {
            "id": "checkLocal",
            "text": "Are fuses ok in your apartment",
            "selected": true
          }
        ],
        "path": [],
        "success": null
      },
      {
        "id": "checkLocal",
        "question": "Are fuses ok in your apartment",
        "answers": [
          {
            "id": "localFusesDown",
            "text": "Turn them on. Is it solved?",
            "selected": false
          },
          {
            "id": "buildingFuses",
            "text": "Are fuses outside your apartment ok?",
            "selected": true
          }
        ],
        "path": [],
        "success": null
      },
      {
        "id": "buildingFuses",
        "question": "Are fuses outside your apartment ok?",
        "answers": [
          {
            "id": "noPower",
            "text": "Pay your bills!",
            "selected": false
          },
          {
            "id": "buildingFusesBroken",
            "text": "Fix fuses outside",
            "selected": false
          }
        ],
        "path": [],
        "success": true
      }
    ]
  }`)

const quizzStateFinish = JSON.parse(`{
    "path": "electricity;checkLocal;buildingFuses",
    "currentStep": {
      "id": "buildingFuses",
      "question": "Super good!",
      "answers": [],
      "success": true
    },
    "history": [
      {
        "id": "electricity",
        "question": "What kind of issue?",
        "answers": [
          {
            "id": "whereIsOutage",
            "text": "Does your neighborhood has power",
            "selected": false
          },
          {
            "id": "checkLocal",
            "text": "Are fuses ok in your apartment",
            "selected": true
          }
        ],
        "path": [],
        "success": null
      },
      {
        "id": "checkLocal",
        "question": "Are fuses ok in your apartment",
        "answers": [
          {
            "id": "localFusesDown",
            "text": "Turn them on. Is it solved?",
            "selected": false
          },
          {
            "id": "buildingFuses",
            "text": "Are fuses outside your apartment ok?",
            "selected": true
          }
        ],
        "path": [],
        "success": null
      },
      {
        "id": "buildingFuses",
        "question": "Are fuses outside your apartment ok?",
        "answers": [
          {
            "id": "noPower",
            "text": "Pay your bills!",
            "selected": false
          },
          {
            "id": "buildingFusesBroken",
            "text": "Fix fuses outside",
            "selected": false
          }
        ],
        "path": [],
        "success": true
      }
    ]
  }`)


const multiline = `
This is a **question** with *markdown*.
Can you see [image](https://image.flaticon.com/icons/png/128/2938/2938229.png) with cat ![Cat](https://image.flaticon.com/icons/png/128/2938/2938229.png)?`
storiesOf('Welcome!', module).add('to Storybook', () => <Welcome showApp={linkTo('Button')} />);

storiesOf('Components', module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))

 .add("Step", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} answers={answers}></Step>)
 .add("Step with markdown", () => <Step question={multiline} action={(a) => console.log("Selected ", a)} answers={answers}></Step>)
 .add("Step success", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} success={true} answers={[]}></Step>)
 .add("Step failure", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} success={false} answers={[]}></Step>)
 .add("HistoryStep", () => <HistoryStep question="How do you feel today?" answers={historyAnswers} path={["a","b"]}></HistoryStep>)
 .add("HistoryStep with markdown", () => <HistoryStep question="How do you *feel* **today?**" answers={historyAnswers} path={["a","b"]}></HistoryStep>)
 .add("Feedback", () => <Feedback path="a;b;c" />)
 .add("Quizz", () => <Quizz quizzId="q1" path={["electricity;checkLocal"]} selectAction={(q, s) => new Promise((res, rej) => res(quizzState))} />)
  .add("Quizz finished", () => <Quizz quizzId="q1" quizzState={quizzStateFinish} selectAction={(q, s) => new Promise((res, rej) => res(quizzStateFinish))} />)
  ;
