'use strict';

var React = require('react/addons');
var EditOutputButton = require('./EditOutputButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var Button = require('react-bootstrap').Button;
var PermissionsMixin = require('../../util/PermissionsMixin');

var Output = React.createClass({
    mixins: [PermissionsMixin],
    _deleteFromStreamButton(output) {
        return (
            <Button className="btn btn-warning btn-xs"
                    onClick={this.props.removeOutputFromStream.bind(null, output.id, this.props.streamId)}>
                <i className="fa fa-remove"></i> Delete from stream
            </Button>
        );
    },
    _deleteGloballyButton(output) {
        return (
            <Button className="btn btn-danger btn-xs" onClick={this.props.removeOutputGlobally.bind(null, output.id)}>
                <i className="fa fa-remove"></i> Delete globally
            </Button>
        );
    },
    render() {
        var output = this.props.output;
        var deleteButton = (this.props.streamId && this.isPermitted(this.props.permissions, ["stream_outputs:delete"]) ? this._deleteFromStreamButton(output) : null);

        var editButton = (this.isPermitted(this.props.permissions, ["outputs:edit"]) ?
            <EditOutputButton output={output} onUpdate={this.props.onUpdate} getTypeDefinition={this.props.getTypeDefinition} /> : null);
        var terminateButton = (this.isPermitted(this.props.permissions, ["outputs:terminate"]) ? this._deleteGloballyButton(output) : null);

        var contentPack = (output.content_pack ? (<span title="Created from content pack"><i className="fa fa-gift"></i></span>) : null);

        return (
            <div key={output.id} className="row content node-row">
                <div className="col-md-12">
                    <span className="pull-right node-row-info">
                        {editButton}
                        {' '}
                        {deleteButton}
                        {' '}
                        {terminateButton}
                    </span>
                    <h3>
                        <i className="fa fa-ellipsis-v"></i> {output.title} ({output.type})
                        {contentPack}
                    </h3>
                    &nbsp;
                    <ConfigurationWell key={"configuration-well-output-" + output.id} id={output.id} configuration={output.configuration} />
                </div>
            </div>
        );
    }
});
module.exports = Output;
