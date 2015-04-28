'use strict';

var React = require('react/addons');

var StreamStateBadge = React.createClass({
    _onClick(evt) {
        this.props.onClick(this.props.stream);
    },
    render() {
        var stream = this.props.stream;
        return (stream.disabled ? <span className="badge alert-danger stream-stopped" onClick={this._onClick}>stopped</span> : <div></div>);
    }
});

module.exports = StreamStateBadge;
