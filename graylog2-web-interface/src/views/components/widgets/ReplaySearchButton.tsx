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

import IconButton from 'components/common/IconButton';
import Icon from 'components/common/Icon';
import SearchLink from 'components/search/SearchLink';
import type { TimeRange } from 'views/logic/queries/Query';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

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

export const buildSearchLink = (
  timerange: TimeRange,
  queryString: string,
  streams: Array<string>,
) => SearchLink.builder()
  .query(createElasticsearchQueryString(queryString))
  .timerange(timerange)
  .streams(streams)
  .build()
  .toURL();

type Props = {
  queryString?: string | undefined,
  timerange?: TimeRange | undefined,
  streams?: string[] | undefined,
  children?: React.ReactNode,
};

export const ReplaySearchButtonComponent = ({ searchLink, children }: { children?: React.ReactNode, searchLink: string }) => (
  <NeutralLink href={searchLink} target="_blank" rel="noopener noreferrer" title="Replay search">
    {children
      ? <>{children} <StyledIcon name="play" /></>
      : <IconButton name="play" focusable={false} />}
  </NeutralLink>
);

const ReplaySearchButton = ({ queryString, timerange, streams, children }: Props) => {
  const searchLink = buildSearchLink(timerange, queryString, streams);

  return <ReplaySearchButtonComponent searchLink={searchLink}>{children}</ReplaySearchButtonComponent>;
};

ReplaySearchButton.defaultProps = {
  queryString: undefined,
  timerange: undefined,
  streams: undefined,
  children: undefined,
};

ReplaySearchButtonComponent.defaultProps = {
  children: undefined,
};

export default ReplaySearchButton;
