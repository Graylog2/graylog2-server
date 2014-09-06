/** @jsx React.DOM */

'use strict';

var React = require('react');

var QuickStartPreview = React.createClass({
    render: function () {
        var preview = "Select an element from the left list to see its preview";
        if (this.props.sourceType) {
            preview = this.props.sourceType;
        }

        return (
            <div className="quick-start-preview">
                <h2>Preview:</h2>
                {preview}
            </div>
        );
    }
});

module.exports = QuickStartPreview;
