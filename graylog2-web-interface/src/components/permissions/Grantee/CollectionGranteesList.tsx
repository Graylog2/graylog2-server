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
import * as React from 'react';
import { useState } from 'react';

import type SharedEntity from 'logic/permissions/SharedEntity';
import { Alert } from 'components/bootstrap';
import type { ActiveShares, SelectedGrantees } from 'logic/permissions/EntityShareState';
import { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';
import CollectionGranteesListItem from 'components/permissions/Grantee/CollectionGranteesListItem';
import {
  StyledGranteeList,
  GranteeListHeader,
  GranteeListPaginationWrapper,
  GranteeListStyledPagination,
  GranteeListStyledPageSizeSelect,
} from 'components/permissions/CommonStyledComponents';

type Props = {
  activeShares: ActiveShares;
  className?: string;
  entityType: SharedEntity['type'];
  selectedGrantees: SelectedGrantees;
  title: string;
  entityTypeTitle?: string | null | undefined;
};

const _paginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
  const begin = pageSize * (currentPage - 1);
  const end = begin + pageSize;

  return selectedGrantees.slice(begin, end);
};

const CollectionGranteesList = ({
  activeShares,
  entityType,
  entityTypeTitle = null,
  selectedGrantees,
  className = null,
  title,
}: Props) => {
  const initialPageSize = DEFAULT_PAGE_SIZES[0];
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [currentPage, setCurrentPage] = useState(1);
  const paginatedGrantees = _paginatedGrantees(selectedGrantees, pageSize, currentPage);
  const totalGrantees = selectedGrantees.size;
  const totalPages = Math.ceil(totalGrantees / pageSize);
  const showPageSizeSelect = totalGrantees > initialPageSize;

  return (
    <div className={className}>
      <GranteeListHeader>
        <h5>{title}</h5>
        {showPageSizeSelect && (
          <GranteeListStyledPageSizeSelect onChange={(newPageSize) => setPageSize(newPageSize)} pageSize={pageSize} />
        )}
      </GranteeListHeader>
      {paginatedGrantees.size > 0 ? (
        <StyledGranteeList>
          {paginatedGrantees
            .map((grantee) => {
              const currentGranteeState = grantee.currentState(activeShares);

              return <CollectionGranteesListItem currentGranteeState={currentGranteeState} grantee={grantee} />;
            })
            .toArray()}
        </StyledGranteeList>
      ) : (
        <Alert>This {entityTypeTitle || entityType} has no collaborators.</Alert>
      )}
      <GranteeListPaginationWrapper>
        <GranteeListStyledPagination totalPages={totalPages} currentPage={currentPage} onChange={setCurrentPage} />
      </GranteeListPaginationWrapper>
    </div>
  );
};

export default CollectionGranteesList;
