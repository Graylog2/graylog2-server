import React from 'react';

import { ListGroup } from 'components/bootstrap';
import { DEFAULT_PAGINATION } from 'components/welcome/helpers';
import EntityItem from 'components/welcome/EntityListItem';
import { useLastOpened } from 'components/welcome/hooks';
import { Spinner } from 'components/common';

const LastOpenList = () => {
  const { data: { lastOpened }, isFetching } = useLastOpened(DEFAULT_PAGINATION);

  if (isFetching) return <Spinner />;
  if (!lastOpened.length) return <i>There are no last opened items</i>;

  return (
    <ListGroup data-testid="last-opened-list">
      {lastOpened.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
    </ListGroup>
  );
};

export default LastOpenList;
