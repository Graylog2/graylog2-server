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
import React, { useEffect, useState } from 'react';

import type { PaginationQueryParameterResult } from 'hooks/usePaginationQueryParameter';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import { Link, LinkContainer } from 'components/common/router';
import { Button, Col, DropdownButton, Label, MenuItem } from 'components/bootstrap';
import { EntityList, EntityListItem, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';
import NumberUtils from 'util/NumberUtils';
import { useStore } from 'stores/connect';
import { IndexSetDeletionForm, IndexSetDetails } from 'components/indices';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { IndexSetsStoreState, IndexSet, IndexSetStats } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';

const IndexSetsComponent = () => {
  const DEFAULT_PAGE_NUMBER = 1;
  const DEFAULT_PAGE_SIZE = 10;

  const { indexSetsCount, indexSets, globalIndexSetStats, indexSetStats } = useStore<IndexSetsStoreState>(IndexSetsStore);
  const { page, resetPage } : PaginationQueryParameterResult = usePaginationQueryParameter();

  // indexSets = paginated call
  // globalIndexSetStats = call to get all stats
  // indexSetStats = call for each set
  // indexSetsCount = number of all indexSets

  // boolean to enable/disable stats fetching -> list call stats true oder false
  // search index sets
  // index_sets/search?searchTitle=blabla&skip=1&limit=2&stats=false

  const [currentPageNumber, setCurrentPageNumber] = useState<number>(DEFAULT_PAGE_NUMBER);
  const [currentPageSize, setCurrentPageSize] = useState<number>(DEFAULT_PAGE_SIZE);
  const [forms, setForms] = useState<{[key: string]: { open:() => void }}>({});

  const loadData = (pageNumber: number, limit: number) => {
    setCurrentPageNumber(pageNumber);
    setCurrentPageSize(limit);
    IndexSetsActions.listPaginated((pageNumber - 1) * limit, limit, true);
    IndexSetsActions.stats();
  };

  useEffect(() => {
    loadData(page, DEFAULT_PAGE_SIZE);
  }, [page]);

  const onChangePaginatedList = (newPage: number, newSize: number) => {
    loadData(newPage, newSize);
  };

  const onSetDefault = (indexSet: IndexSet) => {
    return () => {
      IndexSetsActions.setDefault(indexSet).then(() => loadData(currentPageNumber, currentPageSize));
    };
  };

  const onDelete = (indexSet: IndexSet) => {
    return () => {
      forms[`index-set-deletion-form-${indexSet.id}`].open();
    };
  };

  const deleteIndexSet = (indexSet: IndexSet, deleteIndices: boolean) => {
    resetPage();

    IndexSetsActions.delete(indexSet, deleteIndices).then(() => {
      loadData(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    });
  };

  const formatStatsString = (stats: IndexSetStats) => {
    if (!stats) {
      return 'N/A';
    }

    const indices = `${NumberUtils.formatNumber(stats.indices)} ${StringUtils.pluralize(stats.indices, 'index', 'indices')}`;
    const documents = `${NumberUtils.formatNumber(stats.documents)} ${StringUtils.pluralize(stats.documents, 'document', 'documents')}`;
    const size = NumberUtils.formatBytes(stats.size);

    return `${indices}, ${documents}, ${size}`;
  };

  const formatIndexSet = (indexSet: IndexSet) => {
    const actions = (
      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id)}>
          <Button>Edit</Button>
        </LinkContainer>
        <DropdownButton title="More Actions" id={`index-set-dropdown-${indexSet.id}`} pullRight>
          <MenuItem onSelect={onSetDefault(indexSet)}
                    disabled={!indexSet.can_be_default || indexSet.default}>Set as default
          </MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={onDelete(indexSet)}>Delete</MenuItem>
        </DropdownButton>
      </ButtonToolbar>
    );

    const content = (
      <Col md={12}>
        <IndexSetDetails indexSet={indexSet} />

        <IndexSetDeletionForm ref={
          (elem) => { setForms({ ...forms, [`index-set-deletion-form-${indexSet.id}`]: elem }); }
}
                              indexSet={indexSet}
                              onDelete={deleteIndexSet} />
      </Col>
    );

    const indexSetTitle = (
      <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
        {indexSet.title}
      </Link>
    );

    const isDefault = indexSet.default ? <Label key={`index-set-${indexSet.id}-default-label`} bsStyle="primary">default</Label> : '';
    const isReadOnly = !indexSet.writable ? <Label key={`index-set-${indexSet.id}-readOnly-label`} bsStyle="info">read only</Label> : '';
    let { description } = indexSet;

    if (indexSet.default) {
      description += `${description.endsWith('.') ? '' : '.'} Graylog will use this index set by default.`;
    }

    let statsString;
    const stats = indexSetStats[indexSet.id];

    if (stats) {
      statsString = formatStatsString(stats);
    }

    return (
      <EntityListItem key={`index-set-${indexSet.id}`}
                      title={indexSetTitle}
                      titleSuffix={<span>{statsString} {isDefault} {isReadOnly}</span>}
                      description={description}
                      actions={actions}
                      contentRow={content} />
    );
  };

  const isLoading = !indexSets;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <div>
      <h4><strong>Total:</strong> {formatStatsString(globalIndexSetStats)}</h4>

      <hr style={{ marginBottom: 0 }} />

      <PaginatedList pageSize={DEFAULT_PAGE_SIZE}
                     totalItems={indexSetsCount}
                     onChange={onChangePaginatedList}
                     showPageSizeSelect={false}>
        <EntityList bsNoItemsStyle="info"
                    noItemsText="There are no index sets to display"
                    items={indexSets.map((indexSet) => formatIndexSet(indexSet))} />
      </PaginatedList>
    </div>
  );
};

export default IndexSetsComponent;
