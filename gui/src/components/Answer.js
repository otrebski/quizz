import React from 'react';

class Answer extends React.Component {

    render() {
        return (
            <div>
                <input
                    type="radio"
                    value={this.props.text}
                    name="answer"
                    onChange={this.props.action}
                    checked={this.props.selected}
                /> {this.props.text}
            </div>
        );
    }
}

export default Answer
