// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { GRN } from 'logic/permissions/types';
// import { Pagination, PageSizeSelect } from 'components/common';
import EntityShareState, { type ActiveShares, type AvailableCapabilities, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import Capability from 'logic/permissions/Capability';
import { type ThemeInterface } from 'theme';

import GranteesListItem from './GranteesListItem';

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
`;

const List: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div(({ theme }) => `
  >:nth-child(even) {
    background: ${theme.colors.global.backgroundAlt};
  };
  >:nth-child(odd) {
    background: ${theme.colors.global.background};
  };
`);

// const PaginationWrapper = styled.ul`
//   display: flex;
//   justify-content: center;
// `;

// const StyledPagination = styled(Pagination)`
//   margin-top: 10px;
//   margin-bottom: 0;
// `;

// const StyledPageSizeSelect = styled(PageSizeSelect)(({ theme }) => `
//   label {
//     font-weight: normal;
//     font-size: ${theme.fonts.size.body}
//   }
// `);

type Props = {
  activeShares: ActiveShares,
  availableCapabilities: AvailableCapabilities,
  className?: string,
  entityGRN: GRN,
  onDelete: (GRN) => Promise<EntityShareState>,
  onCapabilityChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    capabilityId: $PropertyType<Capability, 'id'>,
  }) => Promise<EntityShareState>,
  selectedGrantees: SelectedGrantees,
  title: string,
};

// const _paginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
//   const begin = (pageSize * (currentPage - 1));
//   const end = begin + pageSize;

//   return selectedGrantees.slice(begin, end);
// };

const GranteesList = ({ activeShares, onDelete, onCapabilityChange, entityGRN, availableCapabilities, selectedGrantees, className, title }: Props) => {
  // const pageSizes = [10, 50, 100];
  // const [pageSize, setPageSize] = useState(pageSizes[0]);
  // const [currentPage, setCurrentPage] = useState(1);
  // const paginatedGrantees = _paginatedGrantees(selectedGrantees, pageSize, currentPage);
  // const numberPages = Math.ceil(selectedGrantees.size / pageSize);

  return (
    <div className={className}>
      <Header>
        <h5>{title}</h5>
        {/* <StyledPageSizeSelect onChange={(event) => setPageSize(Number(event.target.value))} pageSize={pageSize} pageSizes={pageSizes} /> */}
      </Header>
      <List>
        {selectedGrantees.map((grantee) => {
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
      {/* <PaginationWrapper>
        <StyledPagination totalPages={numberPages}
                          currentPage={currentPage}
                          onChange={setCurrentPage} />
      </PaginationWrapper> */}
    </div>
  );
};

GranteesList.defaultProps = {
  className: undefined,
};

export default GranteesList;
