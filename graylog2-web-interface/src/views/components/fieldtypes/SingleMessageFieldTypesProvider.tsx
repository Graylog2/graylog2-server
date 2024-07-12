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
import * as Immutable from 'immutable';
import React, { useMemo } from 'react';

import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

type Props = {
  children: React.ReactNode,
  streams: Array<string>,
  timestamp: string,
};

export const SingleMessageFieldTypesProvider = ({ streams, timestamp, children }: Props) => {
  const { data: fieldTypes } = useFieldTypes(streams, { type: 'absolute', from: timestamp, to: timestamp });
  const types = useMemo(() => {
    const fieldTypesList = Immutable.List(fieldTypes);

    return ({ all: fieldTypesList, queryFields: Immutable.Map({ query: fieldTypesList }) });
  }, [fieldTypes]);

  return (
    <FieldTypesContext.Provider value={types}>
      {children}
    </FieldTypesContext.Provider>
  );
};

export default SingleMessageFieldTypesProvider;
