import PropTypes from 'prop-types';
import React from 'react';

import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import Value from 'enterprise/components/Value';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';

const SPECIAL_FIELDS = ['full_message', 'level'];

const MessageField = ({ fieldName, message, value, currentView }) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;
  const { selectedQuery } = currentView;

  return (
    <span>
      <dt>
        <Field interactive queryId={selectedQuery} name={fieldName}>{fieldName}</Field>
      </dt>
      <dd>
        <Value queryId={selectedQuery} field={fieldName} value={innerValue}>{innerValue}</Value>
      </dd>
    </span>
  );
};

MessageField.propTypes = {
  fieldName: PropTypes.string.isRequired,
  message: PropTypes.object.isRequired,
  value: PropTypes.any.isRequired,
};

export default connect(MessageField, { currentView: CurrentViewStore });
