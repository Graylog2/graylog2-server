import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import IfPermitted from 'components/common/IfPermitted';

import { PaginatedList, SearchForm, Spinner, EntityList } from 'components/common';
import View from './View';

const ViewList = createReactClass({
  propTypes: {
    views: PropTypes.arrayOf(PropTypes.object),
    pagination: PropTypes.shape({
      total: PropTypes.number.isRequired,
      page: PropTypes.number.isRequired,
      perPage: PropTypes.number.isRequired,
    }).isRequired,
    handleSearch: PropTypes.func.isRequired,
    handleViewDelete: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      views: undefined,
    };
  },

  getInitialState() {
    return {
      query: '',
      page: 1,
      perPage: 10,
    };
  },

  componentDidMount() {
    this.execSearch();
  },

  execSearch(resetLoadingState = () => {
  }) {
    const { query, page, perPage } = this.state;
    this.props.handleSearch(query, page, perPage).then(resetLoadingState).catch(resetLoadingState);
  },

  handleSearch(query, resetLoadingState) {
    this.setState({ query: query, page: 1 }, () => {
      this.execSearch(resetLoadingState);
    });
  },

  handleSearchReset() {
    this.setState({ query: '', page: 1 }, () => {
      this.execSearch();
    });
  },

  handlePageChange(page, perPage) {
    this.setState({ page: page, perPage: perPage }, () => {
      this.execSearch();
    });
  },

  handleViewDelete(view) {
    return () => {
      this.props.handleViewDelete(view).then(() => {
        this.setState({ page: 1 }, () => {
          this.execSearch();
        });
      });
    };
  },

  itemActionsFactory(view) {
    return (
      <IfPermitted permissions={['*']}>
        <ButtonToolbar>
          <DropdownButton title="Actions" id={`view-actions-dropdown-${view.id}`} bsSize="small" pullRight>
            <MenuItem onSelect={this.handleViewDelete(view)}>Delete</MenuItem>
          </DropdownButton>
        </ButtonToolbar>
      </IfPermitted>
    );
  },

  render() {
    const list = this.props.views;

    if (!list) {
      return <Spinner text="Loading views..." />;
    }

    const items = list.map((view) => (
      <View key={`view-${view.id}`}
            id={view.id}
            owner={view.owner}
            createdAt={view.created_at}
            title={view.title}
            summary={view.summary}
            requires={view.requires}
            description={view.description}>
        {this.itemActionsFactory(view)}
      </View>
    ));

    const { total, page, perPage } = this.props.pagination;
    return (
      <PaginatedList onChange={this.handlePageChange}
                     activePage={page}
                     totalItems={total}
                     pageSize={perPage}
                     pageSizes={[10, 50, 100]}>
        <div style={{ marginBottom: 15 }}>
          <SearchForm onSearch={this.handleSearch}
                      onReset={this.handleSearchReset}
                      topMargin={0} />
        </div>
        <EntityList items={items}
                    bsNoItemsStyle="success"
                    noItemsText="There are no views present/matching the filter!" />
      </PaginatedList>
    );
  },
});

export default ViewList;
