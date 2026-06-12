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
import { useContext } from 'react';

import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import TableFilterContext from 'components/common/PaginatedEntityTable/TableFilterContext';

const useAppendTagFilter = () => {
  // Use the context directly (not the throwing hook) so the component is safe
  // to render outside a PaginatedEntityTable — falls back to whatever filters
  // are in the URL today.
  //
  // Read-from-context vs write-to-URL: when inside a table, the context's
  // searchParams.filters reflects the canonical state (URL + defaults).
  // Writing back via setUrlFilters propagates to context through the table's
  // URL-sync mechanism, so the asymmetry is intentional and consistent with
  // how other filter writers in the codebase work.
  const tableFilterContext = useContext(TableFilterContext);
  const [urlFilters, setUrlFilters] = useUrlQueryFilters();

  return (tag: string) => {
    const currentFilters = tableFilterContext?.searchParams.filters ?? urlFilters;
    const existing = currentFilters.get('tags', []);
    if (existing.includes(tag)) return;
    setUrlFilters(currentFilters.set('tags', [...existing, tag]));
  };
};

export default useAppendTagFilter;
