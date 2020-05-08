// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import styled, { type StyledComponent } from 'styled-components';

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
      <IconButton name="play" />
    </NeutralLink>
  );
};

export default ReplaySearchButton;
