import React, { useCallback, useState } from 'react';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import EntityItem from 'components/welcome/EntityListItem';
import { PaginatedList, Spinner } from 'components/common';
import { usePinnedItems } from 'components/welcome/hooks';

const PinnedItemsList = () => {
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { data: { pinnedItems, pagination: { total } }, isFetching } = usePinnedItems(pagination);
  const onPageChange = useCallback((newPage) => {
    setPagination((cur) => ({ ...cur, page: newPage }));
  }, [setPagination]);

  return (
    <PaginatedList onChange={onPageChange} useQueryParameter={false} activePage={pagination.page} totalItems={total} pageSize={pagination.per_page} showPageSizeSelect={false} hideFirstAndLastPageLinks>
      {isFetching ? <Spinner /> : (
        <ListGroup>
          {pinnedItems.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
        </ListGroup>
      )}
    </PaginatedList>
  );
};

export default PinnedItemsList;
