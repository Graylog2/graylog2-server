// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import SharedEntity from 'logic/permissions/SharedEntity';
import type { GRN } from 'logic/permissions/types';
import { Pagination, PageSizeSelect } from 'components/common';
import { Alert } from 'components/graylog';
import EntityShareState, { type ActiveShares, type CapabilitiesList, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Capability from 'logic/permissions/Capability';
import { type ThemeInterface } from 'theme';

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
    margin 10px 0 0 0;
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
  entityGRN: GRN,
  entityType: $PropertyType<SharedEntity, 'type'>,
  onDelete: (GRN) => Promise<?EntityShareState>,
  onCapabilityChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    capabilityId: $PropertyType<Capability, 'id'>,
  }) => Promise<?EntityShareState>,
  selectedGrantees: SelectedGrantees,
  title: string,
};

const _paginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
  const begin = (pageSize * (currentPage - 1));
  const end = begin + pageSize;

  return selectedGrantees.slice(begin, end);
};

const GranteesList = ({ activeShares, onDelete, onCapabilityChange, entityGRN, entityType, availableCapabilities, selectedGrantees, className, title }: Props) => {
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
                                entityGRN={entityGRN}
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
