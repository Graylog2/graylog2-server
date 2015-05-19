/* global jsRoutes */

'use strict';

var React = require('react/addons');
var StreamThroughput = require('./StreamThroughput');
var StreamControls = require('./StreamControls');
var StreamStateBadge = require('./StreamStateBadge');
var CollapsableStreamRuleList = require('../streamrules/CollapsableStreamRuleList');
var PermissionsMixin = require('../../util/PermissionsMixin');
var StreamsStore = require('../../stores/streams/StreamsStore');
var StreamRulesStore = require('../../stores/streams/StreamRulesStore');
var StreamRuleForm = require('../streamrules/StreamRuleForm');

var Stream = React.createClass({
    mixins: [PermissionsMixin],
    _formatNumberOfStreamRules(stream) {
        return (stream.stream_rules.length > 0 ? stream.stream_rules.length + " configured stream rule(s)." : "no configured rules.");
    },
    _onDelete(stream) {
        if (window.confirm("Do you really want to remove this stream?")) {
            StreamsStore.remove(stream.id, () => {});
        }
    },
    _onResume(evt) {
        StreamsStore.resume(this.props.stream.id, () => {});
    },
    _onUpdate(streamId, stream) {
        StreamsStore.update(streamId, stream, () => {});
    },
    _onClone(streamId, stream) {
        StreamsStore.cloneStream(streamId, stream, () => {});
    },
    _onPause(evt) {
        if (window.confirm("Do you really want to pause stream \"" + this.props.stream.title + "\"?")) {
            StreamsStore.pause(this.props.stream.id, () => {});
        }
    },
    _onQuickAdd() {
        this.refs.quickAddStreamRuleForm.open();
    },
    _onSaveStreamRule(streamRuleId, streamRule) {
        StreamRulesStore.create(this.props.stream.id, streamRule, () => {});
    },
    render() {
        var stream = this.props.stream;
        var permissions = this.props.permissions;
        var editRulesLink = (this.isPermitted(permissions, ['streams:edit:'+stream.id]) ? <a href={jsRoutes.controllers.StreamRulesController.index(stream.id).url} className="btn btn-info">Edit rules</a> : null);

        var manageOutputsLink = null;
        var manageAlertsLink = null;
        if (this.isPermitted(permissions, ['streams:edit:'+stream.id, 'stream_outputs:read'])) {
            manageOutputsLink = <a href={jsRoutes.controllers.StreamOutputsController.index(stream.id).url}
                                       className="btn btn-info">Manage outputs</a>;
            manageAlertsLink = <a href={jsRoutes.controllers.AlertsController.index(stream.id).url}
                                      className="btn btn-info">Manage alerts</a>;
        }

        var toggleStreamLink = null;
        if (this.isPermitted(permissions, ["streams:changestate:" + stream.id])) {
            if (stream.disabled) {
                toggleStreamLink = (<a className="btn btn-success toggle-stream-button" onClick={this._onResume}>Start stream</a>);
            } else {
                toggleStreamLink = (<a className="btn btn-primary toggle-stream-button" onClick={this._onPause}>Pause stream</a>);
            }
        }

        var createdFromContentPack = (stream.content_pack ? <i className="fa fa-cube" title="Created from content pack"></i> : null);

        return (
            <li className="stream">
                <h2>
                    <a href={jsRoutes.controllers.StreamSearchController.index(stream.id, "*", "relative", 300).url}>{stream.title}</a>

                    <StreamStateBadge stream={stream} onClick={this.props.onResume}/>
                </h2>
                <div className="stream-data">
                    <div className="stream-actions pull-right">
                        {editRulesLink}{' '}
                        {manageOutputsLink}{' '}
                        {manageAlertsLink}{' '}
                        {toggleStreamLink}{' '}

                        <StreamControls stream={stream} permissions={this.props.permissions} user={this.props.user}
                                        onDelete={this._onDelete} onUpdate={this._onUpdate} onClone={this._onClone}
                                        onQuickAdd={this._onQuickAdd}/>
                    </div>
                    <div className="stream-description">
                        {createdFromContentPack}

                        {stream.description}
                   </div>
                    <div className="stream-metadata">
                        <StreamThroughput streamId={stream.id} />

                        , {this._formatNumberOfStreamRules(stream)}

                        <CollapsableStreamRuleList key={'streamRules-'+stream.id} stream={stream} streamRuleTypes={this.props.streamRuleTypes}
                                                   permissions={this.props.permissions}/>
                    </div>
                </div>
                <StreamRuleForm ref="quickAddStreamRuleForm" title="New Stream Rule" onSubmit={this._onSaveStreamRule} streamRuleTypes={this.props.streamRuleTypes}/>
            </li>
        );
    }
});

module.exports = Stream;
