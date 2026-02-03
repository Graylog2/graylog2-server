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
import type { ActiveShares, CapabilitiesList, SelectedGrantees } from 'logic/permissions/EntityShareState';
import type EntityShareState from 'logic/permissions/EntityShareState';
import type Grantee from 'logic/permissions/Grantee';
import type Capability from 'logic/permissions/Capability';
import { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';
import {
  GranteeListHeader,
  GranteeListStyledPageSizeSelect,
  GranteeListStyledPagination,
  GranteeListPaginationWrapper,
  StyledGranteeList,
} from 'components/permissions/CommonStyledComponents';

import GranteesListItem from './GranteesListItem';
import CreateGranteesListItem from './CreateGranteesListItem';

type Props = {
  activeShares: ActiveShares;
  availableCapabilities: CapabilitiesList;
  className?: string;
  entityType: SharedEntity['type'];
  onDelete: (GRN) => Promise<EntityShareState | undefined | null>;
  onCapabilityChange: (payload: {
    granteeId: Grantee['id'];
    capabilityId: Capability['id'];
  }) => Promise<EntityShareState | undefined | null>;
  selectedGrantees: SelectedGrantees;
  title: string;
  entityTypeTitle?: string | null | undefined;
  isCreating?: boolean;
};

const _paginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
  const begin = pageSize * (currentPage - 1);
  const end = begin + pageSize;

  return selectedGrantees.slice(begin, end);
};

const GranteesList = ({
  activeShares,
  onDelete,
  onCapabilityChange,
  entityType,
  entityTypeTitle = null,
  availableCapabilities,
  selectedGrantees,
  className = null,
  title,
  isCreating = false,
}: Props) => {
  const initialPageSize = DEFAULT_PAGE_SIZES[0];
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [currentPage, setCurrentPage] = useState(1);
  const paginatedGrantees = _paginatedGrantees(selectedGrantees, pageSize, currentPage);
  const totalGrantees = selectedGrantees.size;
  const totalPages = Math.ceil(totalGrantees / pageSize);
  const showPageSizeSelect = totalGrantees > initialPageSize;
  const ItemComponent = isCreating ? CreateGranteesListItem : GranteesListItem;

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

              return (
                <ItemComponent
                  availableCapabilities={availableCapabilities}
                  currentGranteeState={currentGranteeState}
                  grantee={grantee}
                  key={grantee.id}
                  onDelete={onDelete}
                  onCapabilityChange={onCapabilityChange}
                />
              );
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

export default GranteesList;
