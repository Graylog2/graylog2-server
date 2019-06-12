// @flow strict
import * as React from 'react';

import FieldType from 'views/logic/fieldtypes/FieldType';

export type ValueRendererProps = {
  field: string,
  value: *,
  type: FieldType,
};

export type ValueRenderer = React.ComponentType<ValueRendererProps>;
