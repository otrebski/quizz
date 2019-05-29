import React from 'react';
import ListGroup from 'react-bootstrap/ListGroup'
import "./HistoryStep.css"
import {Link} from "react-router-dom";

class HistoryStep extends React.Component {

    render() {
        let finalStep = <div/>;
        if (this.props.success === true) {
            finalStep = <div>Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div>Failure!</div>
        }

        const answers = this.props.answers.map(a => {
            let variant= a.selected ? "light" : "success";

            return <ListGroup.Item
                key={a.id}
                // onClick={() => this.props.action(this.props.quizzId, a.id)}
                action
                variant={variant}
            >
            <Link to={`/quizz/${this.props.quizzId}/path/${a.id}`}>{a.text}</Link>
            </ListGroup.Item>
        }
        );

        return (
            <div className="historyStep">
                <h3>{this.props.question}</h3>
                <div>
                    Answers:
                    {answers}
                </div>
                {finalStep}
            </div>
        );
    }
}

export default HistoryStep
