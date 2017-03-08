import React from 'react';
import { ButtonGroup, Col } from 'react-bootstrap';
import String from 'string';

import { LogLevelDropdown } from 'components/loggers';

const LoggingSubsystem = React.createClass({
  propTypes: {
    name: React.PropTypes.string.isRequired,
    nodeId: React.PropTypes.string.isRequired,
    subsystem: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <div className="subsystem-row">
        <Col md={6} className="subsystem" style={{ marginBottom: '10px' }}>
          <h3 className="u-light">
            Subsystem: {String(this.props.name).capitalize().toString()}
            <ButtonGroup className="pull-right">
              <LogLevelDropdown nodeId={this.props.nodeId} name={this.props.name} subsystem={this.props.subsystem} />
            </ButtonGroup>
          </h3>
          {this.props.subsystem.description}
          <br style={{ clear: 'both' }} />
        </Col>
      </div>
    );
  },
});

export default LoggingSubsystem;
