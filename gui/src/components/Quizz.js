import React from 'react';
import Step from './Step'
import Feedback from './Feedback';
import HistoryStep from './HistoryStep';

class Quiz extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            quizzId: props.quizzId,
            quizzState: {
                path: this.props.path,
                history: [],
                currentStep: {
                    id: "",
                    question: "Loading...",
                    answers: []
                },
            },
            selectAction: props.selectAction
        };
        this.loadState(this.props.quizzId, this.props.path)
    }

    componentWillReceiveProps() {
        console.log("Received props: ", this.props);
        this.loadState(this.props.quizzId, this.props.path)
    }


    loadState = (quizzId, path) => {
        console.log("Load state for quizz " + quizzId + " and path " + path);
        this.props.selectAction(quizzId, path)
            .then(e => {
                this.feedbackAction = (rate, comment) => {
                    this.props.feedbackSendAction(rate, comment, quizzId, path)
                };

                this.setState({
                    quizzState: e,
                    feedbackSendAction: this.feedbackAction
                });
                console.log("Have state: ", this.state)
                return e;
            })
    };

    scrollToBottom = () => {
        this.el.scrollIntoView({behavior: "smooth"});
    };

    componentDidMount() {
        this.scrollToBottom();
    }

    componentDidUpdate() {
        this.scrollToBottom();
    }

    render() {
        const selectFun = (quizzId, path) => {
            console.log("calling funcion selectFun for quizzId", quizzId)
            let pathQuery = this.state.quizzState.path
            if (pathQuery.length === 0) {
                pathQuery = path
            } else {
                pathQuery = pathQuery + ";" + path
            }
            this.props
                .selectAction(quizzId, pathQuery)
                .then(e => this.setState({quizzState: e}))
        };

        const history = this.state.quizzState.history.map(h =>
            <div key={"history_" + h.id}>
                <HistoryStep
                    quizzId={this.props.quizzId}
                    answers={h.answers}
                    question={h.question}
                    success={h.success}
                    action={(a, b) => alert("Not supported yet")}/>
                <hr/>
            </div>
        );
        const isThisLastStep = this.state.quizzState.currentStep.answers.length === 0;
        let currentTitle = isThisLastStep ? "Solution" : "Question:";

        const lastStep = <div>
            <h2>{currentTitle}</h2>
            <Step
                question={this.state.quizzState.currentStep.question}
                answers={this.state.quizzState.currentStep.answers}
                quizzId={this.props.quizzId}
                action={selectFun}
            /></div>;

        let feedback = <div/>;
        if (isThisLastStep) {
            feedback = <div>
                <h3>Feedback:</h3>
                <Feedback sendAction={this.state.feedbackSendAction}/>
            </div>
        }

        return (
            <div>
                <h2>History:</h2>
                {history}
                <hr/>
                <div ref={el => {
                    this.el = el;
                }}/>
                {lastStep}
                {feedback}

            </div>
        );
    }
}

export default Quiz
