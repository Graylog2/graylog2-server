// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import MessageField from 'views/components/messagelist/MessageField';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { StyledComponent } from 'styled-components';

import CustomHighlighting from './CustomHighlighting';

const Fields: StyledComponent<{}, {}, HTMLDListElement> = styled.dl`
  color: #666;

  dd {
    font-family: monospace;
  }

  dd:not(:last-child) {
      border-bottom: 1px solid #ececec;
  }
`;

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
      const { type } = fields.find(t => t.name === key, undefined, FieldTypeMapping.create(key, FieldType.Unknown));
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
    <Fields className="message-details message-details-fields">
      {renderedFields}
    </Fields>
  );
};

export default MessageFields;
