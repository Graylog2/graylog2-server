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
import { FULL_MESSAGE_FIELD } from 'views/Constants';

const SPECIAL_FIELDS = [FULL_MESSAGE_FIELD, 'level'];

type MessageFieldProps = {
  customFieldActions?: React.ReactNode;
  fieldName: string;
  message: any;
  renderForDisplay: (...args: any[]) => void;
  value: any;
};

const MessageField = ({
  message,
  value,
  fieldName,
  customFieldActions,
  renderForDisplay,
}: MessageFieldProps) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;

  return (
    <span>
      <dt key={`${fieldName}Title`}>{fieldName}</dt>
      <MessageFieldDescription key={`${fieldName}Description`}
                               message={message}
                               fieldName={fieldName}
                               fieldValue={innerValue}
                               renderForDisplay={renderForDisplay}
                               customFieldActions={customFieldActions} />
    </span>
  );
};

export default MessageField;
