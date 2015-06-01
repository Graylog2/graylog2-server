'use strict';

var React = require('react/addons');
var EditOutputButton = require('./EditOutputButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var Button = require('react-bootstrap').Button;
var PermissionsMixin = require('../../util/PermissionsMixin');
var Col = require('react-bootstrap').Col;
var Row = require('react-bootstrap').Row;

var Output = React.createClass({
    mixins: [PermissionsMixin],
    _deleteFromStreamButton(output) {
        return (
            <Button bsStyle="info" onClick={this.props.removeOutputFromStream.bind(null, output.id, this.props.streamId)}>
                Delete from stream
            </Button>
        );
    },
    _deleteGloballyButton(output) {
        return (
            <Button bsStyle="primary" onClick={this.props.removeOutputGlobally.bind(null, output.id)}>
                Delete globally
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
                <Col md={12}>
                    <Row className="row-sm">
                        <Col md={6}>
                            <h2 className="extractor-title">
                                {output.title} {contentPack}
                                <small>ID: {output.id}</small>
                            </h2>
                            Type: {output.type}
                        </Col>
                        <Col md={6}>
                            <span className="pull-right node-row-info">
                                {editButton}
                                {' '}
                                {deleteButton}
                                {' '}
                                {terminateButton}
                            </span>
                        </Col>
                    </Row>
                    <Row>
                        <Col md={8}>
                            <ConfigurationWell key={"configuration-well-output-" + output.id} id={output.id} configuration={output.configuration} />
                        </Col>
                    </Row>
                </Col>
            </div>
        );
    }
});
module.exports = Output;
