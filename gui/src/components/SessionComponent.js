import React from "react";
import SessionLoading from "./SessionLoading";
import SessionLoadingError from "./SessionLoadingError";
import SessionStep from "./SessionStep";
import DateUtil from "../DateUtil"


class SessionComponent extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            loading: true,
            loaded: false,
            error: false,
        }
    }


    componentDidMount() {
        this.loadSession()
    }

    loadSession() {
        this.setState({
            loading: true,
            loaded: false,
            error: false,
        })
        this.props.loadAction(this.props.sessionId, this.props.treeId)
            .then(session => {
                    this.setState({
                        loading: false,
                        loaded: true,
                        error: false,
                        session: session,
                        // steps: session.steps

                    });
                }
            ).catch(e => {
            this.setState({loading: false, loaded: false, error: true})
        })
    }

    render() {
        if (this.state.loading) {
            return <SessionLoading/>
        } else if (this.state.error) {
            return <SessionLoadingError/>
        } else if (this.state.error && this.state.loading) { //Wrong if instead of commit
            return <div>
                <h2> Tree: {this.props.treeId} [{this.props.version}], duration: {DateUtil.formatTime(this.state.details.duration)}<br/></h2>
                <h3>Steps: &nbsp;</h3>
            </div>
        } else if (this.state.loaded){
            return <div>
                <h2> Tree: {this.props.treeId} [{this.state.session.details.version}],</h2>
                    duration: {DateUtil.formatTime(this.state.session.details.duration)}<br/>
                <h3>Steps: &nbsp;</h3>
                {
                    this.state.session.steps.map((step, index) => {
                        const min = Date.parse(this.state.session.details.date)
                        const max = Date.parse(this.state.session.details.date) + this.state.session.details.duration
                        return <SessionStep key={index}
                            min={min}
                            max={max}
                            step={step}
                            index={index}
                        />
                    }
                )}
            </div>
        }
    }
}


export default SessionComponent