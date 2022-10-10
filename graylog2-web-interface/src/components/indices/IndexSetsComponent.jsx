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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import { Link, LinkContainer } from 'components/common/router';
import { Button, Col, DropdownButton, Label, MenuItem } from 'components/bootstrap';
import { EntityList, EntityListItem, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';
import NumberUtils from 'util/NumberUtils';
import { IndexSetDeletionForm, IndexSetDetails } from 'components/indices';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';

const IndexSetsComponent = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    paginationQueryParameter: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(IndexSetsStore)],

  componentDidMount() {
    this.loadData(this.props.paginationQueryParameter.page, this.PAGE_SIZE);
  },

  forms: {},

  loadData(pageNo, limit) {
    this.currentPageNo = pageNo;
    this.currentPageSize = limit;
    IndexSetsActions.listPaginated((pageNo - 1) * limit, limit, true);
    IndexSetsActions.stats();
  },

  // Stores the current page and page size to be able to reload the current page
  currentPageNo: 1,

  currentPageSize: 10,
  PAGE_SIZE: 10,

  _onChangePaginatedList(page, size) {
    this.loadData(page, size);
  },

  _onSetDefault(indexSet) {
    return () => {
      IndexSetsActions.setDefault(indexSet).then(() => this.loadData(this.currentPageNo, this.currentPageSize));
    };
  },

  _onDelete(indexSet) {
    return () => {
      this.forms[`index-set-deletion-form-${indexSet.id}`].open();
    };
  },

  _deleteIndexSet(indexSet, deleteIndices) {
    this.props.paginationQueryParameter.resetPage();

    IndexSetsActions.delete(indexSet, deleteIndices).then(() => {
      this.loadData(1, this.PAGE_SIZE);
    });
  },
  // eslint-disable-next-line react/no-unstable-nested-components
  _formatIndexSet(indexSet) {
    const { indexSetStats } = this.state;

    const actions = (
      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id)}>
          <Button>Edit</Button>
        </LinkContainer>
        <DropdownButton title="More Actions" id={`index-set-dropdown-${indexSet.id}`} pullRight>
          <MenuItem onSelect={this._onSetDefault(indexSet)}
                    disabled={!indexSet.can_be_default || indexSet.default}>Set as default
          </MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this._onDelete(indexSet)}>Delete</MenuItem>
        </DropdownButton>
      </ButtonToolbar>
    );

    const content = (
      <Col md={12}>
        <IndexSetDetails indexSet={indexSet} />

        <IndexSetDeletionForm ref={(elem) => { this.forms[`index-set-deletion-form-${indexSet.id}`] = elem; }} indexSet={indexSet} onDelete={this._deleteIndexSet} />
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
      statsString = this._formatStatsString(stats);
    }

    return (
      <EntityListItem key={`index-set-${indexSet.id}`}
                      title={indexSetTitle}
                      titleSuffix={<span>{statsString} {isDefault} {isReadOnly}</span>}
                      description={description}
                      actions={actions}
                      contentRow={content} />
    );
  },

  _formatStatsString(stats) {
    if (!stats) {
      return 'N/A';
    }

    const indices = `${NumberUtils.formatNumber(stats.indices)} ${StringUtils.pluralize(stats.indices, 'index', 'indices')}`;
    const documents = `${NumberUtils.formatNumber(stats.documents)} ${StringUtils.pluralize(stats.documents, 'document', 'documents')}`;
    const size = NumberUtils.formatBytes(stats.size);

    return `${indices}, ${documents}, ${size}`;
  },

  _isLoading() {
    const { indexSets } = this.state;

    return !indexSets;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { globalIndexSetStats, indexSetsCount, indexSets } = this.state;

    return (
      <div>
        <h4><strong>Total:</strong> {this._formatStatsString(globalIndexSetStats)}</h4>

        <hr style={{ marginBottom: 0 }} />

        <PaginatedList pageSize={this.PAGE_SIZE}
                       totalItems={indexSetsCount}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle="info"
                      noItemsText="There are no index sets to display"
                      items={indexSets.map((indexSet) => this._formatIndexSet(indexSet))} />
        </PaginatedList>
      </div>
    );
  },
});

export default withPaginationQueryParameter(IndexSetsComponent);
