import React, { useEffect, useReducer } from 'react';
import PropTypes from 'prop-types';
import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import IfPermitted from 'components/common/IfPermitted';

import { PaginatedList, SearchForm, Spinner, EntityList } from 'components/common';
import View from './View';

const itemActionsFactory = (view, onViewDelete) => {
  return (
    <IfPermitted permissions={[`view:edit:${view.id}`, 'view:edit']} anyPermissions>
      <ButtonToolbar>
        <DropdownButton title="Actions" id={`view-actions-dropdown-${view.id}`} bsSize="small" pullRight>
          <MenuItem onSelect={onViewDelete(view)}>Delete</MenuItem>
        </DropdownButton>
      </ButtonToolbar>
    </IfPermitted>
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
      {itemActionsFactory(view, onViewDelete)}
    </View>
  ));

  return (
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
