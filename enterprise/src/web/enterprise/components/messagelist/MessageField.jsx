// @flow strict
import PropTypes from 'prop-types';
import React from 'react';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import Value from 'enterprise/components/Value';
import { ViewStore } from 'enterprise/stores/ViewStore';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import CustomPropTypes from '../CustomPropTypes';
import Highlight from './Highlight';

const SPECIAL_FIELDS = ['full_message', 'level'];

type Props = {
  fieldName: string,
  fieldType: FieldType,
  message: {
    fields: { [string]: any },
  },
  value: any,
  currentView: {
    activeQuery: string,
  },
};

const MessageField = ({ fieldName, fieldType, message, value, currentView }: Props) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;
  const { activeQuery } = currentView;

  return (
    <span>
      <dt>
        <Field interactive queryId={activeQuery} name={fieldName} type={fieldType}>{fieldName}</Field>
      </dt>
      <dd>
        <Value queryId={activeQuery} field={fieldName} value={innerValue} type={fieldType} render={Highlight} />
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
