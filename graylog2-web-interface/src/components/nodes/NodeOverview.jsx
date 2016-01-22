import React, {PropTypes} from 'react';
import { Row, Col } from 'react-bootstrap';

import BufferUsage from './BufferUsage';
import SystemOverviewDetails from './SystemOverviewDetails';
import JvmHeapUsage from './JvmHeapUsage';

const NodeOverview = React.createClass({
  propTypes: {
    node: PropTypes.object.isRequired,
    systemOverview: PropTypes.object.isRequired,
  },
  render() {
    const node = this.props.node;
    const systemOverview = this.props.systemOverview;

    return (
      <div>
        <Row className="content">
          <Col md={12}>
            <SystemOverviewDetails node={node} information={systemOverview}/>
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2 style={{marginBottom: 5}}>Memory/Heap usage</h2>
            <JvmHeapUsage nodeId={node.node_id}/>
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <h2>Buffers</h2>
            <p className="description">
              Buffers are built to cache small amounts of messages for a very short time
              (usually milliseconds) on their way through the different processors.
            </p>
            <Row>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Input buffer" bufferType="input"/>
              </Col>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Process buffer" bufferType="process"/>
              </Col>
              <Col md={4}>
                <BufferUsage nodeId={node.node_id} title="Output buffer" bufferType="output"/>
              </Col>
            </Row>
          </Col>
        </Row>
      </div>
    );
  },
});

export default NodeOverview;
