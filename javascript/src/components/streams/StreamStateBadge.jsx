'use strict';

var React = require('react');

var StreamStateBadge = React.createClass({
    _onClick(evt) {
        if (typeof this.props.onClick === 'function') {
            this.props.onClick(this.props.stream);
        }
    },
    render() {
        var stream = this.props.stream;
        return (stream.disabled ? <span className="badge alert-danger stream-stopped" onClick={this._onClick} style={{marginLeft: 5}}>stopped</span> : null);
    }
});

module.exports = StreamStateBadge;
