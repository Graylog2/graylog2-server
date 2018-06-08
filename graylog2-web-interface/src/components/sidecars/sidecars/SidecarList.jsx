import React from 'react';
import PropTypes from 'prop-types';
import { Alert, Button, Col, Row, Table } from 'react-bootstrap';

import { PaginatedList } from 'components/common';

import SidecarRow from './SidecarRow';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import style from './SidecarList.css';

const SidecarList = React.createClass({
  propTypes: {
    sidecars: PropTypes.array.isRequired,
    onlyActive: PropTypes.bool.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    sort: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onSortChange: PropTypes.func.isRequired,
    toggleShowInactive: PropTypes.func.isRequired,
  },

  getTableHeaderClassName(field) {
    return (this.props.sort.field === field ? `sort-${this.props.sort.order}` : 'sortable');
  },

  formatSidecarList(sidecars) {
    const { onSortChange } = this.props;

    return (
      <Table striped responsive className={style.sidecarList}>
        <thead>
          <tr>
            <th className={this.getTableHeaderClassName('node_name')}
                onClick={onSortChange('node_name')}>Name
            </th>
            <th className={this.getTableHeaderClassName('node_details.status.status')}
                onClick={onSortChange('node_details.status.status')}>
              Status
            </th>
            <th className={this.getTableHeaderClassName('node_details.operating_system')}
                onClick={onSortChange('node_details.operating_system')}>
              Operating System
            </th>
            <th className={this.getTableHeaderClassName('last_seen')}
                onClick={onSortChange('last_seen')}>Last Seen
            </th>
            <th className={this.getTableHeaderClassName('node_id')}
                onClick={onSortChange('node_id')}>
              Node Id
            </th>
            <th className={this.getTableHeaderClassName('collector_version')}
                onClick={onSortChange('collector_version')}>
              Sidecar Version
            </th>
            <th className={style.actions}>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {sidecars}
        </tbody>
      </Table>
    );
  },

  formatEmptyListAlert() {
    const showInactiveHint = (this.props.onlyActive ? ' and/or click on "Include inactive sidecars"' : null);
    return <Alert>There are no sidecars to show. Try adjusting your search filter{showInactiveHint}.</Alert>;
  },

  render() {
    const { sidecars, onlyActive, pagination, query, onQueryChange, onPageChange, toggleShowInactive } = this.props;
    const sidecarRows = sidecars.map(sidecar => <SidecarRow key={sidecar.node_id} sidecar={sidecar} />);
    const showOrHideInactive = (onlyActive ? 'Include' : 'Hide');
    const sidecarList = (sidecarRows.length > 0 ? this.formatSidecarList(sidecarRows) : this.formatEmptyListAlert());

    return (
      <div>
        <div className={style.sidecarsFilter}>
          <SidecarSearchForm query={query}
                             onSearch={onQueryChange}
                             onReset={onQueryChange}>
            <Button bsStyle="primary"
                    onClick={toggleShowInactive}
                    className={style.inactiveSidecarsButton}>
              {showOrHideInactive} inactive sidecars
            </Button>
          </SidecarSearchForm>
        </div>

        <PaginatedList activePage={pagination.page}
                       pageSize={pagination.pageSize}
                       pageSizes={[10, 25, 50, 100]}
                       totalItems={pagination.total}
                       onChange={onPageChange}>
          <Row>
            <Col md={12}>
              {sidecarList}
            </Col>
          </Row>
        </PaginatedList>
      </div>
    );
  },
});

export default SidecarList;
