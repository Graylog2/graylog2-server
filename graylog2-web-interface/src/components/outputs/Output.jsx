import React from 'react';
import { Alert, Button, Col, Row } from 'react-bootstrap';

import EditOutputButton from 'components/outputs/EditOutputButton';
import { ConfigurationWell } from 'components/configurationforms';
import { IfPermitted, Spinner } from 'components/common';

const Output = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string,
    output: React.PropTypes.object.isRequired,
    types: React.PropTypes.object.isRequired,
    getTypeDefinition: React.PropTypes.func.isRequired,
    removeOutputFromStream: React.PropTypes.func.isRequired,
    removeOutputGlobally: React.PropTypes.func.isRequired,
  },
  getInitialState() {
    return {};
  },
  componentDidMount() {
    if (!this._typeNotAvailable()) {
      this.props.getTypeDefinition(this.props.output.type, (typeDefinition) => {
        this.setState({ typeDefinition: typeDefinition });
      });
    }
  },
  _onDeleteFromStream() {
    this.props.removeOutputFromStream(this.props.output.id, this.props.streamId);
  },
  _onDeleteGlobally() {
    this.props.removeOutputGlobally(this.props.output.id);
  },
  _typeNotAvailable() {
    return (this.props.types[this.props.output.type] === undefined);
  },
  render() {
    if (!this._typeNotAvailable() && !this.state.typeDefinition) {
      return <Spinner />;
    }

    const output = this.props.output;
    const contentPack = (output.content_pack ?
      <span title="Created from content pack"><i className="fa fa-gift" /></span> : null);

    let alert;
    let configurationWell;
    if (this._typeNotAvailable()) {
      alert = (
        <Alert bsStyle="danger">
          The plugin required for this output is not loaded. Editing it is not possible. Please load the plugin or
          delete the output.
        </Alert>
      );
    } else {
      configurationWell = (
        <ConfigurationWell key={`configuration-well-output-${output.id}`}
                           id={output.id} configuration={output.configuration}
                           typeDefinition={this.state.typeDefinition} />
      );
    }

    const streamId = this.props.streamId;
    let deleteFromStreamButton;
    if (streamId !== null && streamId !== undefined) {
      deleteFromStreamButton = (
        <IfPermitted permissions="stream_outputs:delete">
          {' '}
          <Button bsStyle="info" onClick={this._onDeleteFromStream}>
            Delete from stream
          </Button>
        </IfPermitted>
      );
    } else {
      deleteFromStreamButton = '';
    }

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
              <div className="text-right node-row-info">
                <IfPermitted permissions="outputs:edit">
                  <EditOutputButton disabled={this._typeNotAvailable()} output={output} onUpdate={this.props.onUpdate}
                                    getTypeDefinition={this.props.getTypeDefinition} />
                </IfPermitted>
                {deleteFromStreamButton}
                <IfPermitted permissions="outputs:terminate">
                  {' '}
                  <Button bsStyle="primary" onClick={this._onDeleteGlobally}>
                    Delete globally
                  </Button>
                </IfPermitted>
              </div>
            </Col>
          </Row>
          <Row>
            <Col md={8}>
              {alert}
              {configurationWell}
            </Col>
          </Row>
        </Col>
      </div>
    );
  },
});

export default Output;
