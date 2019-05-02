// @flow strict
import * as React from 'react';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import type { ValueRenderer, ValueRendererProps } from 'enterprise/components/messagelist/decoration/ValueRenderer';

import ValueActions from './ValueActions';
import TypeSpecificValue from './TypeSpecificValue';

type Props = {
  field: string,
  value: *,
  render?: ValueRenderer,
  queryId: string,
  type: FieldType,
};

const defaultRenderer: ValueRenderer = ({ value }: ValueRendererProps) => value;

const Value = ({ field, value, queryId, render = defaultRenderer, type = FieldType.Unknown }: Props) => {
  const RenderComponent: ValueRenderer = render || ((props: ValueRendererProps) => props.value);
  const Component = v => <RenderComponent field={field} value={v.value} type={type} />;
  const element = <TypeSpecificValue field={field} value={value} type={type} render={Component} />;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      {field} = <TypeSpecificValue field={field} value={value} type={type} truncate />
    </ValueActions>
  );
};

Value.defaultProps = {
  render: defaultRenderer,
};

export default Value;
