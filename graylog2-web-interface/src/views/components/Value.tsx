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
import CustomHighlighting from 'views/components/highlighting/CustomHighlighting';

import ValueActions from './actions/ValueActions';
import TypeSpecificValue from './TypeSpecificValue';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {
  field: string,
  value: any,
  render?: ValueRenderer,
  type: FieldType,
};

const ValueActionTitle = styled.span`
  white-space: nowrap;
`;

type TypeSpecificValueWithHighlightProps = {
  field: string,
  value?: any,
  type?: FieldType
  render?: React.ComponentType<ValueRendererProps>,
}
const TypeSpecificValueWithHighlight = ({ field, value, type, render }: TypeSpecificValueWithHighlightProps) => (
  <CustomHighlighting field={field}
                      value={value}>
    <TypeSpecificValue field={field} value={value} type={type} render={render} />
  </CustomHighlighting>
);

const defaultRenderer: ValueRenderer = ({ value }: ValueRendererProps) => value;

const InteractiveValue = ({ field, value, render, type }: Props) => {
  const queryId = useActiveQueryId();
  const RenderComponent: ValueRenderer = useMemo(() => render ?? ((props: ValueRendererProps) => props.value), [render]);
  const Component = useCallback(({ value: componentValue }) => <RenderComponent field={field} value={componentValue} />, [RenderComponent, field]);
  const element = <TypeSpecificValueWithHighlight field={field} value={value} type={type} render={Component} />;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      <ValueActionTitle data-testid="value-actions-title">
        {field} = <TypeSpecificValue field={field} value={value} type={type} truncate />
      </ValueActionTitle>
    </ValueActions>
  );
};

InteractiveValue.defaultProps = {
  render: defaultRenderer,
};

const Value = ({ field, value, render = defaultRenderer, type = FieldType.Unknown }: Props) => (
  <InteractiveContext.Consumer>
    {(interactive) => (interactive
      ? <InteractiveValue field={field} value={value} render={render} type={type} />
      : <span><TypeSpecificValueWithHighlight field={field} value={value} render={render} type={type} /></span>)}
  </InteractiveContext.Consumer>
);

Value.defaultProps = {
  render: defaultRenderer,
};

export default Value;
