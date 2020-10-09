import React, { useEffect, useReducer, useState } from 'react';
import PropTypes from 'prop-types';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { IfPermitted, HasOwnership, PaginatedList, SearchForm, Spinner, EntityList } from 'components/common';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import EntityShareModal from 'components/permissions/EntityShareModal';

import View from './View';

import ViewTypeLabel from '../ViewTypeLabel';

const itemActionsFactory = (view, onViewDelete, setViewToShare) => {
  return (
    <ButtonToolbar>
      <DropdownButton title="Actions" id={`view-actions-dropdown-${view.id}`} bsSize="small" pullRight>
        <HasOwnership type="dashboard" id={view.id}>
          {({ disabled }) => (
            <MenuItem disabled={disabled} onSelect={() => setViewToShare(view)}>
              Share {disabled && <SharingDisabledPopover type="dashboard" />}
            </MenuItem>
          )}
        </HasOwnership>
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
