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
import styled, { css } from 'styled-components';

import Field from 'views/components/Field';
import Value from 'views/components/Value';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { FULL_MESSAGE_FIELD } from 'views/Constants';

import DecoratedValue from './decoration/DecoratedValue';
import type { Message } from './Types';

import InteractiveContext from '../contexts/InteractiveContext';

const SPECIAL_FIELDS = [FULL_MESSAGE_FIELD, 'level'];

type Props = {
  fieldName: string;
  fieldType: FieldType;
  message: Message;
  value: any;
};

const DecoratedField = styled.small(
  ({ theme }) => css`
    color: ${theme.colors.gray[70]};
    font-weight: normal;
  `,
);

const DefinitionDescription = styled.dd(
  ({ theme }) => `
  font-family: ${theme.fonts.family.monospace};
`,
);

type MessageFieldNamePros = {
  fieldName: string;
  fieldType: FieldType;
  message: Message;
  value?: unknown;
};

type MessageFieldValueProps = {
  fieldName: string;
  fieldType: FieldType;
  message: Message;
  value: any;
};

export const MessageFieldName = ({ fieldName, fieldType, message, value = undefined }: MessageFieldNamePros) => {
  const isDecoratedField =
    message?.decoration_stats?.added_fields?.[fieldName] || message?.decoration_stats?.changed_fields?.[fieldName];

  return (
    <Field name={fieldName} type={isDecoratedField ? FieldType.Decorated : fieldType} value={value}>
      {fieldName}
    </Field>
  );
};

const ValueContext = ({
  isDecoratedField,
  children = null,
}: React.PropsWithChildren<{ isDecoratedField: boolean }>) => {
  if (isDecoratedField)
    return (
      <InteractiveContext.Provider value="disabled">
        {children} <DecoratedField>(decorated)</DecoratedField>
      </InteractiveContext.Provider>
    );

  return <>{children}</>;
};

export const MessageFieldValue = ({ message, fieldName, fieldType, value }: MessageFieldValueProps) => {
  const isDecoratedField =
    message?.decoration_stats?.added_fields?.[fieldName] || message?.decoration_stats?.changed_fields?.[fieldName];

  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;

  const type = isDecoratedField ? FieldType.Decorated : fieldType;

  return (
    <ValueContext isDecoratedField={isDecoratedField}>
      {Array.isArray(innerValue) ? (
        innerValue.map((singleValue, idx) => (
          // eslint-disable-next-line react/no-array-index-key
          <React.Fragment key={`${idx}-${singleValue}`}>
            {idx > 0 && ', '}
            <Value field={fieldName} value={singleValue} type={type} render={DecoratedValue} />
          </React.Fragment>
        ))
      ) : (
        <Value field={fieldName} value={innerValue} type={type} render={DecoratedValue} />
      )}
    </ValueContext>
  );
};

const MessageField = ({ fieldName, fieldType, message, value }: Props) => (
  <>
    <dt data-testid={`message-field-name-${fieldName}`}>
      <MessageFieldName message={message} fieldName={fieldName} fieldType={fieldType} value={value} />
    </dt>
    <DefinitionDescription data-testid={`message-field-value-${fieldName}`}>
      <MessageFieldValue value={value} fieldName={fieldName} fieldType={fieldType} message={message} />
    </DefinitionDescription>
  </>
);

export default MessageField;
