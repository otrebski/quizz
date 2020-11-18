import React from 'react';

import {Link} from "react-router-dom";
import Alert from 'react-bootstrap/Alert'
import Button from 'react-bootstrap/Button'

import "./Trees.css"

class Trees extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            trees: [],
            treesWithErrors: [],
            loading: true,
            error: ""
        }
        this.loadTrees(true)
    }

    loadTrees = (initial) => {
        if (!initial) {
            this.setState({
                loading: true,
                loadingError: false,
                error: ""
            })
        }
        this.props.loadAction()
            .then(q =>
                this.setState({
                    trees: q.trees,
                    treesWithErrors: q.treesWithErrors,
                    loading: false
                })
            ).catch(e => {
                this.setState({loading: false, loadingError: true, error: e.message, trees: [], treesWithErrors: []})
            }
        )
    };

    render() {
        const noTrees = (this.state.trees.length === 0) ? <div><Alert variant={"secondary"}>
            <Alert.Heading>There is no decision trees defined</Alert.Heading>
            <hr/>
            <div>
               Check in documentation how to define and load decision tree.
            </div>

        </Alert></div> : <div/>
        const trees = this.state.trees.map(q =>
            <div id="tree-link" key={q.id} className="quizzLink">
                <Link id={q.id} to={`/tree/${q.id}`}>{q.title}</Link>
            </div>);
        const error = this.state.loadingError ? <Alert variant={"danger"}>Can't load data: {this.state.error}</Alert> : <div/>
        const invalidTrees = (this.state.treesWithErrors.length !== 0) ? <div><Alert variant={"warning"}>
            <Alert.Heading>Following decision trees can't be parsed {this.state.treesWithErrors.size}: </Alert.Heading>
            <hr/>
            <div className="quizzParsingError">
                {this.state.treesWithErrors.map(eq => <p key={eq.id}><b>Quizz {eq.id}:</b> {eq.error}</p>)}
            </div>

        </Alert></div> : <div/>

        return (
            <div>
                <h2 id="quizz-list-header">Choose decision tree to start:</h2>
                {noTrees}
                {error}
                {trees}
                {invalidTrees}
                <Button variant="light" disabled={this.state.isLoading}
                        onClick={!this.state.loading ? () => this.loadTrees(false) : null}>{this.state.loading ? 'Loading…' : 'Click to reload'}</Button>
            </div>
        );
    }
}

export default Trees
