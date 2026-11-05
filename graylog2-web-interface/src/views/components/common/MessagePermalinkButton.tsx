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
import { useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';

type Props = {
  messageIndex: string | undefined;
  messageId: string;
};

const MessagePermalinkButton = ({ messageIndex, messageId }: Props) => {
  const { t } = useTranslation('search');
  const messageUrl = messageIndex ? Routes.message_show(messageIndex, messageId) : '#';

  return (
    <Button href={messageUrl} disabled={!messageIndex} bsSize="small">
      {t('message_permalink.label')}
    </Button>
  );
};

export default MessagePermalinkButton;
