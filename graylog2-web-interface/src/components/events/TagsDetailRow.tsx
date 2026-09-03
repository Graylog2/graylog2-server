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

import DefinitionList from 'components/common/DefinitionList';
import ChipsCell from 'components/common/ChipsCell';
import useAppendTagFilter from 'components/events/useAppendTagFilter';

type Props = {
  tags: ReadonlyArray<string> | undefined | null;
  // Tag chips only make sense as filter shortcuts when a filterable list is in scope.
  // Set to false in contexts without one (e.g. widgets, investigation evidence) to avoid
  // rendering clickable chips that just pollute the URL with an unused `filter` param.
  interactive?: boolean;
};

const TagsDetailRow = ({ tags, interactive = true }: Props) => {
  const onTagClick = useAppendTagFilter();

  if (!tags?.length) return null;

  return (
    <DefinitionList>
      <dt>Tags</dt>
      <dd>
        {interactive ? (
          <ChipsCell items={tags} truncate={false} onItemClick={onTagClick} itemLabel="tag" />
        ) : (
          <ChipsCell items={tags} truncate={false} />
        )}
      </dd>
    </DefinitionList>
  );
};

export default TagsDetailRow;
