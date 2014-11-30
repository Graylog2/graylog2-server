'use strict';

var React = require('react');
var Markdown = require('markdown').markdown;

var ConfigurationBundlePreview = React.createClass({
    _confirmDeletion() {
       return window.confirm("You are about to delete this content pack, are you sure?");
    },
    render() {
        var preview = "Select a content pack from the list to see its preview.";
        var apply_action = "";
        var delete_action = "";

        if (this.props.sourceTypeDescription) {
            preview = this.props.sourceTypeDescription;
            apply_action = <form action={"/a/system/contentpacks/" + this.props.sourceTypeId + "/apply"} method="POST">
                             <input type="submit" value="Apply content" className="btn btn-small btn-success"/>
                           </form>;
            delete_action = <form action={"/a/system/contentpacks/" + this.props.sourceTypeId + "/delete"} method="POST" onSubmit={this._confirmDeletion} className="pull-right">
                              <input type="submit" value="Remove pack" className="btn btn-mini btn-warning"/>
                            </form>;
        }

        var markdownPreview = Markdown.toHTML(preview);
        return (
            <div className="bundle-preview">
                {delete_action}
                <h2>Content pack description:</h2>
                <div dangerouslySetInnerHTML={{__html: markdownPreview}} />
                <div className="preview-actions">
                    {apply_action}
                </div>
            </div>
        );
    }
});

module.exports = ConfigurationBundlePreview;
