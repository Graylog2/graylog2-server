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
import TagsCell from 'components/events/TagsCell';
import useAppendTagFilter from 'components/events/useAppendTagFilter';

type Props = {
  tags: ReadonlyArray<string> | undefined | null;
};

const TagsDetailRow = ({ tags }: Props) => {
  const onTagClick = useAppendTagFilter();

  if (!tags?.length) return null;

  return (
    <DefinitionList>
      <dt>Tags</dt>
      <dd>
        <TagsCell tags={tags} truncate={false} onTagClick={onTagClick} />
      </dd>
    </DefinitionList>
  );
};

export default TagsDetailRow;
