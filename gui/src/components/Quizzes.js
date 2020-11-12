import React from 'react';

import {Link} from "react-router-dom";
import Alert from 'react-bootstrap/Alert'

class Quizzes extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            quizzes: [],
            errorQuizzes: [],
            loading: true,
            error: ""
        }
        this.loadQuizzes()
    }

    loadQuizzes = () => {
        this.setState({
            loading: true,
            loadingError: false,
            error: ""
        })
        this.props.loadAction()
            .then(q =>
                this.setState({
                    quizzes: q.quizzes,
                    errorQuizzes: q.errorQuizzes
                })
            ).catch(e => {
            this.setState({loading: false, loadingError: true, error: e.message, quizzes:[], errorQuizzes:[]})
        })
    };

    render() {
        const noQuizz = (this.state.quizzes.length === 0) ? <div>No quizzes</div> : <div/>
        const quizzes = this.state.quizzes.map(q =>
            <div id="quizz-link" key={q.id}><Link id={q.id} to={`/quizz/${q.id}`}>{q.title}</Link></div>);
        const error = this.state.loadingError ? <Alert variant={"danger"}>Can't load data: {this.state.error}</Alert> : <div/>
        const invalidQuizzes = (this.state.errorQuizzes.length !== 0) ? <div><Alert variant={"warning"}>
            <Alert.Heading>Following quizzes can't be parsed {this.state.errorQuizzes.size}: </Alert.Heading>
            <hr/>
            <div>
                {this.state.errorQuizzes.map(eq => <p key={eq.id}>Quizz {eq.id}: {eq.error}</p>)}
            </div>

        </Alert></div> : <div/>

        return (
            <div>
                <h2 id="quizz-list-header">Choose quiz to start:</h2>
                {noQuizz}
                {error}
                {quizzes}
                {invalidQuizzes}
            </div>
        );
    }
}

export default Quizzes
