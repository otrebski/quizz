import Badge from "react-bootstrap/Badge";
import ReactMarkdown from "react-markdown/with-html";
import ListGroup from "react-bootstrap/ListGroup";
import React from "react";

function SessionStep(props) {
    return <div>
        <hr/>
        <h4><Badge variant="secondary" pill={false}>{props.index + 1}: {props.step.date}</Badge></h4>
        <br/>
        <h4>
            <ReactMarkdown source={props.step.question.replaceAll('\n', '\n\n')}
                           skipHtml={true}/>
        </h4>

        {props.step.answers.map(answer =>
            <ListGroup.Item key={answer.id} variant={answer.selected ? "success" : "light"}>
                {answer.text}
            </ListGroup.Item>
        )}
        <br/>
    </div>
}

export default SessionStep