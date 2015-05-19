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
    _onDelete(evt) {
        this.props.onDelete(this.props.stream);
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

        var menuItems = [];

        if (this.isPermitted(permissions, ['streams:edit:' + stream.id])) {
            menuItems.push(<MenuItem key={"editStreams-" + stream.id} onClick={this._onEdit}>Edit stream</MenuItem>);
            menuItems.push(<MenuItem key={"quickAddRule-" + stream.id} onClick={this._onQuickAdd}>Quick add rule</MenuItem>);
        }

        if (this.isPermitted(permissions, ["streams:create", "streams:read:" + stream.id])) {
            menuItems.push(<MenuItem key={"cloneStream-" + stream.id} onClick={this._onClone}>Clone this stream</MenuItem>);
        }

        if (this.props.user) {
            menuItems.push(<MenuItem key={"setAsStartpage-" + stream.id} className={this.props.user.readonly ? "disabled" : ""}
                                     href={this.props.user.readonly ? null : jsRoutes.controllers.StartpageController.set("stream", stream.id).url}>
                Set as startpage
            </MenuItem>);
        }

        if (this.isPermitted(permissions, ['streams:edit:' + stream.id])) {
            menuItems.push(<MenuItem key={'divider-' + stream.id} divider />);
            menuItems.push(<MenuItem key={'deleteStream-' + stream.id} onClick={this._onDelete}>Delete this stream</MenuItem>);
        }

        return (
            <ButtonGroup>
                <DropdownButton title='More actions' ref='dropdownButton' pullRight={true}>
                    {menuItems}
                </DropdownButton>
                <StreamForm ref='streamForm' title="Editing Stream" onSubmit={this.props.onUpdate} stream={stream}/>
                <StreamForm ref='cloneForm' title="Cloning Stream" onSubmit={this._onCloneSubmit}/>
            </ButtonGroup>
        );
    }
});

module.exports = StreamControls;
