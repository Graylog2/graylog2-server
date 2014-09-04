/** @jsx React.DOM */

'use strict';

var React = require('React');

var SourceType = React.createClass({
    render: function () {
        return (
            <li>
                <label className="radio">
                    <input type="radio" name="sourceType" id={this.props.name} value={this.props.name}/>
                    {this.props.description}
                </label>
            </li>
        );
    }
});

module.exports = SourceType;