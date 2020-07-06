import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { Col, Row, Button } from 'components/graylog';
import { DataTable, PaginatedList, SearchForm } from 'components/common';
import Routes from 'routing/Routes';

import CollectorRow from './CollectorRow';
import style from './CollectorList.css';

const CollectorList = createReactClass({
  propTypes: {
    collectors: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    total: PropTypes.number.isRequired,
    onClone: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    validateCollector: PropTypes.func.isRequired,
  },

  headerCellFormatter(header) {
    const className = (header === 'Actions' ? style.actionsColumn : '');

    return <th className={className}>{header}</th>;
  },

  collectorFormatter(collector) {
    const { onClone, onDelete, validateCollector } = this.props;

    return (
      <CollectorRow collector={collector}
                    onClone={onClone}
                    onDelete={onDelete}
                    validateCollector={validateCollector} />
    );
  },

  render() {
    const { collectors, pagination, query, total, onPageChange, onQueryChange } = this.props;

    const headers = ['Name', 'Operating System', 'Actions'];

    return (
      <div>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <LinkContainer to={Routes.SYSTEM.SIDECARS.NEW_COLLECTOR}>
                <Button bsStyle="success" bsSize="small">Create Log Collector</Button>
              </LinkContainer>
            </div>
            <h2>Log Collectors <small>{total} total</small></h2>
          </Col>
          <Col md={12}>
            <p>Manage Log Collectors that you can configure and supervise through Graylog Sidecar and Graylog Web Interface.</p>
          </Col>
        </Row>

        <Row className={`row-sm ${style.collectorRow}`}>
          <Col md={12}>
            <SearchForm query={query}
                        onSearch={onQueryChange}
                        onReset={onQueryChange}
                        searchButtonLabel="Find"
                        placeholder="Find collectors"
                        wrapperClass={style.inline}
                        queryWidth={300}
                        topMargin={0}
                        useLoadingState />

            <PaginatedList activePage={pagination.page}
                           pageSize={pagination.pageSize}
                           pageSizes={[10, 25]}
                           totalItems={pagination.total}
                           onChange={onPageChange}>
              <div className={style.collectorTable}>
                <DataTable id="collector-list"
                           className="table-hover"
                           headers={headers}
                           headerCellFormatter={this.headerCellFormatter}
                           rows={collectors}
                           dataRowFormatter={this.collectorFormatter}
                           noDataText="There are no log collectors to display, why don't you create one?"
                           filterLabel=""
                           filterKeys={[]}
                           useResponsiveTable={false} />
              </div>
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  },
});

export default CollectorList;
