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
