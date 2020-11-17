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
import React from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';

import CommonNotificationSummary from './CommonNotificationSummary';
import commonStyles from './LegacyNotificationCommonStyles.css';

class LegacyNotificationSummary extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object.isRequired,
    definitionNotification: PropTypes.object.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  render() {
    const { notification, legacyTypes } = this.props;
    const configurationValues = notification.config.configuration;
    const callbackType = notification.config.callback_type;
    const typeData = legacyTypes[callbackType];

    let content;

    if (typeData) {
      const typeConfiguration = typeData.configuration;

      content = Object.entries(typeConfiguration)
        .map(([key, value]) => {
          return (
            <tr key={key}>
              <td>{value.human_name}</td>
              <td>{configurationValues[key]}</td>
            </tr>
          );
        });
    } else {
      content = (
        <tr className="danger">
          <td>Type</td>
          <td>
            Unknown legacy alarm callback type: <code>{callbackType}</code>.
            Please make sure the plugin is installed.
          </td>
        </tr>
      );
    }

    return (
      <>
        {!typeData && (
          <Alert bsStyle="danger" className={commonStyles.legacyNotificationAlert}>
            Error in {notification.title || 'Legacy Alarm Callback'}: Unknown type <code>{callbackType}</code>,
            please ensure the plugin is installed.
          </Alert>
        )}
        <CommonNotificationSummary {...this.props}>
          <>
            {content}
          </>
        </CommonNotificationSummary>
      </>
    );
  }
}

export default LegacyNotificationSummary;
