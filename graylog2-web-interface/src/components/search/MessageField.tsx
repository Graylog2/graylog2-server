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

import { MessageFieldDescription } from 'components/search';
import type { Message } from 'views/components/messagelist/Types';

type MessageFieldProps = {
  customFieldActions?: React.ReactElement;
  fieldName: string;
  message: Message;
  renderForDisplay: (name: string) => React.ReactElement;
};

const MessageField = ({ message, fieldName, customFieldActions = undefined, renderForDisplay }: MessageFieldProps) => (
  <span>
    <dt key={`${fieldName}Title`}>{fieldName}</dt>
    <MessageFieldDescription
      key={`${fieldName}Description`}
      message={message}
      fieldName={fieldName}
      renderForDisplay={renderForDisplay}
      customFieldActions={customFieldActions}
    />
  </span>
);

export default MessageField;
