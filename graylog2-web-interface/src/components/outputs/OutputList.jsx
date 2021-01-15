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
import naturalSort from 'javascript-natural-sort';

import { Alert, Col, Row } from 'components/graylog';
import { Spinner } from 'components/common';
import Output from 'components/outputs/Output';

class OutputList extends React.Component {
  static propTypes = {
    streamId: PropTypes.string,
    outputs: PropTypes.array,
    onRemove: PropTypes.func.isRequired,
    onTerminate: PropTypes.func.isRequired,
    onUpdate: PropTypes.func.isRequired,
    getTypeDefinition: PropTypes.func.isRequired,
    types: PropTypes.object.isRequired,
  };

  _sortByTitle = (output1, output2) => {
    return naturalSort(output1.title.toLowerCase(), output2.title.toLowerCase());
  };

  _formatOutput = (output) => {
    return (
      <Output key={output.id}
              output={output}
              streamId={this.props.streamId}
              removeOutputFromStream={this.props.onRemove}
              removeOutputGlobally={this.props.onTerminate}
              onUpdate={this.props.onUpdate}
              getTypeDefinition={this.props.getTypeDefinition}
              types={this.props.types} />
    );
  };

  render() {
    if (!this.props.outputs) {
      return <Spinner />;
    }

    if (this.props.outputs.length === 0) {
      return (
        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="info">No outputs configured.</Alert>
          </Col>
        </Row>
      );
    }

    const outputs = this.props.outputs.sort(this._sortByTitle).map(this._formatOutput);

    return <div>{outputs}</div>;
  }
}

export default OutputList;
