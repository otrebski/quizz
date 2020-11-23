import React from 'react';
import Button from 'react-bootstrap/Button';
import Spinner from 'react-bootstrap/Spinner'
import "./Feedback.css"
import {Image} from "react-bootstrap";

class Settings extends React.Component {

    constructor(props) {
        super(props)
        this.state = {
            send: false,
            sending: false
        };
    }

    sendDemo = () => {
        this.setState({
            sending: true,
        });
        this.props.loadDemo()
            .then(demo => {
                console.log("Loaded ", demo);
                this.props.addDemo("demo", demo)
            })
            .then(_ => {
                console.log("Added")
                this.setState({
                    sending: false,
                    send: true
                })
            }).catch(e => {
            console.log("Not added", e)
            this.setState({
                sending: false,
                send: false
            })
        })


    }

    render() {
        let component = <div>
            <Button className="center" variant="light" size="s" onClick={() => this.sendDemo()}><span
                role="img" aria-label=":|">➕ Add demo decision tree</span></Button>
        </div>
        if (this.state.sending) {
            component = <div><Button disabled={true} className="center" variant="light" size="s" onClick={() => this.sendDemo()}><span
                role="img" aria-label=":|">➕ Sending</span><Spinner as="span" animation="grow" size="sm" role="status" aria-hidden="true"/></Button></div>
        } else if (this.state.send) {
            component = <div><Button disabled={true} className="center" variant="light" size="s" onClick={() => this.sendDemo()}><span
                role="img" aria-label=":|">➕ Demo tree added</span></Button></div>
        }
        return (
            <div align="left" style={{"margin-left": 20}}>
                {component}
                <h3>How to create own decision tree</h3>
                <div>Application is using <a href="https://drive.mindmup.com/">Mindmup</a> format. Mindmup have to fulfill following requirements:</div>
                <ul>
                    <li>There is only one root node</li>
                    <li>Every connection line has label</li>
                </ul>
                <div>Mindmup processing rules:</div>
                <ul>
                    <li>Node is a question, label on connection line is answer.</li>
                    <li>Node text is merged with note attached to node</li>
                    <li>Following markdown is support:</li>
                    <ul>
                        <li>Italic: <code>*word*</code></li>
                        <li>Bold: <code>**word**</code></li>
                        <li>Image: <code>![alt text](url)</code></li>
                        <li>Link: <code>[text](url)</code></li>
                        <li>List:<code> <br/>* line 1<br/>* line 2</code></li>
                        <li>Code:<br/><code>~~~js<br/>
                        console.log("it works!)<br/>
                        ~~~</code> </li>
                    </ul>
                </ul>
            <br/>
            On picture below is a mindmup used as demo decision tree.
                <Image src="/Demo.png"/>
            </div>
        );
    }
}

export default Settings
