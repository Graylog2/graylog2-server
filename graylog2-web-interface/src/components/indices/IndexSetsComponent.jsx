import React from 'react';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';
import { Col, Button, Label } from 'react-bootstrap';

import { EntityList, EntityListItem, PaginatedList, Spinner } from 'components/common';
import Routes from 'routing/Routes';

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

  _formatIndexSet(indexSet) {
    const actions = (
      <span>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.SHOW(indexSet.id)}>
          <Button bsStyle="info">Show</Button>
        </LinkContainer>
        <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id)}>
          <Button bsStyle="info">Edit</Button>
        </LinkContainer>
      </span>
    );

    const content = (
      <Col md={12}>
        <ul className="no-padding">
          <li><b>Index prefix:</b> {indexSet.index_prefix}</li>
          <li><b>Shards:</b> {indexSet.shards}</li>
          <li><b>Replicas:</b> {indexSet.replicas}</li>
        </ul>
      </Col>
    );

    const isDefault = indexSet.default ? <Label key={`index-set-${indexSet.id}-default-label`} bsStyle="primary">default</Label> : '';

    return (
      <EntityListItem key={`index-set-${indexSet.id}`}
                    title={indexSet.title}
                    titleSuffix={isDefault}
                    description={indexSet.description}
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
