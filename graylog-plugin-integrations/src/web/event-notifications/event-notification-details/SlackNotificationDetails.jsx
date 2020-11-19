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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/graylog';

const NewExampleWell = styled(Well)(({ theme }) => css`
  margin-bottom: 5px;
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};
  white-space: pre-wrap;
  word-wrap: break-word;
`);

const SlackNotificationDetails = ({ notification }) => (
  <>
    <ReadOnlyFormGroup label="Webhook URL" value={notification.config.webhook_url} />
    <ReadOnlyFormGroup label="Channel" value={notification.config.channel} />
    <ReadOnlyFormGroup label="Custom Message Template "
                       value={(
                         <NewExampleWell bsSize="small">
                           {notification.config.custom_message || <em>Empty body</em>}
                         </NewExampleWell>
                       )} />
    <ReadOnlyFormGroup label="Message Backlog Limit" value={notification.config.backlog_size} />
    <ReadOnlyFormGroup label="User Name" value={notification.config.username} />
    <ReadOnlyFormGroup label="Notify Channel" value={notification.config.notify_channel} />
    <ReadOnlyFormGroup label="Link Names" value={notification.config.link_names} />
    <ReadOnlyFormGroup label="Icon URL" value={notification.config.icon_url} />
    <ReadOnlyFormGroup label="Icon Emoji" value={notification.config.icon_emoji} />
  </>
);

SlackNotificationDetails.propTypes = {
  notification: PropTypes.object.isRequired,
};

export default SlackNotificationDetails;
