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
import PropTypes from 'prop-types';
import React from 'react';

import {
  Col,
  Row,
} from 'components/bootstrap';
import {
  Pagination, PageSizeSelect, NoSearchResult, NoEntitiesExist,
} from 'components/common';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';
import ControlledTableList from 'components/common/ControlledTableList';
import ContentPackListItem from 'components/content-packs/components/ContentPackListItem';
import withTelemetry from 'logic/telemetry/withTelemetry';
import withLocation from 'routing/withLocation';

class ContentPacksList extends React.Component {
  static propTypes = {
    contentPacks: PropTypes.arrayOf(PropTypes.object),
    contentPackMetadata: PropTypes.object,
    onDeletePack: PropTypes.func,
    onInstall: PropTypes.func,
  };

  static defaultProps = {
    contentPacks: [],
    onDeletePack: () => {
    },
    onInstall: () => {
    },
    contentPackMetadata: {},
  };

  constructor(props) {
    super(props);

    this.state = {
      filteredContentPacks: props.contentPacks,
      pageSize: 10,
      currentPage: 1,
    };

    this._filterContentPacks = this._filterContentPacks.bind(this);
    this._itemsShownChange = this._itemsShownChange.bind(this);
    this._onChangePage = this._onChangePage.bind(this);
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ filteredContentPacks: nextProps.contentPacks });
  }

  _formatItemsNew(items) {
    const { pageSize, currentPage } = this.state;
    const { contentPackMetadata, onDeletePack, onInstall } = this.props;
    const begin = (pageSize * (currentPage - 1));
    const end = begin + pageSize;
    const shownItems = items.slice(begin, end);

    return shownItems.map((item) => (
      <ContentPackListItem key={item.id}
                           pack={item}
                           contentPackMetadata={contentPackMetadata}
                           onDeletePack={onDeletePack}
                           onInstall={onInstall} />
    ));
  }

  _filterContentPacks(filteredItems) {
    this.setState({ filteredContentPacks: filteredItems });
  }

  _itemsShownChange(pageSize) {
    this.setState({ pageSize, currentPage: 1 });
  }

  _onChangePage(nextPage) {
    this.setState({ currentPage: nextPage });
  }

  render() {
    const { filteredContentPacks, pageSize, currentPage } = this.state;
    const { contentPacks } = this.props;
    const numberPages = Math.ceil(filteredContentPacks.length / pageSize);

    const pagination = (
      <Pagination totalPages={numberPages}
                  currentPage={currentPage}
                  onChange={this._onChangePage} />
    );

    const pageSizeSelect = (
      <PageSizeSelect onChange={this._itemsShownChange}
                      pageSize={pageSize}
                      pageSizes={[10, 25, 50, 100]} />
    );

    const noContentMessage = contentPacks.length <= 0
      ? <NoEntitiesExist>No content packs found. Please create or upload one</NoEntitiesExist>
      : <NoSearchResult>No matching content packs have been found</NoSearchResult>;
    const content = filteredContentPacks.length <= 0
      ? (<div className="has-bm">{noContentMessage}</div>)
      : (
        <ControlledTableList>
          <ControlledTableList.Header />
          {this._formatItemsNew(filteredContentPacks)}
        </ControlledTableList>
      );

    return (
      <div>
        <Row className="has-bm">
          <Col md={5}>
            <TypeAheadDataFilter id="content-packs-filter"
                                 label="Filter"
                                 data={contentPacks}
                                 displayKey="name"
                                 onDataFiltered={this._filterContentPacks}
                                 searchInKeys={['name', 'summary']}
                                 filterSuggestions={[]} />
          </Col>
          <Col md={5}>
            {pagination}
          </Col>
          <Col md={2} className="text-right">
            {pageSizeSelect}
          </Col>
        </Row>
        {content}
        <Row className="row-sm">
          <Col md={5} />
          <Col md={5}>
            {pagination}
          </Col>
          <Col md={2} className="text-right">
            {pageSizeSelect}
          </Col>
        </Row>
      </div>
    );
  }
}

export default withLocation(withTelemetry(ContentPacksList));
