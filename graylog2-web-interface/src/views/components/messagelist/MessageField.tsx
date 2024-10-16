import React from 'react';
import styled, { css } from 'styled-components';

import Field from 'views/components/Field';
import Value from 'views/components/Value';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { FULL_MESSAGE_FIELD } from 'views/Constants';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

import DecoratedValue from './decoration/DecoratedValue';
import type { Message } from './Types';

import CustomPropTypes from '../CustomPropTypes';
import InteractiveContext from '../contexts/InteractiveContext';

const SPECIAL_FIELDS = [FULL_MESSAGE_FIELD, 'level'];

type Props = {
  fieldName: string,
  fieldType: FieldType,
  message: Message,
  value: any,
};

const DecoratedField = styled.small(({ theme }) => css`
  color: ${theme.colors.gray[70]};
  font-weight: normal;
`);

const DefinitionDescription = styled.dd(({ theme }) => `
  font-family: ${theme.fonts.family.monospace};
`);

const MessageField = ({ fieldName, fieldType, message, value }: Props) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;
  const activeQuery = useActiveQueryId();

  const {
    decoration_stats: decorationStats = {
      added_fields: {},
      changed_fields: {},
      removed_fields: {},
    },
  } = message;

  const isDecoratedField = decorationStats
    && (decorationStats.added_fields[fieldName] !== undefined || decorationStats.changed_fields[fieldName] !== undefined);

  const ValueContext = isDecoratedField
    ? ({ children }) => (
      <InteractiveContext.Provider value={false}>
        {children} <DecoratedField>(decorated)</DecoratedField>
      </InteractiveContext.Provider>
    )
    : ({ children }) => children;

  return (
    <>
      <dt data-testid={`message-field-name-${fieldName}`}>
        <Field queryId={activeQuery} name={fieldName} type={isDecoratedField ? FieldType.Decorated : fieldType}>{fieldName}</Field>
      </dt>
      <DefinitionDescription data-testid={`message-field-value-${fieldName}`}>
        <ValueContext>
          <Value field={fieldName} value={innerValue} type={isDecoratedField ? FieldType.Decorated : fieldType} render={DecoratedValue} />
        </ValueContext>
      </DefinitionDescription>
    </>
  );
};

export default MessageField;
