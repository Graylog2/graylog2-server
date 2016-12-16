import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Col, Button, Label, DropdownButton, MenuItem } from 'react-bootstrap';

import { EntityList, EntityListItem, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';

import { IndexSetDeletionForm, IndexSetDetails } from 'components/indices';

import CombinedProvider from 'injection/CombinedProvider';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');

const IndexSetsComponent = React.createClass({
  mixins: [Reflux.connect(IndexSetsStore)],

  componentDidMount() {
    this.loadData(1, this.PAGE_SIZE);
  },

  loadData(pageNo, limit) {
    IndexSetsActions.listPaginated((pageNo - 1) * limit, limit);
  },

  PAGE_SIZE: 10,

  _onChangePaginatedList(page, size) {
    this.loadData(page, size);
  },

  _onDelete(indexSet) {
    return (e) => {
      e.preventDefault();

      this.refs[`index-set-deletion-form-${indexSet.id}`].open();
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
          <MenuItem onSelect={this._onDelete(indexSet)}>Delete</MenuItem>
        </DropdownButton>
      </div>
    );

    const content = (
      <Col md={12}>
        <IndexSetDetails indexSet={indexSet} />

        <IndexSetDeletionForm ref={`index-set-deletion-form-${indexSet.id}`} indexSet={indexSet} onDelete={this._deleteIndexSet} />
      </Col>
    );

    const indexSetTitle = (
      <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
        <a>{indexSet.title}</a>
      </LinkContainer>
    );

    const isDefault = indexSet.default ? <Label key={`index-set-${indexSet.id}-default-label`} bsStyle="primary">default</Label> : '';
    let description = indexSet.description;
    if (indexSet.default) {
      description += `${description.endsWith('.') ? '' : '.'} Graylog will use this index set by default.`;
    }

    return (
      <EntityListItem key={`index-set-${indexSet.id}`}
                      title={indexSetTitle}
                      titleSuffix={isDefault}
                      description={description}
                      actions={actions}
                      contentRow={content} />
    );
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
        <PaginatedList pageSize={this.PAGE_SIZE} totalItems={this.state.indexSetsCount} onChange={this._onChangePaginatedList}
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
