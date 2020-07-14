import React from 'react';
import Button from 'react-bootstrap/Button';
import ButtonToolbar from 'react-bootstrap/ButtonToolbar';
import Form from "react-bootstrap/Form"
import Alert from 'react-bootstrap/Alert'
import "./Feedback.css"

class Feedback extends React.Component {

    constructor(props) {
        super(props)
        this.state = {
            send: false,
            feedbackText: "",
            feedbackRating: ""
        };
    }

    sendFeedback = (rating) => {
        console.log("Feedback send, rating: ", rating)
        this.setState({
            send: true,
            feedbackRating: rating
        });
        console.log("Current state ", this.state)
        this.props.sendAction(rating, this.state.feedbackText)

    }

    render() {
        let component = <Form>
            <Form.Group controlId="exampleForm.ControlTextarea1">
                <Form.Label>Enter comment and rate you experience</Form.Label>
                <Form.Control
                    as="textarea"
                    rows="5"
                    className="inputText"
                    placeholder="Put your comments here..."
                    onChange={e => {
                        console.log("Setting state, feedback text: ", e.target.value)
                        this.setState({feedbackText: e.target.value})
                    }}
                />
            </Form.Group>
            <ButtonToolbar className="center">
                <Button className="center" variant="dark" size="lg" onClick={() => this.sendFeedback("1")}><span
                    role="img" aria-label=":)" className="emoji"> &#x1f600;</span> </Button>
                <span>&nbsp;</span>
                <Button className="center" variant="dark" size="lg" onClick={() => this.sendFeedback("0")}><span
                    role="img" aria-label=":|" className="emoji"> &#x1f610; </span></Button>
                <span>&nbsp;</span>
                <Button className="center" variant="dark" size="lg" onClick={() => this.sendFeedback("-1")}><span
                    role="img" aria-label=":(" className="emoji"> &#x1f615;</span> </Button>
            </ButtonToolbar>
        </Form>

        if (this.state.send) {
            component = <Alert variant="info">
                <Alert.Heading>Thank you for your feedback</Alert.Heading>
                Your feedback was:
                <hr/>
                Rating: <em>{this.state.feedbackRating}</em>
                <br/>
                Your comment: <br/>
                <em>{this.state.feedbackText}</em>
            </Alert>
        }
        return (
            <div>
                {component}
            </div>
        );
    }
}

export default Feedback
