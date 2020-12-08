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
import styled from 'styled-components';

import { Button, Alert, Col, Row, Table } from 'components/graylog';
import { Icon, PaginatedList } from 'components/common';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import SidecarRow from './SidecarRow';
import style from './SidecarList.css';

const SortableIcon = styled(Icon)`
  && {
    width: 12px;
    margin-left: 5px;
    line-height: 1;
  }
`;

const SortableTH = styled.th`
  cursor: pointer;

  ${SortableIcon} {
    visibility: ${(props) => (props.sorted ? 'visible' : 'hidden')};
  }

  &:hover {
    ${SortableIcon} {
      visibility: visible;
    }
  }
`;

class SidecarList extends React.Component {
  static propTypes = {
    sidecars: PropTypes.array.isRequired,
    onlyActive: PropTypes.bool.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    sort: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onSortChange: PropTypes.func.isRequired,
    toggleShowInactive: PropTypes.func.isRequired,
  };

  _getTableHeaderSortIcon = (field) => {
    const { sort } = this.props;
    const iconSort = {
      asc: 'sort-amount-down',
      desc: 'sort-amount-up',
    };

    return (sort.field === field ? iconSort[sort.order] : 'sort');
  };

  formatSidecarList = (sidecars) => {
    const { onSortChange, sort } = this.props;
    const sidecarCollection = {
      node_name: 'Name',
      'node_details.status.status': 'Status',
      'node_details.operating_system': 'Operating System',
      last_seen: 'Last Seen',
      node_id: 'Node Id',
      sidecar_version: 'Sidecar Version',
    };

    return (
      <Table striped responsive className={style.sidecarList}>
        <thead>
          <tr>
            {Object.keys(sidecarCollection).map((sidecar) => (
              <SortableTH onClick={onSortChange(sidecar)} sorted={sort.field === sidecar} key={sidecar}>
                {sidecarCollection[sidecar]} <SortableIcon name={this._getTableHeaderSortIcon(sidecar)} />
              </SortableTH>
            ))}
            <th className={style.actions}>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {sidecars}
        </tbody>
      </Table>
    );
  };

  formatEmptyListAlert = () => {
    const { onlyActive } = this.props;
    const showInactiveHint = (onlyActive ? ' and/or click on "Include inactive sidecars"' : null);

    return <Alert>There are no sidecars to show. Try adjusting your search filter{showInactiveHint}.</Alert>;
  };

  render() {
    const { sidecars, onlyActive, pagination, query, onQueryChange, onPageChange, toggleShowInactive } = this.props;
    const sidecarRows = sidecars.map((sidecar) => <SidecarRow key={sidecar.node_id} sidecar={sidecar} />);
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
  }
}

export default SidecarList;
