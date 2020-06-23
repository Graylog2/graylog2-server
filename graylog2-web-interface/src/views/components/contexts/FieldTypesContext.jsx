// @flow strict
import * as React from 'react';
import { List, Map } from 'immutable';

import { singleton } from 'views/logic/singleton';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

type FieldTypeMappingsList = List<FieldTypeMapping>;
export type FieldTypes = {
  all: FieldTypeMappingsList,
  queryFields: Map<string, FieldTypeMappingsList>,
};

const FieldTypesContext = React.createContext<?FieldTypes>();
export default singleton('contexts.FieldTypesContext', () => FieldTypesContext);
