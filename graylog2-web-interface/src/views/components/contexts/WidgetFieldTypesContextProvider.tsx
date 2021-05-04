import * as React from 'react';
import { useContext } from 'react';
import * as Immutable from 'immutable';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';

type Props = {
  children: React.ReactNode,
};

const WidgetFieldTypesContextProvider = ({ children }: Props) => {
  const { timerange, streams } = useContext(WidgetContext);
  const query = useCurrentQuery();
  const { data: fieldTypes } = useFieldTypes(streams, timerange);
  const fieldTypesList = Immutable.List(fieldTypes);

  return (
    <FieldTypesContext.Provider value={{ all: fieldTypesList, queryFields: Immutable.Map({ [query.id]: fieldTypesList }) }}>
      {children}
    </FieldTypesContext.Provider>
  );
};

export default WidgetFieldTypesContextProvider;
