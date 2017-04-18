import React from 'react';
import { Row, Col } from 'react-bootstrap';

const LookupTable = React.createClass({

  propTypes: {
    table: React.PropTypes.object.isRequired,
    cache: React.PropTypes.object.isRequired,
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h3>{this.props.table.title}</h3>
          <span>{this.props.table.description}</span>
        </Col>
        <Col md={6}>
          <h3>Retrieve data</h3>
          <p>Use this to query the lookup table.</p>
        </Col>
      </Row>
    );
  },

});

export default LookupTable;
