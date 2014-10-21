/** @jsx React.DOM */

'use strict';

var React = require('react');
var Markdown = require('markdown').markdown;

var ConfigurationBundlePreview = React.createClass({
    render: function () {
        var preview = "Select an element from the left list to see its preview";
        var action = "";

        if (this.props.sourceTypeDescription) {
            preview = this.props.sourceTypeDescription;
            action = <form action={"/a/system/contentpacks/" + this.props.sourceTypeId + "/apply"} method="POST">
                        <input type="submit" value="Apply" className="btn btn-success"/>
                     </form>;
        }

        return (
            <div className="bundle-preview">
                <h2>Preview:</h2>
                <p dangerouslySetInnerHTML={{__html: Markdown.toHTML(preview)}}></p>
                {action}
            </div>
        );
    }
});

module.exports = ConfigurationBundlePreview;
