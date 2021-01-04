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
import styled from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { ValueRenderer, ValueRendererProps } from 'views/components/messagelist/decoration/ValueRenderer';

import ValueActions from './actions/ValueActions';
import TypeSpecificValue from './TypeSpecificValue';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {
  children?: React.ReactNode,
  field: string,
  value: any,
  render?: ValueRenderer,
  queryId: string | null | undefined,
  type: FieldType,
};

const ValueActionTitle = styled.span`
  white-space: nowrap;
`;

const defaultRenderer: ValueRenderer = ({ value }: ValueRendererProps) => value;

const Value = ({ children, field, value, queryId, render = defaultRenderer, type = FieldType.Unknown }: Props) => {
  const RenderComponent: ValueRenderer = render || ((props: ValueRendererProps) => props.value);
  const Component = (v) => <RenderComponent field={field} value={v.value} type={type} />;
  const element = <TypeSpecificValue field={field} value={value} type={type} render={Component} />;

  return (
    <InteractiveContext.Consumer>
      {(interactive) => ((interactive && queryId)
        ? (
          <ValueActions element={children || element} field={field} queryId={queryId} type={type} value={value}>
            <ValueActionTitle>
              {field} = <TypeSpecificValue field={field} value={value} type={type} truncate />
            </ValueActionTitle>
          </ValueActions>
        )
        : <span><TypeSpecificValue field={field} value={value} type={type} /></span>)}
    </InteractiveContext.Consumer>
  );
};

Value.defaultProps = {
  render: defaultRenderer,
  children: null,
};

export default Value;
