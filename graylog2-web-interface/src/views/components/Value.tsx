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
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import styled from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { ValueRenderer, ValueRendererProps } from 'views/components/messagelist/decoration/ValueRenderer';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import CustomHighlighting from 'views/components/highlighting/CustomHighlighting';

import ValueActions from './actions/ValueActions';
import TypeSpecificValue from './TypeSpecificValue';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {
  field: string,
  value: any,
  render?: ValueRenderer,
  type: FieldType,
  unit?: FieldUnit,
};

const ValueActionTitle = styled.span`
  white-space: nowrap;
`;

type TypeSpecificValueWithHighlightProps = {
  field: string,
  value?: any,
  type?: FieldType
  render?: React.ComponentType<ValueRendererProps>,
  unit?: FieldUnit,
}
const TypeSpecificValueWithHighlight = ({ field, value, type, render, unit }: TypeSpecificValueWithHighlightProps) => (
  <CustomHighlighting field={field}
                      value={value}>
    <TypeSpecificValue field={field} value={value} type={type} render={render} unit={unit} />
  </CustomHighlighting>
);

const defaultRenderer: ValueRenderer = ({ value }: ValueRendererProps) => value;

const InteractiveValue = ({ field, value, render = defaultRenderer, type, unit }: Props) => {
  const queryId = useActiveQueryId();
  const RenderComponent: ValueRenderer = useMemo(() => render ?? ((props: ValueRendererProps) => props.value), [render]);
  const Component = useCallback(({ value: componentValue }) => <RenderComponent field={field} value={componentValue} />, [RenderComponent, field]);
  const element = <TypeSpecificValueWithHighlight field={field} value={value} type={type} render={Component} unit={unit} />;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      <ValueActionTitle data-testid="value-actions-title">
        {field} = <TypeSpecificValue field={field} value={value} type={type} truncate />
      </ValueActionTitle>
    </ValueActions>
  );
};

const Value = ({ field, value, render = defaultRenderer, type = FieldType.Unknown, unit }: Props) => (
  <InteractiveContext.Consumer>
    {(interactive) => (interactive
      ? <InteractiveValue field={field} value={value} render={render} type={type} unit={unit} />
      : <span><TypeSpecificValueWithHighlight field={field} value={value} render={render} type={type} unit={unit} /></span>)}
  </InteractiveContext.Consumer>
);

export default Value;
