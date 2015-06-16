'use strict';

var React = require('react');
var MessageLoader = require('./MessageLoader');

var ExtractorExampleMessage = React.createClass({
    getInitialState() {
        return ({
            example: "",
            field: ""
        });
    },
    componentWillMount() {
        this.setState({example: this.props.example});
    },
    onExampleLoaded(message) {
        var newExample = message.fields[this.props.field];

        if (newExample !== null && newExample !== undefined && newExample !== "") {
            this.setState({example: newExample});
        }
    },
    render() {
        var originalMessage = <span id="xtrc-original-example" style={{display:"none"}}>{this.state.example}</span>;
        var messagePreview;

        if(this.state.example === "") {
            messagePreview = (
                <div className="alert alert-warning xtrc-no-example">
                    Could not load an example of field '{this.props.field}'. It is not possible to test
                    the extractor before updating it.
                </div>
            );
        } else {
            messagePreview = (
                <div className="well well-sm xtrc-new-example">
                    <span id="xtrc-example">{this.state.example}</span>
                </div>
            );
        }

        return (
            <div>
                {originalMessage}
                {messagePreview}
                <MessageLoader onMessageLoaded={this.onExampleLoaded}/>
            </div>
        );
    }
});

module.exports = ExtractorExampleMessage;