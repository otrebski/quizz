import React from 'react';

import {Link} from "react-router-dom";

class Quizzes extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            quizzes: []
        }
        this.loadQuizzes()
    }

    loadQuizzes = () => {
        console.log("Loading quizzes");
        this.props.loadAction()
            .then(q =>
                // console.log("Loaded ",q)
                this.setState({quizzes: q.quizzes})
            )
    };

    render() {
        const quizzes = this.state.quizzes.map(q =>
            <div key={q.id}><Link to={`/quizz/${q.id}`}>{q.title}</Link></div>);
        return (
            <div>
                <h2>Choose quiz to start:</h2>
                {quizzes}
            </div>
        );
    }
}

export default Quizzes
