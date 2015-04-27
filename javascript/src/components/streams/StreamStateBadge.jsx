'use strict';

var React = require('react/addons');

var StreamStateBadge = React.createClass({
    render() {
        return (this.props.stream.disabled ? <span className="badge alert-danger stream-stopped" onClick={this.props.onClick}>stopped</span> : <div></div>);
    }
});

module.exports = StreamStateBadge;
