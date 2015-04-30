'use strict';

var React = require('react/addons');
var StreamThroughput = require('./StreamThroughput');
var StreamControls = require('./StreamControls');
var StreamStateBadge = require('./StreamStateBadge');
var CollapsableStreamList = require('./CollapsableStreamList');

var Stream = React.createClass({
    _formatNumberOfStreamRules(stream) {
        return (stream.stream_rules.length > 0 ? stream.stream_rules.length + " configured stream rule(s)." : "no configured rules.");
    },
    _handleDelete() {
        this.props.onDelete(this.props.stream);
    },
    render() {
        var stream = this.props.stream;
        // @if(isPermitted(STREAMS_EDIT, stream.getId)) {
        var editRulesLink = <a href={jsRoutes.controllers.StreamRulesController.index(stream.id).url} className="btn btn-info">Edit rules</a>;
        // @if(isPermitted(STREAMS_EDIT, stream.getId) && isPermitted(STREAM_OUTPUTS_READ)) {
        var manageOutputsLink = <a href={jsRoutes.controllers.StreamOutputsController.index(stream.id).url} className="btn btn-info">Manage outputs</a>;
        var manageAlertsLink = <a href={jsRoutes.controllers.AlertsController.index(stream.id).url} className="btn btn-info">Manage alerts</a>;
        // @if(isPermitted(STREAMS_EDIT, stream.getId)) {
        var deleteStreamLink = <a className="btn btn-danger" onClick={this._handleDelete}>
            <i className="fa fa-trash"></i>
        </a>;

        var createdFromContentPack = (stream.content_pack ? <i className="fa fa-cube" title="Created from content pack"></i> : "");

        return (
            <li className="stream">
                <h2>
                    <a href={jsRoutes.controllers.StreamSearchController.index(stream.id, "*", "relative", 300).url}>{stream.title}</a>

                    <StreamStateBadge stream={stream} onClick={this.props.onResume}/>
                </h2>
                <div className="stream-data">
                    <div className="stream-actions pull-right">
                        {editRulesLink}
                        {manageOutputsLink}
                        {manageAlertsLink}
                        {deleteStreamLink}

                        <StreamControls stream={stream}
                                        onResume={this.props.onResume} onUpdate={this.props.onUpdate} onClone={this.props.onClone}/>
                    </div>
                    <div className="stream-description">
                        {createdFromContentPack}

                        {stream.description}
                   </div>
                    <div className="stream-metadata">
                        <StreamThroughput streamId={stream.id} />

                        , {this._formatNumberOfStreamRules(stream)}

                        <CollapsableStreamList streamRules={stream.stream_rules}/>
                    </div>
                </div>
            </li>
        );
    }
});

module.exports = Stream;
