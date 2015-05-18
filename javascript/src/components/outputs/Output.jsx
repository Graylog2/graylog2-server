'use strict';

var React = require('react/addons');
var EditOutputButton = require('./EditOutputButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var UserLink = require('../users/UserLink');

var Output = React.createClass({
    _isPermitted(permissions) {
        var result = permissions.every((p) => this.props.permissions[p]);
        return result;
    },
    _deleteFromStreamButton(output) {
        return (
            <button className="btn btn-warning btn-xs"
                    onClick={this.props.removeOutputFromStream.bind(null, output.id, this.props.streamId)}>
                <i className="fa fa-remove"></i> Delete from stream
            </button>
        );
    },
    _deleteGloballyButton(output) {
        return (
            <button className="btn btn-danger btn-xs" onClick={this.props.removeOutputGlobally.bind(null, output.id)}
                    data-confirm={"Really delete output " + output.title + " globally? It will be removed from all streams in the system."}>
                <i className="fa fa-remove"></i> Delete globally
            </button>
        );
    },
    render() {
        var output = this.props.output;
        var deletionForm = (this.props.streamId && this._isPermitted(["STREAM_OUTPUTS_DELETE"]) ? this._deleteFromStreamButton(output) : "");

        var editButton = (this._isPermitted(["OUTPUTS_EDIT"]) ?
            <EditOutputButton output={output} onUpdate={this.props.onUpdate} getTypeDefinition={this.props.getTypeDefinition} /> : "");
        var terminationForm = (this._isPermitted(["OUTPUTS_TERMINATE"]) ? this._deleteGloballyButton(output) : "");

        var contentPack = (output.content_pack ? (<span title="Created from content pack"><i className="fa fa-gift"></i></span>) : (<div></div>));

        var creatorUserLink = (<UserLink username={output.creator_user_id} />);

        return (
            <div key={output.id} className="row content node-row">
                <div className="col-md-12">
                    <span className="pull-right node-row-info">
                        <span className="text">Started by {creatorUserLink}</span>

                        <span className="text" title={moment(output.created_at).format()}>{moment(output.created_at).fromNow()}</span>
                        &nbsp;
                        {editButton}
                        {deletionForm}
                        {terminationForm}
                    </span>
                    <h3>
                        <i className="fa fa-ellipsis-v"></i> {output.title} ({output.type})
                        {contentPack}
                    </h3>
                    <ConfigurationWell key={"configuration-well-output-" + output.id} id={output.id} configuration={output.configuration} />
                </div>
            </div>
        );
    }
});
module.exports = Output;
