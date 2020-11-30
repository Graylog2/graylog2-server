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
// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { IconButton } from 'components/common';
import SearchLink from 'components/search/SearchLink';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import DrilldownContext from '../contexts/DrilldownContext';

const NeutralLink: StyledComponent<{}, {}, HTMLAnchorElement> = styled.a`
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

const ReplaySearchButton = () => {
  const { query, timerange, streams } = useContext(DrilldownContext);
  const searchLink = buildSearchLink(timerange, query.query_string, streams);

  return (
    <NeutralLink href={searchLink} target="_blank" rel="noopener noreferrer" title="Replay search">
      <IconButton name="play" focusable={false} />
    </NeutralLink>
  );
};

export default ReplaySearchButton;
