// @flow strict
import * as React from 'react';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import ValueActions from './ValueActions';
import TypeSpecificValue from './TypeSpecificValue';

type RenderProps = {
  field: string,
  value: *,
};

type Props = {
  field: string,
  value: *,
  render?: React.ComponentType<RenderProps>,
  queryId: string,
  type: FieldType,
};

const defaultRenderer: React.ComponentType<RenderProps> = ({ value }: RenderProps) => value;

const Value = ({ field, value, queryId, render = defaultRenderer, type = FieldType.Unknown }: Props) => {
  const RenderComponent = render || ((props: RenderProps) => props.value);
  const Component = v => <RenderComponent field={field} value={v.value} />;
  const element = <TypeSpecificValue value={value} type={type} render={Component} />;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      {field} = <TypeSpecificValue value={value} type={type} truncate />
    </ValueActions>
  );
};

Value.defaultProps = {
  children: null,
  render: defaultRenderer,
};

export default Value;
