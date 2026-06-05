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
import { Provider } from 'react-redux';

import FormWarningsProvider from 'contexts/FormWarningsProvider';
import StreamsContext from 'contexts/StreamsContext';
import type { EventDefinitionType } from 'components/event-definitions/types';
import createStore from 'store';

import FilterAggregationForm from './FilterAggregationForm';

const FilterAggregationFormContainer: EventDefinitionType['formComponent'] = (props) => {
  const streams = useContext(StreamsContext);
  const minimalStore = createStore([], {});

  return (
    <Provider store={minimalStore}>
      <FormWarningsProvider>
        <FilterAggregationForm streams={streams} {...props} />
      </FormWarningsProvider>
    </Provider>
  );
};

export default FilterAggregationFormContainer;
