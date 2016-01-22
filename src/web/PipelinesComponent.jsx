import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import PipelinesActions from 'PipelinesActions';

const PipelinesComponent = React.createClass({
  propTypes: {
    pipelines: PropTypes.array.isRequired,
  },

  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <h2>Pipelines</h2>
          <p>{this.props.pipelines.length}</p>
        </Col>
      </Row>
    );
  },
});

export default PipelinesComponent;