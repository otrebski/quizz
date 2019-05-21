import React from 'react';
import Card from 'react-bootstrap/Card'
import ListGroup from 'react-bootstrap/ListGroup'
import "./Step.css"

class Step extends React.Component {

    render() {
        let finalStep = <div />
        if (this.props.success === true) {
            finalStep = <div><span role="img" aria-label="Checkmarks">&#10003;</span> Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div><span role="img" aria-label="Cross">&#x274C;</span> Failure!</div>
        }
        const answers2 = this.props.answers.map(a =>
            <ListGroup.Item
                key={a.id}
                onClick={() => this.props.action(this.props.quizzId, a.id)}
                action                
            >{a.text}
            </ListGroup.Item>
        )
        return (
            <div>
                <Card style={{ width: '20rem' }} className="step">
                    <Card.Body>
                        <Card.Title>{this.props.question}</Card.Title>
                        <ListGroup>
                            {answers2}
                        </ListGroup>
                    </Card.Body>
                </Card>

                {finalStep}
            </div>
        );
    }
}

export default Step
