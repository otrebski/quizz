import React from 'react';
import './App.css';
import Tree from "./components/Tree"
import Api from "./Api"
import { BrowserRouter as Router, Route} from "react-router-dom";
import Trees from "./components/Trees";
import Navbar from 'react-bootstrap/Navbar'
import Nav from 'react-bootstrap/Nav'

function App() {

  const Index = <div><Trees loadAction={() => Api.getTrees()}/></div>

  return (
    <div>
      <Navbar bg="dark" variant="dark" sticky="top">
        <Navbar.Brand>🤷‍♂ Decision Tree 🤷‍♀️</Navbar.Brand>
        <Nav>
          <Nav.Link href="/">Home</Nav.Link>
          <Nav.Link href="/docs">Swagger API</Nav.Link>
        </Nav>
      </Navbar>

      <Router forceRefresh={false} >
        <div className="App">
          <Route path="/" exact render={() => Index} />
          <Route path="/tree/:id/path/:path" exact render={(query) => {
            return <Tree
                treeId={query.match.params.id}
                path={query.match.params.path}
                selectAction={(treeId, path) => Api.sendResponse(treeId, path)}
                feedbackSendAction={(rate, comment, treeId, path) => Api.sendFeedback(rate, comment, treeId, path)}
            />
          }
          } />

          <Route path="/tree/:id" exact render={(query) => {
            const match = query.match;
            return <Tree
                {...query}
                treeId={match.params.id}
                path=""
                selectAction={(treeId, path) => Api.sendResponse(treeId, path)}
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
