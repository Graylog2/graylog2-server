import React, { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import { Table } from 'components/bootstrap';
import { DEFAULT_PAGINATION, typeLinkMap } from 'components/welcome/helpers';
import { PaginatedList, Spinner } from 'components/common';
import { relativeDifference } from 'util/DateTime';
import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import { StyledLabel } from 'components/welcome/EntityListItem';
import { useRecentActivities } from 'components/welcome/hooks';

const ActionItemLink = styled(Link)(({ theme }) => css`
  color: ${theme.colors.variant.primary};
  &:hover {
    color: ${theme.colors.variant.darker.primary};
  }
`);

const ActionItem = ({ action, id, entityType, entityName }) => {
  return (
    <span>
      {`${action.actionUser} ${action.actionType} ${entityType}`}
      {' '}
      <ActionItemLink target="_blank" to={Routes.pluginRoute(typeLinkMap[entityType].link)(id)}>{entityName}</ActionItemLink>
      {' '}
      {`with ${action.actedUser}`}
    </span>
  );
};

const RecentActivityList = () => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: { resentActivities, pagination: { total } }, isFetching } = useRecentActivities(pagination);
  const onPageChange = useCallback((newPage) => {
    setPagination((cur) => ({ ...cur, page: newPage }));
  }, [setPagination]);

  return (
    <PaginatedList onChange={onPageChange} useQueryParameter={false} activePage={pagination.page} totalItems={total} pageSize={pagination.per_page} showPageSizeSelect={false} hideFirstAndLastPageLinks>
      {isFetching ? <Spinner /> : (
        <Table striped>
          <tbody>
            {
              resentActivities.map(({ timestamp, action, id, entityName, entityType }) => {
                return (
                  <tr key={id}>
                    <td style={{ width: 100 }}>
                      <StyledLabel title={timestamp} bsStyle="primary">{relativeDifference(timestamp)}
                      </StyledLabel>
                    </td>
                    <td>
                      <ActionItem id={id} action={action} entityName={entityName} entityType={entityType} />
                    </td>
                  </tr>
                );
              })
        }
          </tbody>
        </Table>
      )}
    </PaginatedList>
  );
};

export default RecentActivityList;
