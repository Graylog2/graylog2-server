import React, {PropTypes} from 'react';
import {Row, Col, Button} from 'react-bootstrap';

const PipelineStreamComponent = React.createClass({
  propTypes: {
    pipelines: PropTypes.array.isRequired,
    streams: PropTypes.array.isRequired,
    assignments: PropTypes.array.isRequired,
  },
  render() {
    let streamAssignments = [
      <li key="1">Todo</li>
    ];
    return (
      <Row className="content">
        <Col md={12}>
          <h2>Stream binding</h2>
          <ul>
            {streamAssignments}
          </ul>
        </Col>
      </Row>);
  },
});

export default PipelineStreamComponent;