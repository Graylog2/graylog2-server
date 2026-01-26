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
import { useCallback } from 'react';

import { PaginatedEntityTable } from 'components/common';
import type View from 'views/logic/views/View';
import QueryHelper from 'components/common/QueryHelper';
import { fetchSavedSearches, queryKey } from 'views/hooks/useSavedSearches';
import useColumnRenderers from 'views/components/searchbar/saved-search/useColumnRenderes';
import usePluggableEntityTableElements from 'hooks/usePluggableEntityTableElements';
import { DEFAULT_LAYOUT } from 'views/components/searchbar/saved-search/Constants';

import SearchActions from './SearchActions';

import BulkActions from '../BulkActions';

type Props = {
  activeSavedSearchId: string;
  deleteSavedSearch: (view: View) => Promise<void>;
  onLoadSavedSearch: () => void;
};

const SavedSearchesOverview = ({ activeSavedSearchId, deleteSavedSearch, onLoadSavedSearch }: Props) => {
  const { pluggableAttributes, pluggableExpandedSections } = usePluggableEntityTableElements<View>(null, 'search');

  const renderSavedSearchActions = useCallback(
    (search: View) => (
      <SearchActions
        search={search}
        onDeleteSavedSearch={deleteSavedSearch}
        activeSavedSearchId={activeSavedSearchId}
      />
    ),
    [activeSavedSearchId, deleteSavedSearch],
  );

  const customColumnRenderers = useColumnRenderers(onLoadSavedSearch);

  return (
    <PaginatedEntityTable<View>
      additionalAttributes={pluggableAttributes.attributes}
      bulkSelection={{ actions: <BulkActions /> }}
      columnRenderers={customColumnRenderers}
      entityActions={renderSavedSearchActions}
      entityAttributesAreCamelCase
      expandedSectionRenderers={pluggableExpandedSections}
      fetchEntities={fetchSavedSearches}
      focusSearchAfterMount
      humanName="Saved Searches"
      keyFn={queryKey}
      queryHelpComponent={<QueryHelper entityName="search" commonFields={['id', 'title']} />}
      tableLayout={DEFAULT_LAYOUT}
      withoutURLParams
    />
  );
};

export default SavedSearchesOverview;
