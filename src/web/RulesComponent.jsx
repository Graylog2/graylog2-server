import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import RulesActions from 'RulesActions';

const RulesComponent = React.createClass({
  propTypes: {
    rules: PropTypes.array.isRequired,
  },

  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <h2>Rules</h2>
          <p>{this.props.rules.length}</p>
        </Col>
      </Row>
    );
  },
});

export default RulesComponent;