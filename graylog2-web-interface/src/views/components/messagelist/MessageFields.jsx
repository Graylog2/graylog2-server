// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { MessageDetailsDefinitionList } from 'components/graylog';
import { type ThemeInterface } from 'theme/types';
import MessageField from 'views/components/messagelist/MessageField';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import CustomHighlighting from './CustomHighlighting';

type Props = {
  message: {
    formatted_fields: {},
  },
  fields: FieldTypeMappingsList,
};

const MessageDetailsDL: StyledComponent<{}, ThemeInterface, HTMLDListElement> = styled(MessageDetailsDefinitionList)(({ theme }) => `
  color: ${theme.colors.gray[40]};

  dd {
    font-family: monospace;

    &:not(:last-child) {
      border-bottom: 1px solid  ${theme.colors.gray[90]};
    }
  }
`);

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
    <MessageDetailsDL className="message-details-fields">
      {renderedFields}
    </MessageDetailsDL>
  );
};

export default MessageFields;
