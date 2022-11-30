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
import { useContext } from 'react';
import styled from 'styled-components';

import { IconButton } from 'components/common';
import SearchLink from 'components/search/SearchLink';
import type { TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import DrilldownContext from '../contexts/DrilldownContext';

const NeutralLink = styled.a`
  color: inherit;
  text-decoration: none;

  &:visited {
    color: inherit;
  }
`;

const buildSearchLink = (timerange, query, streams) => SearchLink.builder()
  .query(createElasticsearchQueryString(query))
  .timerange(timerange)
  .streams(streams)
  .build()
  .toURL();

type Props = {
  query?: string | undefined,
  timerange?: TimeRange | undefined,
  streams?: string[] | undefined,
};

const ReplaySearchButton = ({ query: queryProp, timerange: timerangeProp, streams: streamsProp }: Props) => {
  const { query, timerange, streams } = useContext(DrilldownContext);
  let searchLink;

  if (queryProp === undefined && timerangeProp === undefined && streamsProp === undefined) {
    searchLink = buildSearchLink(timerange, query.query_string, streams);
  } else {
    searchLink = buildSearchLink(timerangeProp, queryProp, streamsProp);
  }

  return (
    <NeutralLink href={searchLink} target="_blank" rel="noopener noreferrer" title="Replay search">
      <IconButton name="play" focusable={false} />
    </NeutralLink>
  );
};

ReplaySearchButton.defaultProps = {
  query: undefined,
  timerange: undefined,
  streams: undefined,
};

export default ReplaySearchButton;
