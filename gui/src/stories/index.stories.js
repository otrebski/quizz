import React from 'react';

import {storiesOf} from '@storybook/react';
import {MemoryRouter} from "react-router-dom"; // our router
import Step from '../components/Step'
import HistoryStep from '../components/HistoryStep'
import Tree from '../components/Tree'

import Feedback from '../components/Feedback';
import 'bootstrap/dist/css/bootstrap.css';
import Trees from "../components/Trees";
import Sessions from "../components/Sessions"
import SessionComponent from "../components/SessionComponent"
import SessionLoading from "../components/SessionLoading";
import SessionLoadingError from "../components/SessionLoadingError";
import SessionStep from "../components/SessionStep";
import Session from "../components/Session";

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

const sessions = JSON.parse(`{
  "sessions": [
    {
      "session": "36281dd8-920c-450a-aa30-3b5bae5f1fa6",
      "date": "2020-11-24 22:26",
      "treeId": "test1",
      "duration": 2323
    },
    {
      "session": "8a6689b0-89f6-44a0-8820-1b924bd04a61",
      "date": "2020-11-24 22:16",
      "treeId": "test1",
      "duration": 5220
    },
    {
      "session": "8a6689b0-89f6-44a0-8820-1b924bd04a61",
      "date": "2020-11-24 22:12",
      "treeId": "test1",
      "duration": 65
    },
    {
      "session": "8a6689b0-89f6-44a0-8820-1b924bd04a61",
      "date": "2020-11-24 22:11",
      "treeId": "test1",
      "duration": 41
    }
    ]
  }`)

const session = JSON.parse(`{
  "details": {
    "session": "3f086231-a17f-4a97-bf7b-ca816fd717c5",
    "date": "2020-12-08 17:00:12",
    "treeId": "demo",
    "version": 94,
    "duration": 20041
  },
  "steps": [
    {
      "question": "Issue with service X",
      "date": "2020-12-08 17:00:16",
      "duration": 3651,
      "answers": [
        {
          "id": "3.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Alert from PagerDuty",
          "selected": true
        },
        {
          "id": "4.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Call from user",
          "selected": false
        }
      ],
      "success": null
    },
    {
      "question": "Which **alert?**",
      "date": "2020-12-08 17:00:22",
      "duration": 5877,
      "answers": [
        {
          "id": "6.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Alert x",
          "selected": false
        },
        {
          "id": "5.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Alert y",
          "selected": true
        }
      ],
      "success": null
    },
    {
      "question": "Production is on fire\\n![fire image](https://upload.wikimedia.org/wikipedia/commons/thumb/1/17/School_burn.JPG/180px-School_burn.JPG)",
      "date": "2020-12-08 17:00:27",
      "duration": 4935,
      "answers": [],
      "success": null
    },
    {
      "question": "What is the problem?\\nSome paragraph with *important* and **important bold text**.\\nSome list\\n* task 1 [doc](www.google.com/?q=restarting+database+for+dummies)\\n* task 2",
      "date": "2020-12-08 17:00:32",
      "duration": 5503,
      "answers": [
        {
          "id": "8.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Problem A",
          "selected": false
        },
        {
          "id": "9.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Problem B",
          "selected": true
        }
      ],
      "success": null
    }
  ]
}`)

const loadedSteps = new Map()
loadedSteps.set("root;3.82d7.ffd6e842a-f910.2cd53981f", JSON.parse(`{
    "question": "This is question",
    "answers": [
    {
        "id": "x1",
        "text": "Answer 1",
        "selected": true
    },
    {
        "id": "x2",
        "text": "Answer 2",
        "selected": false
    }
],
    "success": true
}`))

const sessionStep = JSON.parse(`
    {
      "question": "Issue with service X",
      "date": "2020-12-08 17:00:16",
      "duration": 3651,
      "answers": [
        {
          "id": "3.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Alert from PagerDuty",
          "selected": true
        },
        {
          "id": "4.82d7.ffd6e842a-f910.2cd53981f",
          "text": "Call from user",
          "selected": false
        }
      ],
      "success": null
    }`)

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
    .add("Tree in progress", () => <Tree treeId="q1" path={["electricity;checkLocal"]}
                                         selectAction={(q, s) => new Promise((res) => res(treeStateInProgress))}/>)
    .add("Tree finished", () => <Tree treeId="q1" treeState={treeStateFinish} selectAction={(q, s) => new Promise((res) => res(treeStateFinish))}/>)


storiesOf('Other', module)
    .addDecorator(story => (
        <MemoryRouter initialEntries={['/']}>{story()}</MemoryRouter>
    ))
    .add("Feedback", () => <Feedback path="a;b;c"/>)
    .add("Sessions", () => <Sessions loadAction={(q, s) => new Promise((res) => res(sessions))}/>)
    .add("Sessions loading error", () => <Sessions loadAction={(q, s) => new Promise((res, rej) => rej(new Error("?")))}/>)
    .add("SessionComponent", () => <SessionComponent treeId="demo" sessionId="abcdefgh" loadAction={(q, s)=>new Promise((res, rej)=> res(session))} />)
    .add("SessionLoading", () => <SessionLoading/>)
    .add("SessionLoadingError", () => <SessionLoadingError/>)
    .add("Session", () => <Session session={session}/>)
    .add("SessionStep", () => <SessionStep index={2} step={sessionStep}/>)
;


