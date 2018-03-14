import PropTypes from 'prop-types';
import React from 'react';
import { ButtonGroup, Col } from 'react-bootstrap';
import lodash from 'lodash';

import { LogLevelDropdown } from 'components/loggers';

class LoggingSubsystem extends React.Component {
  static propTypes = {
    name: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
    subsystem: PropTypes.object.isRequired,
  };

  render() {
    return (
      <div className="subsystem-row">
        <Col md={6} className="subsystem" style={{ marginBottom: '10px' }}>
          <h3 className="u-light">
            Subsystem: {lodash.capitalize(this.props.name)}
            <ButtonGroup className="pull-right">
              <LogLevelDropdown nodeId={this.props.nodeId} name={this.props.name} subsystem={this.props.subsystem} />
            </ButtonGroup>
          </h3>
          {this.props.subsystem.description}
          <br style={{ clear: 'both' }} />
        </Col>
      </div>
    );
  }
}

export default LoggingSubsystem;
