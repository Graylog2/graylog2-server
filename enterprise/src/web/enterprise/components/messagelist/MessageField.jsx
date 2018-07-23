import PropTypes from 'prop-types';
import React from 'react';

import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import Value from 'enterprise/components/Value';
import { ViewStore } from '../../stores/ViewStore';
import CustomPropTypes from '../CustomPropTypes';

const SPECIAL_FIELDS = ['full_message', 'level'];

const MessageField = ({ fieldName, fieldType, message, value, currentView }) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;
  const { activeQuery } = currentView;

  return (
    <span>
      <dt>
        <Field interactive queryId={activeQuery} name={fieldName} type={fieldType}>{fieldName}</Field>
      </dt>
      <dd>
        <Value queryId={activeQuery} field={fieldName} value={innerValue} type={fieldType} />
      </dd>
    </span>
  );
};

MessageField.propTypes = {
  currentView: CustomPropTypes.CurrentView.isRequired,
  fieldName: PropTypes.string.isRequired,
  fieldType: CustomPropTypes.FieldType.isRequired,
  message: PropTypes.object.isRequired,
  value: PropTypes.any.isRequired,
};

export default connect(MessageField, { currentView: ViewStore });
