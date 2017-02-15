import React from 'react';
import { Alert, Col, Row } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Spinner } from 'components/common/Spinner';
import Output from 'components/outputs/Output';

const OutputList = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string,
    outputs: React.PropTypes.array,
    onRemove: React.PropTypes.func.isRequired,
    onTerminate: React.PropTypes.func.isRequired,
    onUpdate: React.PropTypes.func.isRequired,
    getTypeDefinition: React.PropTypes.func.isRequired,
    types: React.PropTypes.object.isRequired,
  },
  _sortByTitle(output1, output2) {
    return naturalSort(output1.title.toLowerCase(), output2.title.toLowerCase());
  },
  _formatOutput(output) {
    return (
      <Output key={output.id} output={output} streamId={this.props.streamId}
              removeOutputFromStream={this.props.onRemove} removeOutputGlobally={this.props.onTerminate}
              onUpdate={this.props.onUpdate} getTypeDefinition={this.props.getTypeDefinition}
              types={this.props.types} />
    );
  },
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
  },
});

export default OutputList;
