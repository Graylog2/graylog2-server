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
import styled, { css } from 'styled-components';

import { Alert } from 'components/bootstrap';

const DangerRow = styled.tr(
  ({ theme }) => css`
    background-color: ${theme.colors.variant.light.danger};
  `,
);

import CommonNotificationSummary from './CommonNotificationSummary';
import commonStyles from './LegacyNotificationCommonStyles.css';

type Props = {
  type: string;
  notification: any;
  definitionNotification: any;
  legacyTypes: { [key: string]: { configuration: { [key: string]: { human_name: string } } } };
};

const LegacyNotificationSummary = (props: Props) => {
  const { notification, legacyTypes } = props;
  const configurationValues = notification.config.configuration;
  const callbackType = notification.config.callback_type;
  const typeData = legacyTypes[callbackType];

  let content;

  if (typeData) {
    const typeConfiguration = typeData.configuration;

    content = Object.entries(typeConfiguration).map(([key, value]) => (
      <tr key={key}>
        <td>{value.human_name}</td>
        <td>{configurationValues[key]}</td>
      </tr>
    ));
  } else {
    content = (
      <DangerRow>
        <td>Type</td>
        <td>
          Unknown legacy alarm callback type: <code>{callbackType}</code>. Please make sure the plugin is installed.
        </td>
      </DangerRow>
    );
  }

  return (
    <>
      {!typeData && (
        <Alert bsStyle="danger" className={commonStyles.legacyNotificationAlert}>
          Error in {notification.title || 'Legacy Alarm Callback'}: Unknown type <code>{callbackType}</code>, please
          ensure the plugin is installed.
        </Alert>
      )}
      <CommonNotificationSummary {...props}>
        <>{content}</>
      </CommonNotificationSummary>
    </>
  );
};

export default LegacyNotificationSummary;
