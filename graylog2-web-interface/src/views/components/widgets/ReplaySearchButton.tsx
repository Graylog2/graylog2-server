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

  &:hover {
    text-decoration: none;
  }

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
  streamCategories: Array<string>,
  parameters?: Immutable.Set<Parameter>,
) => {
  let searchLink = SearchLink.builder()
    .query(createElasticsearchQueryString(queryString))
    .timerange(timerange)
    .streams(streams)
    .streamCategories(streamCategories)
    .build()
    .toURL();

  if (parameters?.size) {
    searchLink = new URI(searchLink).setSearch('session-id', sessionId).toString();
  }

  return searchLink;
};

const iconName = 'play_arrow' as const;

type CustomComponentProps = React.PropsWithChildren<{
  href?: string;
  title?: string;
  onClick?: React.MouseEventHandler<unknown>;
  target?: string;
  rel?: string;
}>;

export const ReplaySearchButtonComponent = ({
  searchLink,
  children = undefined,
  onClick = undefined,
  component: Component = NeutralLink,
}: {
  children?: React.ReactNode;
  searchLink: string;
  onClick?: () => void;
  component?: React.ComponentType<CustomComponentProps>;
}) => {
  const title = 'Replay search';

  return (
    <Component href={searchLink} target="_blank" rel="noopener noreferrer" title={title} onClick={onClick}>
      {children ? (
        <>
          {children} <StyledIcon name={iconName} />
        </>
      ) : (
        <IconButton name={iconName} focusable={false} title={title} />
      )}
    </Component>
  );
};

type Props = {
  queryString?: string | undefined;
  timerange?: TimeRange | undefined;
  streams?: string[] | undefined;
  streamCategories?: string[] | undefined;
  parameters?: Immutable.Set<Parameter>;
  children?: React.ReactNode;
  parameterBindings?: ParameterBindings;
};

const ReplaySearchButton = ({
  queryString = undefined,
  timerange = undefined,
  streams = undefined,
  streamCategories = undefined,
  parameters = undefined,
  children = undefined,
  parameterBindings = undefined,
}: Props) => {
  const sessionId = useMemo(() => `replay-search-${generateId()}`, []);
  const searchLink = buildSearchLink(sessionId, timerange, queryString, streams, streamCategories, parameters);

  const onReplaySearch = useCallback(() => {
    if (parameters?.size) {
      Store.set(sessionId, JSON.stringify({ parameters, parameterBindings }));
    }
  }, [sessionId, parameters, parameterBindings]);

  return (
    <ReplaySearchButtonComponent searchLink={searchLink} onClick={onReplaySearch}>
      {children}
    </ReplaySearchButtonComponent>
  );
};

export default ReplaySearchButton;
