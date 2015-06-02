'use strict';

var React = require('react/addons');
var Output = require('./Output');
var Alert = require('react-bootstrap').Alert;
var Spinner = require('../common/Spinner');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;

var OutputList = React.createClass({
    _sortByTitle(output1, output2) {
        return output1.title.localeCompare(output2.title);
    },
    _formatOutput(output) {
        return (<Output key={output.id} output={output} streamId={this.props.streamId} permissions={this.props.permissions}
                        removeOutputFromStream={this.props.onRemove} removeOutputGlobally={this.props.onTerminate}
                        onUpdate={this.props.onUpdate} getTypeDefinition={this.props.getTypeDefinition} types={this.props.types} />);
    },
    render() {
        if (this.props.outputs) {
            var outputList;
            if (this.props.outputs.length === 0) {
                outputList = (
                    <Row className="content">
                        <Col md={12}>
                            <Alert bsStyle="info">No outputs configured.</Alert>
                        </Col>
                    </Row>
                );
            } else {
                var outputs = this.props.outputs.sort(this._sortByTitle).map(this._formatOutput);
                outputList = (
                    <div>{outputs}</div>
                );
            }

            return outputList;
        } else {
            return <Spinner />;
        }
    }
});

module.exports = OutputList;
