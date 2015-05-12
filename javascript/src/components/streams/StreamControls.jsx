/* global jsRoutes */

'use strict';

var React = require('react/addons');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var StreamForm = require('./StreamForm');
var PermissionsMixin = require('../../util/PermissionsMixin');

var StreamControls = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {};
    },
    _onResume(evt) {
        this.props.onResume(this.props.stream);
        this.refs.dropdownButton.setDropdownState(false);
    },
    _onPause(evt) {
        this.props.onPause(this.props.stream);
        this.refs.dropdownButton.setDropdownState(false);
    },
    _onEdit(evt) {
        this.refs.streamForm.open();
    },
    _onClone(evt) {
        this.refs.cloneForm.open();
    },
    _onCloneSubmit(streamId, stream) {
        this.props.onClone(this.props.stream.id, stream);
    },
    _onQuickAdd() {
        this.props.onQuickAdd(this.props.stream.id);
    },
    render() {
        var permissions = this.props.permissions;
        var stream = this.props.stream;
        // TODO: replace with real user
        var user = {'is_readonly': false};

        var menuItems = [];

        if (this.isPermitted(permissions, ['streams:edit:' + stream.id])) {
            menuItems.push(<MenuItem key={"editStreams-" + stream.id}><a onClick={this._onEdit}>Edit stream</a></MenuItem>);
            menuItems.push(<MenuItem  key={"quickAddRule-" + stream.id}className={stream.stream_rules.length > 0 ? "" : "disabled"}>
                <a href="#" onClick={this._onQuickAdd}>Quick add rule</a>
            </MenuItem>);
        }

        if (this.isPermitted(permissions, ["streams:changestate:" + stream.id])) {
            if (stream.disabled) {
                menuItems.push(<MenuItem key={"startStream-" + stream.id}><a onClick={this._onResume}>Start this stream</a></MenuItem>);
            } else {
                menuItems.push(<MenuItem key={"stopStream-" + stream.id}><a onClick={this._onPause}>Stop this stream</a></MenuItem>);
            }
        }

        if (this.isPermitted(permissions, ["streams:create", "streams:read:" + stream.id])) {
            menuItems.push(<MenuItem key={"cloneStream-" + stream.id}><a onClick={this._onClone}>Clone this stream</a></MenuItem>);
        }

        menuItems.push(<MenuItem key={"setAsStartpage-" + stream.id} className={user.is_readonly ? "disabled" : ""}>
            <a href={jsRoutes.controllers.StartpageController.set("stream", stream.id).url}>Set as startpage</a>
        </MenuItem>);

        return (
            <ButtonGroup>
                <DropdownButton title='More actions' ref='dropdownButton'>
                    {menuItems}
                </DropdownButton>
                <StreamForm ref='streamForm' title="Editing Stream" onSubmit={this.props.onUpdate} stream={stream}/>
                <StreamForm ref='cloneForm' title="Cloning Stream" onSubmit={this._onCloneSubmit}/>
            </ButtonGroup>
        );
    }
});

module.exports = StreamControls;
