import React from "react";
import Table from "react-bootstrap/Table";
import Spinner from 'react-bootstrap/Spinner'
import Alert from 'react-bootstrap/Alert'
import {Link} from "react-router-dom";
import DateUtil from "../DateUtil";


class Sessions extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            error: false,
            loading: false,
            loaded: false,
        }
    }


    load() {
        this.setState({
            loading: true,
            error: false,
        })
        this.props.loadAction()
            .then(sessions =>
                this.setState({
                    sessions: sessions,
                    loading: false,
                    loaded: true
                })
            ).catch(e => {
            console.log("Error", e);
            this.setState({loading: false, error: true, loaded: false})
        })
    }


    componentDidMount() {
        this.load()
    }

    render() {
        console.log("Rendering ", this.state)
        let sessions = <div>Loading</div>
        if (this.state.loading) {
            return <div><Spinner animation="border"/>Loading</div>
        } else if (this.state.error) {
            return <Alert variant="danger">Can't load sessions</Alert>
        } else if (this.state.loaded && this.state.sessions.sessions.length === 0) {
            return <Alert variant="info">No sessions yet</Alert>
        } else if (this.state.loaded && this.state.sessions.sessions.length > 0) {
            sessions = <Table striped bordered hover>
                <thead>
                <tr>
                    <th>#</th>
                    <th>Tree Id</th>
                    <th>Date</th>
                    <th>Duration</th>
                    <th>Details</th>
                </tr>
                </thead>
                <tbody>
                {this.state.sessions.sessions.map((session, index) =>
                    <tr key={index}>

                        <td>{index + 1}</td>
                        <td>{session.treeId}</td>
                        <td>{session.date}</td>
                        <td>{DateUtil.formatTime(session.duration)}</td>
                        <td><Link to={`/tracking/session/${session.session}/tree/${session.treeId}`}><span>🗒️ check details</span></Link></td>
                    </tr>
                )}
                </tbody>
            </Table>
        }

        return <div><h2>Sessions</h2><br/>
            {sessions}
        </div>
    }
}

export default Sessions