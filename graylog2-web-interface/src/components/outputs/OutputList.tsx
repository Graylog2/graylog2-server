import React from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Col, Row } from 'components/bootstrap';
import { Spinner, NoEntitiesExist } from 'components/common';
import Output from 'components/outputs/Output';

const sortByTitle = (output1, output2) => naturalSort(output1.title.toLowerCase(), output2.title.toLowerCase());

type OutputListProps = {
  streamId?: string;
  outputs?: any[];
  onRemove: (...args: any[]) => void;
  onTerminate: (...args: any[]) => void;
  onUpdate: (...args: any[]) => void;
  getTypeDefinition: (...args: any[]) => void;
  types: any;
};

class OutputList extends React.Component<OutputListProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    streamId: '',
    outputs: [],
  };

  _formatOutput = (output) => (
    <Output key={output.id}
            output={output}
            streamId={this.props.streamId}
            removeOutputFromStream={this.props.onRemove}
            removeOutputGlobally={this.props.onTerminate}
            onUpdate={this.props.onUpdate}
            getTypeDefinition={this.props.getTypeDefinition}
            types={this.props.types} />
  );

  render() {
    if (!this.props.outputs) {
      return <Spinner />;
    }

    if (this.props.outputs.length === 0) {
      return (
        <Row className="content">
          <Col md={12}>
            <NoEntitiesExist>
              No outputs configured.
            </NoEntitiesExist>
          </Col>
        </Row>
      );
    }

    const outputs = this.props.outputs.sort(sortByTitle).map(this._formatOutput);

    return <div>{outputs}</div>;
  }
}

export default OutputList;
