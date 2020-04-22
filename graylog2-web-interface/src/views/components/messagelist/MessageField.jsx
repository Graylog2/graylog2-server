// @flow strict
import PropTypes from 'prop-types';
import React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import connect from 'stores/connect';
import Field from 'views/components/Field';
import Value from 'views/components/Value';
import { ViewStore } from 'views/stores/ViewStore';
import FieldType from 'views/logic/fieldtypes/FieldType';

import CustomPropTypes from '../CustomPropTypes';
import DecoratedValue from './decoration/DecoratedValue';
import InteractiveContext from '../contexts/InteractiveContext';
import type { Message } from './Types';

const SPECIAL_FIELDS = ['full_message', 'level'];

type Props = {
  fieldName: string,
  fieldType: FieldType,
  message: Message,
  value: any,
  currentView: {
    activeQuery: string,
  },
};

const DecoratedField: StyledComponent<{}, ThemeInterface, HTMLElement> = styled.small(({ theme }) => `
  color: ${theme.color.gray[70]};
  font-weight: normal;
`);

const MessageField = ({ fieldName, fieldType, message, value, currentView }: Props) => {
  const innerValue = SPECIAL_FIELDS.indexOf(fieldName) !== -1 ? message.fields[fieldName] : value;
  const { activeQuery } = currentView;

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
      <dt>
        <Field queryId={activeQuery} name={fieldName} type={isDecoratedField ? FieldType.Decorated : fieldType}>{fieldName}</Field>
      </dt>
      <dd>
        <ValueContext>
          <Value queryId={activeQuery} field={fieldName} value={innerValue} type={isDecoratedField ? FieldType.Decorated : fieldType} render={DecoratedValue} />
        </ValueContext>
      </dd>
    </>
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
