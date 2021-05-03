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
import React, { useEffect, useReducer, useState } from 'react';
import PropTypes from 'prop-types';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { IfPermitted, PaginatedList, SearchForm, Spinner, EntityList, ShareButton } from 'components/common';
import EntityShareModal from 'components/permissions/EntityShareModal';

import View from './View';

import ViewTypeLabel from '../ViewTypeLabel';
import QueryHelper from '../../../components/common/QueryHelper';

const itemActionsFactory = (view, onViewDelete, setViewToShare) => {
  return (
    <ButtonToolbar>
      <ShareButton entityId={view.id} entityType="dashboard" onClick={() => setViewToShare(view)} />
      <DropdownButton title="Actions" id={`view-actions-dropdown-${view.id}`} pullRight>
        <IfPermitted permissions={[`view:edit:${view.id}`, 'view:edit']} anyPermissions>
          <MenuItem onSelect={onViewDelete(view)}>Delete</MenuItem>
        </IfPermitted>
      </DropdownButton>
    </ButtonToolbar>
  );
};

const reducer = (state, action) => {
  const { payload = {} } = action;
  const { newQuery, newPage, newPerPage } = payload;

  switch (action.type) {
    case 'search':
      return { ...state, query: newQuery, page: 1 };
    case 'searchReset':
      return { ...state, query: '', page: 1 };
    case 'pageChange':
      return { ...state, page: newPage, perPage: newPerPage };
    case 'viewDelete':
      return { ...state, page: 1 };
    default:
      return state;
  }
};

const ViewList = ({ pagination, handleSearch, handleViewDelete, views }) => {
  const [{ query, page, perPage }, dispatch] = useReducer(reducer, { query: '', page: 1, perPage: 10 });
  const [viewToShare, setViewToShare] = useState();

  const execSearch = () => handleSearch(query, page, perPage);

  useEffect(() => {
    execSearch();
  }, [query, page, perPage]);

  const onViewDelete = (view) => () => {
    handleViewDelete(view).then(() => {
      dispatch({ type: 'viewDelete' });
      execSearch();
    });
  };

  if (!views) {
    return <Spinner text="Loading views..." />;
  }

  const items = views.map((view) => (
    <View key={`view-${view.id}`}
          id={view.id}
          owner={view.owner}
          createdAt={view.created_at}
          title={view.title}
          summary={view.summary}
          requires={view.requires}
          description={view.description}>
      {itemActionsFactory(view, onViewDelete, setViewToShare)}
    </View>
  ));

  return (
    <>
      { viewToShare && (
        <EntityShareModal entityId={viewToShare.id}
                          entityType="dashboard"
                          description={`Search for a User or Team to add as collaborator on this ${ViewTypeLabel({ type: viewToShare.type })}.`}
                          entityTitle={viewToShare.title}
                          onClose={() => setViewToShare(undefined)} />
      )}
      <PaginatedList onChange={(newPage, newPerPage) => dispatch({ type: 'pageChange', payload: { newPage, newPerPage } })}
                     activePage={pagination.page}
                     totalItems={pagination.total}
                     pageSize={pagination.perPage}
                     pageSizes={[10, 50, 100]}>
        <div style={{ marginBottom: 15 }}>
          <SearchForm onSearch={(newQuery) => dispatch({ type: 'search', payload: { newQuery } })}
                      queryHelpComponent={<QueryHelper entityName="dashboard" commonFields={['id', 'title', 'summary']} />}
                      onReset={() => dispatch({ type: 'searchReset' })}
                      topMargin={0} />
        </div>
        <EntityList items={items}
                    bsNoItemsStyle="success"
                    noItemsText="There are no views present/matching the filter!" />
      </PaginatedList>
    </>
  );
};

ViewList.propTypes = {
  views: PropTypes.arrayOf(PropTypes.object),
  pagination: PropTypes.shape({
    total: PropTypes.number.isRequired,
    page: PropTypes.number.isRequired,
    perPage: PropTypes.number.isRequired,
  }).isRequired,
  handleSearch: PropTypes.func.isRequired,
  handleViewDelete: PropTypes.func.isRequired,
};

ViewList.defaultProps = {
  views: undefined,
};

export default ViewList;
