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
            <div>
                {component}
                <Image src="/Demo.png"/>
            </div>
        );
    }
}

export default Settings
