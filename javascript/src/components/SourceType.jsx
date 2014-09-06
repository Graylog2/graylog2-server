/** @jsx React.DOM */

'use strict';

var React = require('react');

var SourceType = React.createClass({
    _onChange: function(event) {
        this.props.onSelect(event.target.value);
    },
    render: function () {
        return (
            <li>
                <label className="radio">
                    <input type="radio" name="sourceType" id={this.props.name} value={this.props.name} onChange={this._onChange}/>
                    {this.props.description}
                </label>
            </li>
        );
    }
});

module.exports = SourceType;
