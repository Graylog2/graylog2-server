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

import { ButtonToolbar, MenuItem } from 'components/bootstrap';
import { IconButton, IfPermitted, LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';
import Routes from 'routing/Routes';

import { NOT_APPLICABLE_REASON, rowMenu } from './actions';
import type { IndexRow } from './fetchIndices';
import IndexActionConfirm from './IndexActionConfirm';
import type { IndexAction } from './types';
import useCanRun from './useCanRun';
import useIndexActions from './useIndexActions';

// All time: a search only reaches indices whose time range it overlaps, and the query already narrows it to one.
export const indexSearchUrl = (index: string) => Routes.search(`_index:${index}`, { relative: 0 });

// Searches the index's messages, as the index sets' rows do for a whole index set. A closed index can't be searched.
const SearchButton = ({ index }: { index: IndexRow }) => {
  const closed = index.status === 'close';
  const button = (
    <IconButton
      name="search"
      title={closed ? `${index.index} is closed and can't be searched` : `Search in index ${index.index}`}
      bsStyle="default"
      size="xsmall"
      disabled={closed}
    />
  );

  return (
    <IfPermitted permissions="searches:relative">
      {closed ? button : <LinkContainer to={indexSearchUrl(index.index)}>{button}</LinkContainer>}
    </IfPermitted>
  );
};

// A search button, and the "More" menu of one row: only actions permitted on that index; no menu if there are none.
const IndexRowActions = ({ index }: { index: IndexRow }) => {
  const can = useCanRun();
  const { runAction, isRunning } = useIndexActions();
  const [pending, setPending] = useState<IndexAction | undefined>(undefined);
  const items = rowMenu(index, can);

  if (items.length === 0) {
    return <SearchButton index={index} />;
  }

  const confirm = async () => {
    try {
      await runAction({ action: pending, indices: [index] });
    } finally {
      setPending(undefined);
    }
  };

  return (
    <ButtonToolbar>
      <SearchButton index={index} />
      <MoreActions>
        {items.map(({ action, applies }) => (
          <MenuItem
            key={action.key}
            disabled={!applies}
            title={applies ? undefined : NOT_APPLICABLE_REASON}
            onClick={() => setPending(action)}>
            {action.label}
          </MenuItem>
        ))}
      </MoreActions>
      {pending && (
        <IndexActionConfirm
          action={pending}
          targets={[index]}
          skipped={0}
          isRunning={isRunning}
          onConfirm={confirm}
          onCancel={() => setPending(undefined)}
        />
      )}
    </ButtonToolbar>
  );
};

export default IndexRowActions;
