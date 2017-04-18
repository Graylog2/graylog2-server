import React from 'react';
import { Row, Col } from 'react-bootstrap';

const DataAdapter = React.createClass({

  propTypes: {
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h3>{this.props.dataAdapter.title}</h3>
          <span>{this.props.dataAdapter.description}</span>
        </Col>
        <Col md={6}>
          <h3>Retrieve data</h3>
          <p>Use this to manually trigger the data adapter.</p>
        </Col>
      </Row>
    );
  },

});

export default DataAdapter;
