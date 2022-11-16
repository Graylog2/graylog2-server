import React from 'react';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import EntityItem from 'components/welcome/EntityListItem';
import { useLastOpen } from 'components/welcome/hooks';
import { Spinner } from 'components/common';

const LastOpenList = () => {
  const { data: { lastOpen }, isFetching } = useLastOpen(DEFAULT_PAGINATION);

  return isFetching ? <Spinner /> : (
    <ListGroup>
      {lastOpen.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
    </ListGroup>
  );
};

export default LastOpenList;
