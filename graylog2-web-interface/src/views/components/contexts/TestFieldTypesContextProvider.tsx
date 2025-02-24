import * as React from 'react';
import * as Immutable from 'immutable';

import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

const fieldTypes = {
  all: Immutable.List<FieldTypeMapping>(),
  currentQuery: Immutable.List<FieldTypeMapping>(),
  queryFields: Immutable.Map<string, Immutable.List<FieldTypeMapping>>(),
};

const TestFieldTypesContextProvider = ({ children }: React.PropsWithChildren<{}>) => (
  <FieldTypesContext.Provider value={fieldTypes}>{children}</FieldTypesContext.Provider>
);

export default TestFieldTypesContextProvider;
