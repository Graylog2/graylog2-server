import PropTypes from 'prop-types';
import React from 'react';

import { MessageFieldDescription } from 'components/search';

const SPECIAL_FIELDS = ['full_message', 'level'];

const MessageField = ({ message, value, fieldName, customFieldActions, renderForDisplay }) => {
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

MessageField.propTypes = {
  customFieldActions: PropTypes.node,
  fieldName: PropTypes.string.isRequired,
  message: PropTypes.object.isRequired,
  renderForDisplay: PropTypes.func.isRequired,
  value: PropTypes.any.isRequired,
};

MessageField.defaultProps = {
  customFieldActions: undefined,
};

export default MessageField;
