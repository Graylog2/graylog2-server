// @flow strict
import * as React from 'react';

import { MessageDetailsDefinitionList } from 'components/graylog';

import MessageField from 'views/components/messagelist/MessageField';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import styles from './MessageFields.css';
import CustomHighlighting from './CustomHighlighting';

type Props = {
  message: {
    formatted_fields: {},
  },
  fields: FieldTypeMappingsList,
};

const MessageFields = ({ message, fields }: Props) => {
  const formattedFields = message.formatted_fields;
  const renderedFields = Object.keys(formattedFields)
    .sort()
    .map((key) => {
      const { type } = fields.find((t) => t.name === key, undefined, FieldTypeMapping.create(key, FieldType.Unknown));
      return (
        <CustomHighlighting key={key}
                            field={key}
                            value={formattedFields[key]}>
          <MessageField fieldName={key}
                        fieldType={type}
                        message={message}
                        value={formattedFields[key]}
                        disableFieldActions={false} />
        </CustomHighlighting>
      );
    });

  return (
    <MessageDetailsDefinitionList className={`message-details-fields ${styles.messageFields}`}>
      {renderedFields}
    </MessageDetailsDefinitionList>
  );
};

export default MessageFields;
