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
import { useMemo } from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';

import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { filtersToStreamSet } from 'views/logic/queries/Query';
import type { RelativeTimeRange } from 'views/logic/queries/Query';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';

import FieldTypesContext from './FieldTypesContext';

const defaultId = '';
const defaultTimeRange: RelativeTimeRange = { type: 'relative', from: 300 };

const DefaultFieldTypesProvider = ({ children }: { children: React.ReactElement }) => {
  const currentQuery = useCurrentQuery();
  const currentStreams = useMemo(() => filtersToStreamSet(currentQuery?.filter).toArray(), [currentQuery?.filter]);
  const { data: currentFieldTypes } = useFieldTypes(currentStreams, currentQuery?.timerange || defaultTimeRange);
  const { data: allFieldTypes } = useFieldTypes([], currentQuery?.timerange || defaultTimeRange);
  const queryFields = useMemo(() => Immutable.Map({ [currentQuery?.id || defaultId]: Immutable.List(currentFieldTypes) }), [currentFieldTypes, currentQuery?.id]);
  const all = useMemo(() => Immutable.List(allFieldTypes ?? []), [allFieldTypes]);
  const fieldTypes = useMemo(() => ({ all, queryFields }), [all, queryFields]);

  return (
    <FieldTypesContext.Provider value={fieldTypes}>
      {children}
    </FieldTypesContext.Provider>
  );
};

DefaultFieldTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default DefaultFieldTypesProvider;
