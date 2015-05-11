/* global jsRoutes */

'use strict';

var React = require('react/addons');
var StreamThroughput = require('./StreamThroughput');
var StreamControls = require('./StreamControls');
var StreamStateBadge = require('./StreamStateBadge');
var CollapsableStreamRuleList = require('../streamrules/CollapsableStreamRuleList');
var PermissionsMixin = require('../../util/PermissionsMixin');

var Stream = React.createClass({
    mixins: [PermissionsMixin],
    _formatNumberOfStreamRules(stream) {
        return (stream.stream_rules.length > 0 ? stream.stream_rules.length + " configured stream rule(s)." : "no configured rules.");
    },
    _handleDelete() {
        this.props.onDelete(this.props.stream);
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

        var deleteStreamLink = (this.isPermitted(permissions, ['streams:edit:'+stream.id]) ? <a className="btn btn-danger" onClick={this._handleDelete}>
            <i className="fa fa-trash"></i>
        </a> : null);

        var createdFromContentPack = (stream.content_pack ? <i className="fa fa-cube" title="Created from content pack"></i> : null);

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

                        <StreamControls stream={stream} permissions={this.props.permissions}
                                        onResume={this.props.onResume} onUpdate={this.props.onUpdate}
                                        onPause={this.props.onPause} onClone={this.props.onClone}/>
                    </div>
                    <div className="stream-description">
                        {createdFromContentPack}

                        {stream.description}
                   </div>
                    <div className="stream-metadata">
                        <StreamThroughput streamId={stream.id} />

                        , {this._formatNumberOfStreamRules(stream)}

                        <CollapsableStreamRuleList key={'streamRules-'+stream.id} stream={stream} permissions={this.props.permissions}/>
                    </div>
                </div>
            </li>
        );
    }
});

module.exports = Stream;
