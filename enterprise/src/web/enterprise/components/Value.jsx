// @flow strict
import * as React from 'react';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import ValueActions from './ValueActions';
import TypeSpecificValue from './TypeSpecificValue';

type Props = {
  children?: React.Node,
  field: string,
  value: *,
  queryId: string,
  type: FieldType,
}

const Value = ({ children, field, value, queryId, type = FieldType.Unknown }: Props) => {
  const caption = <TypeSpecificValue value={value} type={type} />;
  const element = children || caption;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      {field} = <TypeSpecificValue value={value} type={type} truncate />
    </ValueActions>
  );
};

export default Value;
