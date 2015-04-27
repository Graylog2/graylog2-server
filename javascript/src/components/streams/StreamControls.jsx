'use strict';

var React = require('react/addons');
var DropdownButton = require('react-bootstrap').DropdownButton;
var MenuItem = require('react-bootstrap').MenuItem;
var ButtonGroup = require('react-bootstrap').ButtonGroup;

var StreamControls = React.createClass({
    getInitialState() {
        return {};
    },
    render() {
        var stream = this.props.stream;
        var user = {'is_readonly' : false};

        //@if(isPermitted(STREAMS_EDIT, stream.getId)) {
        var editStream = <MenuItem><a href={jsRoutes.controllers.StreamsController.edit(stream.id).url}>Edit stream</a></MenuItem>;
        var quickAddRule = <MenuItem className={stream.stream_rules.length > 0 ? "" : "disabled"}><a href="#" data-stream-id={stream.id} className="show-stream-rule">Quick add rule</a></MenuItem>;
        // }

        //@if(isPermitted(STREAMS_CHANGESTATE, stream.getId)) {
        var stateControl = (stream.disabled ?
            <MenuItem><a href={jsRoutes.controllers.StreamsController.resume(stream.id).url}>Start this stream</a></MenuItem> :
            <MenuItem><a href={jsRoutes.controllers.StreamsController.pause(stream.id).url} data-confirm="Really stop stream?">Stop this stream</a></MenuItem>
        );
        //}

        //@if(isPermitted(STREAMS_CREATE) && isPermitted(STREAMS_READ, stream.getId)) {
        var cloneStream = <MenuItem><a href={jsRoutes.controllers.StreamsController.cloneStreamForm(stream.id).url}>Clone this stream</a></MenuItem>;
        //}

        var setAsStartpage = <MenuItem className={user.is_readonly ? "disabled" : ""}>
            <a href={jsRoutes.controllers.StartpageController.set("stream", stream.id).url}>Set as startpage</a>
        </MenuItem>;

        return (
            <ButtonGroup>
                <DropdownButton title='More actions'>
                    {editStream}
                    {quickAddRule}

                    {stateControl}

                    {cloneStream}

                    {setAsStartpage}
                </DropdownButton>
            </ButtonGroup>
        );
    }
});

module.exports = StreamControls;
