import React from 'react';
import ListGroup from 'react-bootstrap/ListGroup'
import {Link} from "react-router-dom";
import "./HistoryStep.css"

class HistoryStep extends React.Component {

    render() {
        let finalStep = <div/>;
        if (this.props.success === true) {
            finalStep = <div>Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div>Failure!</div>
        }
        const answers = this.props.answers.map(a => {
                let variant = a.selected ? "success" : "light";
                // console.log(`${a.id} is selected: ${a.selected}, variant: ${variant}`);
                let path = Array.from(this.props.path);
                path.push(this.props.id);
                path.push(a.id);
                return <Link to={`/quizz/${this.props.quizzId}/path/${path.join(";")}`}>
                <ListGroup.Item
                    key={a.id}
                    action
                    variant={variant}
                >{a.text}
                </ListGroup.Item>
                </Link>
            }
        );
        const text = this.props.question.split('\n').map((item, i) => {
            return <p key={i}>{item}</p>;
        })
        return (
            <div className="historyStep">
                <h3>{text}</h3>
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
