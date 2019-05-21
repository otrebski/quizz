import React from 'react';
import './App.css';
import Quizz from "./components/Quizz"
import Api from "./Api"
import { BrowserRouter as Router, Route , Link} from "react-router-dom";
import Quizzes from "./components/Quizzes";



function App() {

  const Index = <div><Quizzes loadAction={() => Api.getQuizes()}/></div>

  return (
    <div>
      <Router>
        <h1><Link to={"/"}>Home</Link></h1>
        <div className="App">
          <Route path="/" exact render={() => Index} />
          <Route path="/quizz/:id" exact render={(query) => {
            const match = query.match;
            return <Quizz
              quizzId={match.params.id}
              path=""
              selectAction={(quizzId, path) => Api.sendReponse(quizzId, path)}
              feedbackSendAction={(rate, comment, quizzId, path) => Api.sendFeedback(rate, comment, quizzId, path)}
            />
          }
          }
          />
          <Route path="/quizz/:id/path/:path" exact render={(query) => {
            return <Quizz
              quizzId={query.match.params.id}
              path={query.match.params.path}
              selectAction={(quizzId, path) => Api.sendReponse(quizzId, path)}
              feedbackSendAction={(rate, comment, quizzId, path) => Api.sendFeedback(rate, comment, quizzId, path)}
            />
          }
          } />
        </div>
        <br />
        <br />
        <br />
        <br />
        <br />
        <br />
        <br />
      </Router>
    </div>
  );
}

export default App;
