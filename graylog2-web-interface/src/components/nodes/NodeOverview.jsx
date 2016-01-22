import React, {PropTypes} from 'react';
import { Row, Col } from 'react-bootstrap';

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
      </div>
    );
  },
});

export default NodeOverview;
