import React from 'react';
import { Button, Col, Row } from 'react-bootstrap';

const LoggerOverview = React.createClass({
  render() {
    return (
      <Row className="row-sm log-writing-node content">
        <Col md={12}>
          <div style={{marginBottom: '20'}}>
            <div className="pull-right">
              <Button bsSize="sm" bsStyle="primary" className="trigger-log-level-metrics">
                <i className="fa fa-dashboard"/>{' '}
                Show log level metrics
              </Button>
            </div>
          </div>
        </Col>
      </Row>
    );
  },
});

export default LoggerOverview;
