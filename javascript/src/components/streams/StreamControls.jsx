'use strict';

var React = require('react/addons');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;
var StreamForm = require('./StreamForm');

var StreamControls = React.createClass({
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
    render() {
        var stream = this.props.stream;
        // TODO: replace with real user
        var user = {'is_readonly' : false};

        //@if(isPermitted(STREAMS_EDIT, stream.getId)) {
        var editStream = <MenuItem><a onClick={this._onEdit}>Edit stream</a></MenuItem>;
        var quickAddRule = <MenuItem className={stream.stream_rules.length > 0 ? "" : "disabled"}><a href="#" data-stream-id={stream.id} className="show-stream-rule">Quick add rule</a></MenuItem>;
        // }

        //@if(isPermitted(STREAMS_CHANGESTATE, stream.getId)) {
        var stateControl = (stream.disabled ?
            <MenuItem><a onClick={this._onResume}>Start this stream</a></MenuItem> :
            <MenuItem><a onClick={this._onPause}>Stop this stream</a></MenuItem>
        );
        //}

        //@if(isPermitted(STREAMS_CREATE) && isPermitted(STREAMS_READ, stream.getId)) {
        var cloneStream = <MenuItem><a onClick={this._onClone}>Clone this stream</a></MenuItem>;
        //}

        var setAsStartpage = <MenuItem className={user.is_readonly ? "disabled" : ""}>
            <a href={jsRoutes.controllers.StartpageController.set("stream", stream.id).url}>Set as startpage</a>
        </MenuItem>;

        return (
            <ButtonGroup>
                <DropdownButton title='More actions' ref='dropdownButton'>
                    {editStream}
                    {quickAddRule}

                    {stateControl}

                    {cloneStream}

                    {setAsStartpage}
                </DropdownButton>
                <StreamForm ref='streamForm' title="Editing Stream" onSubmit={this.props.onUpdate} stream={stream}/>
                <StreamForm ref='cloneForm' title="Cloning Stream" onSubmit={this._onCloneSubmit}/>
            </ButtonGroup>
        );
    }
});

module.exports = StreamControls;
