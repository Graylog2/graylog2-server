import React from 'react';
import { Alert, Col, Label } from 'react-bootstrap';

import { EntityListItem } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';

const AlarmCallbackHistory = React.createClass({
  propTypes: {
    types: React.PropTypes.object.isRequired,
    alarmCallbackHistory: React.PropTypes.object.isRequired,
  },

  render() {
    const history = this.props.alarmCallbackHistory;
    const configuration = history.alarmcallbackconfiguration;
    const type = this.props.types[configuration.type];

    const hadError = history.result.type === 'error';
    const result = (hadError ? <Label bsStyle="danger">Error</Label> : <Label bsStyle="success">Sent</Label>);

    const title = (
      <span>
        {type ? configuration.title || 'Untitled notification' : 'Unknown notification'}
        {' '}
        <small>({type ? type.name : configuration.type})</small>
      </span>
    );
    const description = (hadError ? `Error sending notification: ${history.result.error}` : 'Notification was sent successfully.');

    let configurationWell;
    let configurationInfo;
    if (type) {
      configurationWell = <ConfigurationWell configuration={configuration.configuration} typeDefinition={type} />;
    } else {
      configurationInfo = (
        <Alert bsStyle="warning">
          The plugin required for this notification is not loaded. Not displaying its configuration.
        </Alert>
      );
    }

    const content = (
      <Col md={12}>
        {configurationInfo}
        <div className="alert-callback">
          {configurationWell}
        </div>
      </Col>
    );

    return (
      <EntityListItem title={title} titleSuffix={result} description={description} contentRow={content} />
    );
  },
});

export default AlarmCallbackHistory;
