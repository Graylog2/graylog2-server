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
import styled, { css } from 'styled-components';
import capitalize from 'lodash/capitalize';

import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { useStore } from 'stores/connect';
import EntityShareStore from 'stores/permissions/EntityShareStore';
import type { SelectedGrantees } from 'logic/permissions/EntityShareState';
import { DEFAULT_PAGE_SIZES } from 'hooks/usePaginationQueryParameter';
import {
  GranteeInfo,
  GranteeListItemContainer,
  GranteeListItemTitle,
  StyledGranteeIcon,
} from 'components/permissions/CommonStyledComponents';
import { PageSizeSelect, Pagination } from 'components/common';
import { Alert } from 'components/bootstrap';

type Props = {
  shareState?: EntitySharePayload;
};
const List = styled.div(
  ({ theme }) => `
  >:nth-child(even) {
    background: ${theme.colors.table.row.backgroundStriped};
  };

  >:nth-child(odd) {
    background: ${theme.colors.table.row.background};
  };
`,
);
const StyledPageSizeSelect = styled(PageSizeSelect)(
  ({ theme }) => css`
    label {
      font-weight: normal;
      font-size: ${theme.fonts.size.body};
    }
  `,
);
const PaginationWrapper = styled.ul`
  display: flex;
  justify-content: center;

  .pagination {
    margin: 10px 0;
  }
`;

const StyledPagination = styled(Pagination)`
  margin-top: 10px;
  margin-bottom: 0;
`;
const StyledDiv = styled.div`
  flex: 1;
`;

const getPaginatedGrantees = (selectedGrantees: SelectedGrantees, pageSize: number, currentPage: number) => {
  const begin = pageSize * (currentPage - 1);
  const end = begin + pageSize;

  return selectedGrantees.slice(begin, end);
};

const ShareDetails = ({ shareState = null }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);

  const initialPageSize = DEFAULT_PAGE_SIZES[0];
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [currentPage, setCurrentPage] = useState(1);

  if (!shareState) {
    return null;
  }

  const activeShares = entityShareState?.activeShares;
  const selectedGrantees = entityShareState?.selectedGrantees;
  const paginatedGrantees = getPaginatedGrantees(selectedGrantees, pageSize, currentPage);
  const totalGrantees = selectedGrantees.size;
  const totalPages = Math.ceil(totalGrantees / pageSize);
  const showPageSizeSelect = totalGrantees > initialPageSize;

  return (
    <>
      <h3>Share</h3>
      <p>Collaborators</p>
      {showPageSizeSelect && (
        <StyledPageSizeSelect onChange={(newPageSize) => setPageSize(newPageSize)} pageSize={pageSize} />
      )}
      {paginatedGrantees.size > 0 ? (
        <List>
          {paginatedGrantees
            .map((grantee) => {
              const currentGranteeState = grantee.currentState(activeShares);

              return (
                <GranteeListItemContainer $currentState={currentGranteeState}>
                  <GranteeInfo title={grantee.title}>
                    <StyledGranteeIcon type={grantee.type} />
                    <GranteeListItemTitle>{grantee.title}</GranteeListItemTitle>
                  </GranteeInfo>
                  <StyledDiv>{capitalize(grantee.capabilityId)}</StyledDiv>
                </GranteeListItemContainer>
              );
            })
            .toArray()}
        </List>
      ) : (
        <Alert>This Event definition has no collaborators.</Alert>
      )}
      <PaginationWrapper>
        <StyledPagination totalPages={totalPages} currentPage={currentPage} onChange={setCurrentPage} />
      </PaginationWrapper>
    </>
  );
};

export default ShareDetails;
