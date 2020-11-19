/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { Alert, Col, Label } from 'components/graylog';
import { EntityListItem, Timestamp } from 'components/common';
import { ConfigurationWell } from 'components/configurationforms';
import DateTime from 'logic/datetimes/DateTime';

class AlarmCallbackHistory extends React.Component {
  static propTypes = {
    types: PropTypes.object.isRequired,
    alarmCallbackHistory: PropTypes.object.isRequired,
  };

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
    const description = (hadError
      ? <span>Error sending notification at <Timestamp dateTime={history.created_at} format={DateTime.Formats.DATETIME} />: {history.result.error}</span>
      : <span>Notification was sent successfully at <Timestamp dateTime={history.created_at} format={DateTime.Formats.DATETIME} />.</span>);

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
  }
}

export default AlarmCallbackHistory;
