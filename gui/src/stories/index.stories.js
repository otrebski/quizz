import React from 'react';

import {storiesOf} from '@storybook/react';
import {MemoryRouter} from "react-router-dom"; // our router
import Step from '../components/Step'
import HistoryStep from '../components/HistoryStep'
import Tree from '../components/Tree'

import Feedback from '../components/Feedback';
import 'bootstrap/dist/css/bootstrap.css';
import Trees from "../components/Trees";

const answers = [
    {id: "id1", text: "Yes"},
    {id: "id2", text: "No"},
    {id: "id3", text: "Maybe"}
]
const historyAnswers = [
    {id: "id1", text: "Yes"},
    {id: "id2", text: "No", selected: true},
    {id: "id3", text: "Maybe"}
]

const treeStateStarting = JSON.parse(`{
  "path": "",
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
  "history": []
}`)
const treeStateInProgress = JSON.parse(`{
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

const treeStateFinish = JSON.parse(`{
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

const trees = JSON.parse(`{
  "trees": [
    { "id": "q1", "title": "Tree 1" },
    { "id": "q2", "title": "Tree 2" }
  ],
  "treesWithErrors": []
}`)

const treesWithErrors = JSON.parse(`{
  "trees": [
    { "id": "q1", "title": "Tree 1" },
    { "id": "q2", "title": "Tree 2" }
  ],
  "treesWithErrors": [
    {"id": "q3", "error": "Invalid syntax ..."},
    {"id": "q4", "error": "Node(1) / ..."}
  ]
}`)

const multiline = `
This is a **question** with *markdown*.
Can you see [image](https://image.flaticon.com/icons/png/128/2938/2938229.png) with cat ![Cat](https://image.flaticon.com/icons/png/128/2938/2938229.png)?`
// storiesOf('Welcome', module).add('to Storybook', () => <div>Hello</div>);

storiesOf('Main page', module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))
    .add("Trees loading error", () => <Trees loadAction={() => new Promise((res, rej) => rej(new Error("server error")))}/>)
    .add("Trees loaded with errors", () => <Trees loadAction={() => new Promise((res) => res(treesWithErrors))}/>)
    .add("Trees all loaded", () => <Trees loadAction={() => new Promise((res) => res(trees))}/>)
;

storiesOf("Step", module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))
    .add("Step", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} answers={answers}/>)
    .add("Step with markdown", () => <Step question={multiline} action={(a) => console.log("Selected ", a)} answers={answers}/>)
    .add("Step success", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} success={true} answers={[]}/>)
    .add("Step failure", () => <Step question="How do you feel today?" action={(a) => console.log("Selected ", a)} success={false} answers={[]}/>)
    .add("HistoryStep", () => <HistoryStep question="How do you feel today?" answers={historyAnswers} path={["a", "b"]}/>)
    .add("HistoryStep with markdown", () => <HistoryStep question="How do you *feel* **today?**" answers={historyAnswers} path={["a", "b"]}/>)

storiesOf("Tree", module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))
    .add("Tree starting", () => <Tree treeId="q1" path={[""]} selectAction={(q, s) => new Promise((res) => res(treeStateStarting))}/>)
    .add("Tree in progress", () => <Tree treeId="q1" path={["electricity;checkLocal"]} selectAction={(q, s) => new Promise((res) => res(treeStateInProgress))}/>)
    .add("Tree finished", () => <Tree treeId="q1" treeState={treeStateFinish} selectAction={(q, s) => new Promise((res) => res(treeStateFinish))}/>)


storiesOf('Other', module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))
    .add("Feedback", () => <Feedback path="a;b;c"/>)
;


