/** @jsx React.DOM */

'use strict';

var React = require('react');

var SourceType = React.createClass({
    _onChange(event) {
        this.props.onSelect(event.target.id, event.target.value);
    },
    render() {
        return (
            <label className="radio">
                <input type="radio" name="sourceType" id={this.props.id} value={this.props.description} onChange={this._onChange}/>
                {this.props.name}
            </label>
        );
    }
});

module.exports = SourceType;
