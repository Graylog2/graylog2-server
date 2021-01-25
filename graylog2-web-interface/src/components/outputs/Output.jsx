/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { Alert, Col, Row, Button } from 'components/graylog';
import EditOutputButton from 'components/outputs/EditOutputButton';
import { ConfigurationWell } from 'components/configurationforms';
import { IfPermitted, Spinner, Icon } from 'components/common';

const NodeRow = styled.div(({ theme }) => css`
  border-bottom: 1px solid ${theme.colors.gray[80]};
  padding-bottom: 8px;
  margin-bottom: 8px;
  margin-top: 0;

  .hostname {
    font-size: ${theme.fonts.size.small};
  }

  .well {
    margin-bottom: 0;
    margin-top: 3px;
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
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
`);

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
    onUpdate: PropTypes.func,
    removeOutputFromStream: PropTypes.func.isRequired,
    removeOutputGlobally: PropTypes.func.isRequired,
  };

  static defaultProps = {
    streamId: '',
    onUpdate: () => {},
  }

  constructor(props) {
    super(props);

    this.state = {
      typeDefinition: undefined,
    };
  }

  componentDidMount() {
    const { getTypeDefinition, output } = this.props;

    if (!this._typeNotAvailable()) {
      getTypeDefinition(output.type, (typeDefinition) => {
        this.setState({ typeDefinition });
      });
    }
  }

  _onDeleteFromStream = () => {
    const { removeOutputFromStream, output, streamId } = this.props;

    removeOutputFromStream(output.id, streamId);
  };

  _onDeleteGlobally = () => {
    const { removeOutputGlobally, output } = this.props;

    removeOutputGlobally(output.id);
  };

  _typeNotAvailable = () => {
    const { types, output } = this.props;

    return (types[output.type] === undefined);
  };

  render() {
    const { typeDefinition } = this.state;
    const { onUpdate, getTypeDefinition } = this.props;

    if (!this._typeNotAvailable() && !typeDefinition) {
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
                           typeDefinition={typeDefinition} />
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
                                    onUpdate={onUpdate}
                                    getTypeDefinition={getTypeDefinition} />
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
