/** @jsx React.DOM */

'use strict';

var React = require('react');
var Markdown = require('markdown').markdown;

var ConfigurationBundlePreview = React.createClass({
    _confirmDeletion() {
       return window.confirm("You are about to delete this content pack, are you sure?");
    },
    render() {
        var preview = "Select an element from the left list to see its preview.";
        var apply_action = "";
        var delete_action = "";

        if (this.props.sourceTypeDescription) {
            preview = this.props.sourceTypeDescription;
            apply_action = <form action={"/a/system/contentpacks/" + this.props.sourceTypeId + "/apply"} method="POST">
                             <input type="submit" value="Apply" className="btn btn-success"/>
                           </form>;
            delete_action = <form action={"/a/system/contentpacks/" + this.props.sourceTypeId + "/delete"} method="POST" onSubmit={this._confirmDeletion}>
                              <input type="submit" value="Delete" className="btn btn-danger"/>
                            </form>;
        }

        var markdownPreview = Markdown.toHTML(preview);

        return (
            <div className="bundle-preview">
                <h2>Preview:</h2>
                <div dangerouslySetInnerHTML={{__html: markdownPreview}} />
                <div className="preview-actions">
                    {apply_action}
                    {delete_action}
                </div>
            </div>
        );
    }
});

module.exports = ConfigurationBundlePreview;
