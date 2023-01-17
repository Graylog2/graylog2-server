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
import { PaginatedList, NoSearchResult, NoEntitiesExist, SortIcon } from 'components/common';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import SidecarFailureTrackingRows from './SidecarFailureTrackingRows';

const StyledSortIcon = styled(SortIcon)`
  && {
    width: 12px;
    margin-left: 5px;
    line-height: 1;
  }
`;

export const PAGE_SIZES = [10, 25, 50, 100];

const SidecarFailureTrackingList = ({
  sidecars,
  pagination,
  query,
  sort,
  onlyActive,
  onPageChange,
  onQueryChange,
  onSortChange,
  toggleShowInactive,
}) => {
  const formatSidecarList = (sidecarRows: React.ReactNode[]) => {
    const sidecarCollection = {
      node_name: 'Name',
      last_seen: 'Last Seen',
      'node_details.status.status': 'Status',
      message: 'Error Message',
      verbose_message: 'Verbose Message',
    };

    return (
      <Table striped responsive>
        <thead>
          <tr>
            {Object.keys(sidecarCollection).map((sort_key) => (
              <th key={sort_key}>
                {sidecarCollection[sort_key]}
                {['node_name', 'last_seen'].includes(sort_key) && (
                  <StyledSortIcon activeDirection={sort.field === sort_key ? sort.order : null}
                                  onChange={() => onSortChange(sort_key)}
                                  ascId="asc"
                                  descId="desc" />
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sidecarRows}
        </tbody>
      </Table>
    );
  };

  const formatNoMatchingListAlert = () => {
    const showInactiveHint = (onlyActive ? ' and/or click on "Include inactive sidecars"' : null);

    return (
      <NoSearchResult>
        There are no sidecars matching the search criteria. Try adjusting your search filter{showInactiveHint}.
      </NoSearchResult>
    );
  };

  const renderEmptyList = () => {
    if (query) {
      return formatNoMatchingListAlert();
    }

    return (
      <NoEntitiesExist>
        There are no sidecars with failures.
      </NoEntitiesExist>
    );
  };

  const sidecarRows = sidecars.map((sidecar) => <SidecarFailureTrackingRows key={sidecar.node_id} sidecar={sidecar} />);
  const showOrHideInactive = (onlyActive ? 'Include' : 'Hide');
  const sidecarList = (sidecarRows.length > 0 ? formatSidecarList(sidecarRows) : renderEmptyList());

  return (
    <div>
      <div>
        <SidecarSearchForm query={query}
                           onSearch={onQueryChange}
                           onReset={onQueryChange}>
          <Button bsStyle="primary"
                  onClick={toggleShowInactive}>
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
};

SidecarFailureTrackingList.propTypes = {
  sidecars: PropTypes.array.isRequired,
  pagination: PropTypes.object.isRequired,
  query: PropTypes.string.isRequired,
  sort: PropTypes.object.isRequired,
  onlyActive: PropTypes.bool.isRequired,
  onPageChange: PropTypes.func.isRequired,
  onQueryChange: PropTypes.func.isRequired,
  onSortChange: PropTypes.func.isRequired,
  toggleShowInactive: PropTypes.func.isRequired,
};

export default SidecarFailureTrackingList;
