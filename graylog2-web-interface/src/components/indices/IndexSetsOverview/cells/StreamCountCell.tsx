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

import { CountBadge } from 'components/common';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

import { STREAMS_SECTION } from '../Constants';
import type { IndexSetEntity } from '../types';

type Props = {
  indexSet: IndexSetEntity;
};

const StreamCountCell = ({ indexSet }: Props) => {
  const { toggleSection, expandedSections } = useExpandedSections();
  const isOpen = expandedSections?.[indexSet.id]?.includes(STREAMS_SECTION) ?? false;

  return (
    <CountBadge
      count={indexSet.stream_count}
      iconName={isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(indexSet.id, STREAMS_SECTION)}
      title={`${isOpen ? 'Hide' : 'Show'} associated streams for ${indexSet.title}`}
    />
  );
};

export default StreamCountCell;
