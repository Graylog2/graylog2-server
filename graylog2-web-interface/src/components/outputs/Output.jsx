import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert, Col, Row, Button } from 'components/graylog';
import EditOutputButton from 'components/outputs/EditOutputButton';
import { ConfigurationWell } from 'components/configurationforms';
import { IfPermitted, Spinner, Icon } from 'components/common';

const NodeRow = styled.div`
  border-bottom: 1px solid #ccc;
  padding-bottom: 8px;
  margin-bottom: 8px;
  margin-top: 0;

  .hostname {
    font-size: 12px;
  }

  .well {
    margin-bottom: 0;
    margin-top: 3px;
    font-family: monospace;
    font-size: 11px;
  }

  .xtrc-converters {
    margin-top: 10px;
  }

  .xtrc-config li {
    margin-left: 10px;
  }

  .xtrc-converters li {
    margin-left: 10px;
  }

  .xtrc-converter-config li {
    margin-left: 20px;
  }

  .dropdown-menu a.selected {
    font-weight: bold;
  }
`;

const NodeRowInfo = styled.div`
  position: relative;
  top: 2px;

  form {
    display: inline;
  }

  .text {
    position: relative;
    top: 3px;
  }
`;

class Output extends React.Component {
  static propTypes = {
    streamId: PropTypes.string,
    output: PropTypes.object.isRequired,
    types: PropTypes.object.isRequired,
    getTypeDefinition: PropTypes.func.isRequired,
    removeOutputFromStream: PropTypes.func.isRequired,
    removeOutputGlobally: PropTypes.func.isRequired,
  };

  state = {};

  componentDidMount() {
    if (!this._typeNotAvailable()) {
      this.props.getTypeDefinition(this.props.output.type, (typeDefinition) => {
        this.setState({ typeDefinition: typeDefinition });
      });
    }
  }

  _onDeleteFromStream = () => {
    this.props.removeOutputFromStream(this.props.output.id, this.props.streamId);
  };

  _onDeleteGlobally = () => {
    this.props.removeOutputGlobally(this.props.output.id);
  };

  _typeNotAvailable = () => {
    return (this.props.types[this.props.output.type] === undefined);
  };

  render() {
    if (!this._typeNotAvailable() && !this.state.typeDefinition) {
      return <Spinner />;
    }

    const { output } = this.props;
    const contentPack = (output.content_pack
      ? <span title="Created from content pack"><Icon name="gift" /></span> : null);

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
                           id={output.id}
                           configuration={output.configuration}
                           typeDefinition={this.state.typeDefinition} />
      );
    }

    const { streamId } = this.props;
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
      <NodeRow key={output.id} className="row content">
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
              <NodeRowInfo className="text-right">
                <IfPermitted permissions="outputs:edit">
                  <EditOutputButton disabled={this._typeNotAvailable()}
                                    output={output}
                                    onUpdate={this.props.onUpdate}
                                    getTypeDefinition={this.props.getTypeDefinition} />
                </IfPermitted>
                {deleteFromStreamButton}
                <IfPermitted permissions="outputs:terminate">
                  {' '}
                  <Button bsStyle="primary" onClick={this._onDeleteGlobally}>
                    Delete globally
                  </Button>
                </IfPermitted>
              </NodeRowInfo>
            </Col>
          </Row>
          <Row>
            <Col md={8}>
              {alert}
              {configurationWell}
            </Col>
          </Row>
        </Col>
      </NodeRow>
    );
  }
}

export default Output;
