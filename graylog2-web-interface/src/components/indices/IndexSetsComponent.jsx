import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';
import { Col, Button, Label, DropdownButton, MenuItem } from 'react-bootstrap';

import { EntityList, EntityListItem, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import StringUtils from 'util/StringUtils';
import NumberUtils from 'util/NumberUtils';

import { IndexSetDeletionForm, IndexSetDetails } from 'components/indices';

import CombinedProvider from 'injection/CombinedProvider';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');

const IndexSetsComponent = createReactClass({
  displayName: 'IndexSetsComponent',
  mixins: [Reflux.connect(IndexSetsStore)],

  componentDidMount() {
    this.loadData(1, this.PAGE_SIZE);
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
    IndexSetsActions.delete(indexSet, deleteIndices).then(() => {
      this.loadData(1, this.PAGE_SIZE);
    });
  },

  _formatIndexSet(indexSet) {
    const actions = (
      <div>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id)}>
          <Button bsStyle="info">Edit</Button>
        </LinkContainer>
        {' '}
        <DropdownButton title="More Actions" id={`index-set-dropdown-${indexSet.id}`} pullRight>
          <MenuItem
            onSelect={this._onSetDefault(indexSet)}
            disabled={!indexSet.writable || indexSet.default}>Set as default</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this._onDelete(indexSet)}>Delete</MenuItem>
        </DropdownButton>
      </div>
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
    let description = indexSet.description;
    if (indexSet.default) {
      description += `${description.endsWith('.') ? '' : '.'} Graylog will use this index set by default.`;
    }

    let statsString;
    const stats = this.state.indexSetStats[indexSet.id];
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
    return !this.state.indexSets;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <div>
        <h4><strong>Total:</strong> {this._formatStatsString(this.state.globalIndexSetStats)}</h4>

        <hr style={{ marginBottom: 0 }} />

        <PaginatedList pageSize={this.PAGE_SIZE}
                       totalItems={this.state.indexSetsCount}
                       onChange={this._onChangePaginatedList}
                       showPageSizeSelect={false}>
          <EntityList bsNoItemsStyle="info"
                      noItemsText="There are no index sets to display"
                      items={this.state.indexSets.map(indexSet => this._formatIndexSet(indexSet))} />
        </PaginatedList>
      </div>
    );
  },
});

export default IndexSetsComponent;
