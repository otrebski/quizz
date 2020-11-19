import React from 'react';
import Step from './Step'
import Feedback from './Feedback';
import HistoryStep from './HistoryStep';

class Tree extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            treeId: props.treeId,
            treeState: {
                path: this.props.path,
                history: [],
                currentStep: {
                    id: "",
                    question: "Loading...",
                    answers: []
                },
            },
            loading: true,
            loadingError: false,
            errorMessage: "",
            selectAction: props.selectAction
        };
        this.loadState(this.props.treeId, this.props.path)
    }

    loadState = (treeId, path) => {
        console.log("Loading state for tree " + treeId + " and path " + path);
        this.setState({loadingError: false, loading: true})
        let x = this.props.selectAction(treeId, path)
        x.then(e => {
            console.log("State loaded")
            this.feedbackAction = (rate, comment) => {
                this.props.feedbackSendAction(rate, comment, treeId, path)
            };
            this.setState({
                treeState: e,
                feedbackSendAction: this.feedbackAction,
                history: e.history,
                loading: false
            });
            console.log("State loaded: ", this.state)
            this.scrollToBottom()
            return e;
        })
            .catch(e => {
                console.log("ERROR!", e)
                this.setState({
                    loading: false,
                    loadingError: true,
                    errorMessage: e.stack
                })
            })
    };

    scrollToBottom = () => {
        this.bottomElement.scrollIntoView({behavior: "smooth"});
    };

    componentDidMount() {
        this.loadState(this.props.treeId, this.props.path)
    }

    componentDidUpdate(prevProps) {
        if (this.props.treeId !== prevProps.treeId || this.props.path !== prevProps.path) {
            this.loadState(this.props.treeId, this.props.path)
            this.scrollToBottom();
        }
    }

    render() {
        console.log("Rendering tree with props", this.props);
        if (this.state.loading) {
            return <div>loading...</div>
        } else if (this.state.loadingError) {
            return <div>Loading error: {this.state.errorMessage}</div>
        } else {
            const history = this.state.treeState.history.map(h =>
                <div key={"history_" + h.id}>
                    <HistoryStep
                        treeId={this.props.treeId}
                        answers={h.answers}
                        id={h.id}
                        path={h.path}
                        question={h.question}
                        success={h.success}/>
                    <hr/>
                </div>
            );
            const isThisLastStep = this.state.treeState.currentStep.answers.length === 0;
            let currentTitle = isThisLastStep ? "Solution" : "Question:";
            const lastStep = <div>
                <h2>{currentTitle}</h2>
                <Step
                    question={this.state.treeState.currentStep.question}
                    answers={this.state.treeState.currentStep.answers}
                    path={this.state.treeState.path}
                    treeId={this.props.treeId}
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
                        this.bottomElement = el; //used for scrolling
                    }}/>
                    {lastStep}
                    <hr/>
                    <hr/>
                    {feedback}
                </div>
            );
        }
    }
}

export default Tree
