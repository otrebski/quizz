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
      <Router forceRefresh={false} >
        <h1><Link id="home" to={"/"}>Home</Link></h1>
        <div className="App">
          <Route path="/" exact render={() => Index} />
          <Route path="/quizz/:id/path/:path" exact render={(query) => {
            console.log("path with id /quizz/:id/path/:path", query)
            return <Quizz
                quizzId={query.match.params.id}
                path={query.match.params.path}
                selectAction={(quizzId, path) => Api.sendReponse(quizzId, path)}
                feedbackSendAction={(rate, comment, quizzId, path) => Api.sendFeedback(rate, comment, quizzId, path)}
            />
          }
          } />

          <Route path="/quizz/:id" exact render={(query) => {
            console.log("path /quizz/:id", query)
            const match = query.match;
            return <Quizz
                {...query}
                quizzId={match.params.id}
                path=""
                selectAction={(quizzId, path) => Api.sendReponse(quizzId, path)}
                feedbackSendAction={(rate, comment, quizzId, path) => Api.sendFeedback(rate, comment, quizzId, path)}
            />
          }
          }
          />
        </div>

        <br />
        <br />
        <br />
        <br />
        <br />
        <br />
        <br />
      </Router>
      <div>This site is using cookies</div>
    </div>
  );
}

export default App;
