'use strict';

var React = require('react');

var ExtractorExampleMessage = React.createClass({
    render() {
        var originalMessage = <span id="xtrc-original-example" style={{display:"none"}}>{this.props.example}</span>;
        var messagePreview;

        if(this.props.example === undefined || this.props.example === "") {
            messagePreview = (
                <div className="alert alert-warning xtrc-no-example">
                    Could not load an example of field '{this.props.field}'. It is not possible to test
                    the extractor before updating it.
                </div>
            );
        } else {
            messagePreview = (
                <div className="well well-small xtrc-new-example">
                    <span id="xtrc-example">{this.props.example}</span>
                </div>
            );
        }

        return (
            <div>
                {originalMessage}
                {messagePreview}
            </div>
        );
    }
});

module.exports = ExtractorExampleMessage;