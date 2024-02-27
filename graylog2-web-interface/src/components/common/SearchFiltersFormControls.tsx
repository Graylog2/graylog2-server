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
import React from 'react';
import { Formik } from 'formik';
import { OrderedMap } from 'immutable';
import { v4 as uuidv4 } from 'uuid';

import usePluginEntities from 'hooks/usePluginEntities';
import SearchFilterBanner from 'views/components/searchbar/SearchFilterBanner';
import type { SearchFilter } from 'components/event-definitions/event-definitions-types';

type Props = {
  filters: SearchFilter[],
  onChange: (filters: SearchFilter[]) => void,
  hideFiltersPreview?: (val: boolean) => void,
};

function SearchFiltersFormControls({ filters, onChange, hideFiltersPreview }: Props) {
  const searchFiltersPlugin = usePluginEntities('eventDefinitions.components.searchForm') ?? [];
  const pluggableControls = searchFiltersPlugin.map((controlFn) => controlFn()).filter((control) => !!control);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  React.useEffect(() => hideFiltersPreview(!pluggableControls.length), []);

  const initialFilters = React.useMemo(() => {
    const searchFilters = OrderedMap(filters.map((filter) => ([
      filter.id || uuidv4(),
      { frontendId: filter.id || uuidv4(), ...filter },
    ])));

    return { searchFilters };
  }, [filters]);

  if (!pluggableControls.length) return <SearchFilterBanner onHide={() => hideFiltersPreview(true)} pluggableControls={pluggableControls} />;

  const SearchFiltersComponent = pluggableControls[0].component;

  const handleSearchFiltersChange = ({ searchFilters }: any) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const formFilters = searchFilters.map(({ frontendId, ...filter }) => filter);

    onChange(formFilters);
  };

  return (
    <Formik onSubmit={handleSearchFiltersChange} initialValues={initialFilters}>
      <SearchFiltersComponent />
    </Formik>
  );
}

SearchFiltersFormControls.defaultProps = {
  hideFiltersPreview: () => {},
};

export default SearchFiltersFormControls;
