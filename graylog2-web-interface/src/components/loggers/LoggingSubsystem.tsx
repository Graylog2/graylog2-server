import React from 'react';
import capitalize from 'lodash/capitalize';

import { ButtonGroup, Col } from 'components/bootstrap';
import { LogLevelDropdown } from 'components/loggers';

type LoggingSubsystemProps = {
  name: string;
  nodeId: string;
  subsystem: any;
};

class LoggingSubsystem extends React.Component<LoggingSubsystemProps, {
  [key: string]: any;
}> {
  render() {
    return (
      <div className="subsystem-row">
        <Col md={6} className="subsystem" style={{ marginBottom: '10px' }}>
          <h3 className="u-light">
            Subsystem: {capitalize(this.props.name)}
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
