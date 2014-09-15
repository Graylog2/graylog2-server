/** @jsx React.DOM */

'use strict';

var React = require('react');

var QuickStartPreview = React.createClass({
    render: function () {
        var preview = "Select an element from the left list to see its preview";
        var action = "";

        if (this.props.sourceTypeDescription) {
            preview = this.props.sourceTypeDescription;
            action = <form action={"/a/system/bundles/" + this.props.sourceTypeId + "/apply"} method="POST">
                        <input type="submit" value="Apply" className="btn btn-success"/>
                     </form>;
        }

        return (
            <div className="quick-start-preview">
                <h2>Preview:</h2>
                <p>{preview}</p>
                {action}
            </div>
        );
    }
});

module.exports = QuickStartPreview;
