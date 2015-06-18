'use strict';

var React = require('react');
var MessageDetail = require('./MessageDetail');

var MessageShow = React.createClass({
    possiblyHighlight(fieldName) {
        // No highlighting for the message details view.
        return this.props.message.fields[fieldName];
    },
    render() {
        return (
            <div className="row content">
                <div className="col-md-12">
                    <MessageDetail {...this.props} message={this.props.message}
                                                   inputs={this.props.inputs}
                                                   streams={this.props.streams}
                                                   nodes={this.props.nodes}
                                                   possiblyHighlight={this.possiblyHighlight}
                                                   showTimestamp={true}/>
                </div>
            </div>
        );
    }
});

module.exports = MessageShow;
