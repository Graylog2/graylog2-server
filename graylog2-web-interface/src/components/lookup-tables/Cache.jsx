import React from 'react';
import { Row, Col } from 'react-bootstrap';

const Cache = React.createClass({

  propTypes: {
    cache: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h3>{this.props.cache.title}</h3>
          <span>{this.props.cache.description}</span>
        </Col>
        <Col md={6}>
          <h3>Cached data</h3>
          <p>Use this to inspect the lookup table cache.</p>
        </Col>
      </Row>
    );
  },

});

export default Cache;
