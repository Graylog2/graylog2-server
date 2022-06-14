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
import * as React from 'react';
import styled, { css } from 'styled-components';

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/bootstrap';

import type { TeamsNotificationSummaryType } from '../types';

const NewExampleWell = styled(Well)(({ theme }) => css`
  margin-bottom: 5px;
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};
  white-space: pre-wrap;
  word-wrap: break-word;
`);

const TeamsNotificationDetails: React.FC<TeamsNotificationSummaryType> = ({ notification }) => (
  <>
    <ReadOnlyFormGroup label="Webhook URL" value={notification.config.webhook_url} />
    <ReadOnlyFormGroup label="Custom Message Template "
                       value={(
                         <NewExampleWell bsSize="small">
                           {notification.config.custom_message || <em>Empty body</em>}
                         </NewExampleWell>
                       )} />
    <ReadOnlyFormGroup label="Message Backlog Limit" value={notification.config.backlog_size} />
    <ReadOnlyFormGroup label="Icon URL" value={notification.config.icon_url} />
  </>
);

export default TeamsNotificationDetails;
