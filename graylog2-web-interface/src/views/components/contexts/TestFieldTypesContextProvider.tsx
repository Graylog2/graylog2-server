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
