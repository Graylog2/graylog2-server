// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

import CountBadge from './CountBadge';

type Props = {
  count: number,
  listing: React.Node,
};

const Listing: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
`;

const ListingWithCount = ({ count, listing }: Props) => (
  <Listing title={String(listing)}>
    <CountBadge>{count}</CountBadge>
    {' '}
    {listing}
  </Listing>
);

export default ListingWithCount;
