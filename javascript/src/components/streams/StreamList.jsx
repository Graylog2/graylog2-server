'use strict';

var React = require('react/addons');
var Stream = require('./Stream');
var Alert = require('react-bootstrap').Alert;
var PermissionsMixin = require('../../util/PermissionsMixin');

var StreamList = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {};
    },
    _formatStream(stream) {
        return <Stream key={"stream-" + stream.id} stream={stream} streamRuleTypes={this.props.streamRuleTypes}
                       permissions={this.props.permissions} user={this.props.user}
                       onDelete={this.props.onDelete} onResume={this.props.onResume} onPause={this.props.onPause}
                       onUpdate={this.props.onUpdate} onClone={this.props.onClone}/>;
    },
    _sortByTitle(stream1, stream2) {
        return stream1.title.localeCompare(stream2.title);
    },
    render() {
        if (this.props.streams.length > 0) {
            var streamList = this.props.streams.sort(this._sortByTitle).map(this._formatStream);

            return (
                <ul className="streams">
                    {streamList}
                </ul>
            );
        } else {
            var createLink = (this.isPermitted(this.props.permissions, ['streams:create']) ? <a onClick={this.props.onCreate}>Create one now</a> : "");
            return (
                <Alert bsStyle='warning'>
                    <i className="fa fa-info-circle"></i>&nbsp;
                    No streams configured. {createLink}
                </Alert>
            );
        }
    }
});

module.exports = StreamList;
