/** @jsx React.DOM */

'use strict';

var React = require('React');

var QuickStartPreview = React.createClass({
    render: function () {
        return (
            <div className="quick-start-preview">
                <h2>Preview:</h2>
                {this.props.children}
            </div>
        );
    }
});

module.exports = QuickStartPreview;