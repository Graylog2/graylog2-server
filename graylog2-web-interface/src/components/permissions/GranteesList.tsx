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
// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled, { StyledComponent } from 'styled-components';
import { $PropertyType } from 'utility-types';

import SharedEntity from 'logic/permissions/SharedEntity';
import { Pagination, PageSizeSelect } from 'components/common';
import { Alert } from 'components/graylog';
import EntityShareState, { ActiveShares, CapabilitiesList, SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Capability from 'logic/permissions/Capability';
import { ThemeInterface } from 'theme';

import GranteesListItem from './GranteesListItem';

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
`;

const List: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  >:nth-child(even) {
    background: ${theme.colors.table.backgroundAlt};
  };
  >:nth-child(odd) {
    background: ${theme.colors.table.background};
  };
`);

const PaginationWrapper = styled.ul`
  display: flex;
  justify-content: center;
  .pagination {
    margin: 10px 0 0 0;
  }
`;

const StyledPagination = styled(Pagination)`
  margin-top: 10px;
  margin-bottom: 0;
`;

const StyledPageSizeSelect = styled(PageSizeSelect)(({ theme }) => `
  label {
    font-weight: normal;
    font-size: ${theme.fonts.size.body}
  }
`);

type Props = {
  activeShares: ActiveShares,
  availableCapabilities: CapabilitiesList,
  className?: string,
  entityType: $PropertyType<SharedEntity, 'type'>,
  onDelete: (GRN) => Promise<EntityShareState | undefined | null>,
  onCapabilityChange: (payload: {
    granteeId: $PropertyType<Grantee, 'id'>,
    capabilityId: $PropertyType<Capability, 'id'>,
  }) => Promise<EntityShareState | undefined | null>,
  selectedGrantees: SelectedGrantees,
  title: string,
};

const _paginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
  const begin = (pageSize * (currentPage - 1));
  const end = begin + pageSize;

  return selectedGrantees.slice(begin, end);
};

const GranteesList = ({ activeShares, onDelete, onCapabilityChange, entityType, availableCapabilities, selectedGrantees, className, title }: Props) => {
  const initialPageSize = PageSizeSelect.defaultPageSizes[0];
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [currentPage, setCurrentPage] = useState(1);
  const paginatedGrantees = _paginatedGrantees(selectedGrantees, pageSize, currentPage);
  const totalGrantees = selectedGrantees.size;
  const totalPages = Math.ceil(totalGrantees / pageSize);
  const showPageSizeSelect = totalGrantees > initialPageSize;

  return (
    <div className={className}>
      <Header>
        <h5>{title}</h5>
        {showPageSizeSelect && (
          <StyledPageSizeSelect onChange={(event) => setPageSize(Number(event.target.value))} pageSize={pageSize} />
        )}
      </Header>
      {paginatedGrantees.size > 0 ? (
        <List>
          {paginatedGrantees.map((grantee) => {
            const currentGranteeState = grantee.currentState(activeShares);

            return (
              <GranteesListItem availableCapabilities={availableCapabilities}
                                currentGranteeState={currentGranteeState}
                                grantee={grantee}
                                key={grantee.id}
                                onDelete={onDelete}
                                onCapabilityChange={onCapabilityChange} />
            );
          })}
        </List>
      ) : (
        <Alert>This {entityType} has no collaborators.</Alert>
      )}
      <PaginationWrapper>
        <StyledPagination totalPages={totalPages}
                          currentPage={currentPage}
                          onChange={setCurrentPage} />
      </PaginationWrapper>
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
