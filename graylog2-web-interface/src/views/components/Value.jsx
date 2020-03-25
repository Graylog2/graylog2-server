// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { ValueRenderer, ValueRendererProps } from 'views/components/messagelist/decoration/ValueRenderer';

import ValueActions from './actions/ValueActions';
import TypeSpecificValue from './TypeSpecificValue';
import InteractiveContext from './contexts/InteractiveContext';

type Props = {|
  children?: React.Node,
  field: string,
  value: *,
  render?: ValueRenderer,
  queryId: ?string,
  type: FieldType,
|};

const ValueActionTitle = styled.span`
  white-space: nowrap;
`;

const defaultRenderer: ValueRenderer = ({ value }: ValueRendererProps) => value;

const Value = ({ children, field, value, queryId, render = defaultRenderer, type = FieldType.Unknown }: Props) => {
  const RenderComponent: ValueRenderer = render || ((props: ValueRendererProps) => props.value);
  const Component = v => <RenderComponent field={field} value={v.value} type={type} />;
  const element = <TypeSpecificValue field={field} value={value} type={type} render={Component} />;

  return (
    <InteractiveContext.Consumer>
      {interactive => ((interactive && queryId)
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
