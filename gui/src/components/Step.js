import React from 'react';
import Card from 'react-bootstrap/Card'
import ListGroup from 'react-bootstrap/ListGroup'
import "./Step.css"
import {Link} from "react-router-dom";

class Step extends React.Component {

    render() {
        let finalStep = <div/>
        if (this.props.success === true) {
            finalStep = <div><span role="img" aria-label="Checkmarks">&#10003;</span> Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div><span role="img" aria-label="Cross">&#x274C;</span> Failure!</div>
        }
        let path =  this.props.path === "" ?  "" : `${this.props.path};`
        const answers2 = this.props.answers.map(a =>
            <ListGroup.Item key={a.id} action>
                <Link style={{ textDecoration: 'none' }} to={`/quizz/${this.props.quizzId}/path/${path}${a.id}`}>{a.text}</Link>
            </ListGroup.Item>
        )
        return (
            <div>
                <Card border="info" style={{width: '20rem'}} className="step">
                    <Card.Body>
                        <Card.Title>{this.props.question.split('\n').map((item, i) => {
                            return <p key={i}>{item}</p>;
                        })}</Card.Title>
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
