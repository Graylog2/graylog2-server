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
