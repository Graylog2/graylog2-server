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

import { Button, Col, Row, Table } from 'components/bootstrap';
import { Icon, SortIcon, PaginatedList, NoSearchResult, NoEntitiesExist } from 'components/common';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import SidecarRow from './SidecarRow';
import style from './SidecarList.css';

const StyledSortIcon = styled(SortIcon)`
  && {
    width: 12px;
    margin-left: 5px;
    line-height: 1;
  }
`;

export const PAGE_SIZES = [10, 25, 50, 100];

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
              <th key={sidecar}>
                {sidecarCollection[sidecar]}
                <StyledSortIcon activeDirection={sort.field === sidecar ? sort.order : null} onChange={onSortChange(sidecar)} ascId="asc" descId="desc" />
              </th>
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

  formatNoMatchingListAlert = () => {
    const { onlyActive } = this.props;
    const showInactiveHint = (onlyActive ? ' and/or click on "Include inactive sidecars"' : null);

    return (
      <NoSearchResult>
        <Icon name="info-circle" />&nbsp;There are no sidecars matching the search criteria. Try adjusting your search filter{showInactiveHint}.
      </NoSearchResult>
    );
  };

  renderEmptyList = () => {
    const { query } = this.props;

    if (query) {
      return this.formatNoMatchingListAlert();
    }

    return (
      <NoEntitiesExist>
        There are no sidecars configured.
      </NoEntitiesExist>
    );
  };

  render() {
    const { sidecars, onlyActive, pagination, query, onQueryChange, onPageChange, toggleShowInactive } = this.props;
    const sidecarRows = sidecars.map((sidecar) => <SidecarRow key={sidecar.node_id} sidecar={sidecar} />);
    const showOrHideInactive = (onlyActive ? 'Include' : 'Hide');
    const sidecarList = (sidecarRows.length > 0 ? this.formatSidecarList(sidecarRows) : this.renderEmptyList());

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

        <PaginatedList pageSizes={PAGE_SIZES}
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
