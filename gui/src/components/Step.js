import React from 'react';
import Card from 'react-bootstrap/Card'
import ListGroup from 'react-bootstrap/ListGroup'
import "./Step.css"
import {Link} from "react-router-dom";
import ReactMarkdown from "react-markdown/with-html";

class Step extends React.Component {

    render() {
        let finalStep = <div/>
        if (this.props.success === true) {
            finalStep = <div id="final_step"><span role="img" aria-label="Checkmarks">&#10003;</span> Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div id="final_step"><span role="img" aria-label="Cross">&#x274C;</span> Failure!</div>
        }
        let path = this.props.path === "" ? "" : `${this.props.path};`
        // this.props.answers.map(a => console.log("ID", a.id));
        const answers2 = this.props.answers.map(a =>
            <Link key={a.text} style={{textDecoration: 'none'}} to={`/quizz/${this.props.quizzId}/path/${path}${a.id}`}>
                <ListGroup.Item key={a.text} action>
                    {a.text}
                </ListGroup.Item>
            </Link>
        )
        const id = (this.props.answers.length === 0) ? "final_step" : "question";
        return (
            <div id={id}>
                <Card border="info" style={{width: '90%'}} className="step">
                    <Card.Body>
                        <Card.Title>
                            <ReactMarkdown
                                source={this.props.question.replaceAll('\n', '\n\n')}
                                skipHtml={true}/>
                        </Card.Title>
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
