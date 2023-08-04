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
import styled from 'styled-components';
import { useCallback, useMemo } from 'react';
import type * as Immutable from 'immutable';
import URI from 'urijs';

import IconButton from 'components/common/IconButton';
import Icon from 'components/common/Icon';
import SearchLink from 'views/logic/search/SearchLink';
import Store from 'logic/local-storage/Store';
import type { TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import generateId from 'logic/generateId';
import type Parameter from 'views/logic/parameters/Parameter';
import type { ParameterBindings } from 'views/logic/search/SearchExecutionState';

const NeutralLink = styled.a`
  display: inline-flex;
  align-items: center;
  color: inherit;
  text-decoration: none;

  &:visited {
    color: inherit;
  }
`;

const StyledIcon = styled(Icon)`
  margin-left: 6px;
`;

const buildSearchLink = (
  sessionId: string,
  timerange: TimeRange,
  queryString: string,
  streams: Array<string>,
  parameters?: Immutable.Set<Parameter>,
) => {
  let searchLink = SearchLink.builder()
    .query(createElasticsearchQueryString(queryString))
    .timerange(timerange)
    .streams(streams)
    .build()
    .toURL();

  if (parameters?.size) {
    searchLink = new URI(searchLink).setSearch('session-id', sessionId).toString();
  }

  return searchLink;
};

export const ReplaySearchButtonComponent = ({ searchLink, children, onClick }: { children?: React.ReactNode, searchLink: string, onClick?: () => void }) => (
  <NeutralLink href={searchLink} target="_blank" rel="noopener noreferrer" title="Replay search" onClick={onClick}>
    {children
      ? <>{children} <StyledIcon name="play" /></>
      : <IconButton name="play" focusable={false} />}
  </NeutralLink>
);

type Props = {
  queryString?: string | undefined,
  timerange?: TimeRange | undefined,
  streams?: string[] | undefined,
  parameters?: Immutable.Set<Parameter>,
  children?: React.ReactNode,
  parameterBindings?: ParameterBindings,
};

const ReplaySearchButton = ({ queryString, timerange, streams, parameters, children, parameterBindings }: Props) => {
  const sessionId = useMemo(() => `replay-search-${generateId()}`, []);
  const searchLink = buildSearchLink(sessionId, timerange, queryString, streams, parameters);

  const onReplaySearch = useCallback(() => {
    if (parameters?.size) {
      Store.set(sessionId, JSON.stringify({ parameters, parameterBindings }));
    }
  }, [sessionId, parameters, parameterBindings]);

  return <ReplaySearchButtonComponent searchLink={searchLink} onClick={onReplaySearch}>{children}</ReplaySearchButtonComponent>;
};

ReplaySearchButton.defaultProps = {
  queryString: undefined,
  timerange: undefined,
  streams: undefined,
  parameters: undefined,
  parameterBindings: undefined,
  children: undefined,
};

ReplaySearchButtonComponent.defaultProps = {
  children: undefined,
  onClick: undefined,
};

export default ReplaySearchButton;
