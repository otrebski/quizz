import React from 'react';
import './App.css';
import Quizz from "./components/Quizz"
import Api from "./Api"
import { BrowserRouter as Router, Route , Link} from "react-router-dom";
import Trees from "./components/Trees";

function App() {

  const Index = <div><Trees loadAction={() => Api.getQuizes()}/></div>

  return (
    <div>
      <Router forceRefresh={false} >
        <h1><Link id="home" to={"/"}>Home</Link></h1>
        <div className="App">
          <Route path="/" exact render={() => Index} />
          <Route path="/tree/:id/path/:path" exact render={(query) => {
            return <Quizz
                treeId={query.match.params.id}
                path={query.match.params.path}
                selectAction={(treeId, path) => Api.sendReponse(treeId, path)}
                feedbackSendAction={(rate, comment, treeId, path) => Api.sendFeedback(rate, comment, treeId, path)}
            />
          }
          } />

          <Route path="/tree/:id" exact render={(query) => {
            const match = query.match;
            return <Quizz
                {...query}
                treeId={match.params.id}
                path=""
                selectAction={(treeId, path) => Api.sendReponse(treeId, path)}
                feedbackSendAction={(rate, comment, treeId, path) => Api.sendFeedback(rate, comment, treeId, path)}
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
