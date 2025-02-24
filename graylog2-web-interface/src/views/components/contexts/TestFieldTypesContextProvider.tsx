import * as React from 'react';
import * as Immutable from 'immutable';

import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

export const SimpleFieldTypesContextProvider = ({
  children = undefined,
  fields,
}: React.PropsWithChildren<{ fields: FieldTypeMapping[] }>) => {
  const fieldList = Immutable.List(fields);
  const value = { all: fieldList, currentQuery: fieldList };

  return <FieldTypesContext.Provider value={value}>{children}</FieldTypesContext.Provider>;
};

const TestFieldTypesContextProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <SimpleFieldTypesContextProvider fields={[]}>{children}</SimpleFieldTypesContextProvider>
);

export default TestFieldTypesContextProvider;
