import React from 'react';
import Reflux from 'reflux';

import { Button, Col, Row } from 'react-bootstrap';

const LoggerOverview = React.createClass({
  mixins: [Reflux.connect(SampleStore({foo: 23}))],
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

          <span>{this.state.foo}</span>
        </Col>
      </Row>
    );
  },
});

export default LoggerOverview;
