import React from 'react';
import ListGroup from 'react-bootstrap/ListGroup'
import {Link} from "react-router-dom";
import "./HistoryStep.css"
import ReactMarkdown from "react-markdown/with-html";
class HistoryStep extends React.Component {

    render() {
        let finalStep = <div/>;
        if (this.props.success === true) {
            finalStep = <div>Great success</div>
        } else if (this.props.success === false) {
            finalStep = <div>Failure!</div>
        }
        // console.log("Props:", this.props)
        const answers = this.props.answers.map(a => {
                let variant = a.selected ? "success" : "light";
                // console.log(`${a.id} is selected: ${a.selected}, variant: ${variant}`);
                let path = Array.from(this.props.path);
                path.push(this.props.id);
                path.push(a.id);
                return <Link key={a.id} to={`/tree/${this.props.treeId}/path/${path.join(";")}`}>
                <ListGroup.Item
                    key={a.id}
                    action
                    variant={variant}
                >{a.text}
                </ListGroup.Item>
                </Link>
            }
        );

        return (
            <div className="historyStep" id={`history-step-${this.props.path.join(";")}`}>
                <h3>
                   <ReactMarkdown
                       source={this.props.question.replaceAll('\n','\n\n')}
                       skipHtml={true} />
                </h3>
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
