// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import styled, { type StyledComponent } from 'styled-components';
import Qs from 'qs';

import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import type { TimeRange } from 'views/logic/queries/Query';
import DrilldownContext from '../contexts/DrilldownContext';

const DitheredIcon: StyledComponent<{}, {}, HTMLElement> = styled(Icon)`
  opacity: 0.3;
  position: relative;
  top: 3px;
`;

const NeutralLink: StyledComponent<{}, {}, HTMLAnchorElement> = styled.a`
  color: inherit;
  text-decoration: none;
  
  &:visited {
    color: inherit;
  }
`;

const _searchTimerange = (timerange: TimeRange) => {
  const { type } = timerange;
  const result = { rangetype: type };

  switch (timerange.type) {
    case 'relative': return { ...result, relative: timerange.range };
    case 'keyword': return { ...result, keyword: timerange.keyword };
    case 'absolute': return { ...result, from: timerange.from, to: timerange.to };
    default: return result;
  }
};

const buildSearchLink = (timerange, query, streams) => {
  const searchTimerange = _searchTimerange(timerange);

  const params = {
    ...searchTimerange,
    q: query,
  };
  const paramsWithStreams = streams && streams.length > 0
    ? { ...params, streams: streams.join(',') }
    : params;

  return `${Routes.SEARCH}?${Qs.stringify(paramsWithStreams)}`;
};

const ReplaySearchButton = () => {
  const { query, timerange, streams } = useContext(DrilldownContext);
  const searchLink = buildSearchLink(timerange, query.query_string, streams);
  return (
    <NeutralLink href={searchLink} target="_blank" rel="noopener noreferrer" title="Replay search">
      <DitheredIcon name="play" />
    </NeutralLink>
  );
};

ReplaySearchButton.propTypes = {};

export default ReplaySearchButton;
