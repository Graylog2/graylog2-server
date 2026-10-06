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
import styled, { css } from 'styled-components';

import Button from 'components/bootstrap/Button';

import CopyGlyph, { copyId } from './CopyGlyph';
import type { IndexViewProps } from './CopyGlyph';
import { indexContent, pileRows } from './mapLayout';

// An index in a rounded box holding its shard copies, each one log seen end-on. Closed, it shows up to
// COLLAPSED_SHARDS logs side by side, so every closed index has the same size; opened, all its logs are stacked
// into a woodpile, every upper log resting in the groove between two below it.
// Lines get thinner from the outside in: node 3px, index box 2px, then the logs' own bark rings.
const PILE_WIDTH = 160;
// A glyph is 40px; the next row sits in the grooves, sqrt(3)/2 of a log higher: 35px.
const ROW_OVERLAP = 5;

const IndexBox = styled.div<{ $open: boolean }>(
  ({ theme, $open }) => css`
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: ${theme.spacings.xxs};
    width: ${$open ? 'auto' : `${PILE_WIDTH}px`};
    min-width: ${PILE_WIDTH}px;
    max-width: 100%;
    border: 2px solid ${theme.colors.gray[60]};
    border-radius: 8px;
    background-color: ${theme.colors.global.contentBackground};
    padding: ${theme.spacings.xxs} ${theme.spacings.xs};
  `,
);

// The index's name, a hairline under it; long names cut at the end (full name in the title).
const NameStrip = styled.div(
  ({ theme }) => css`
    font-family: monospace;
    font-size: ${theme.fonts.size.small};
    border-bottom: 1px solid ${theme.colors.gray[80]};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: center;
  `,
);

// The logs. Boxes in a row are as tall as the tallest pile; the logs sit in the middle of their box.
const Stack = styled.div(
  ({ theme }) => css`
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    min-height: 44px;
    padding: ${theme.spacings.xxs} ${theme.spacings.xs};
  `,
);

const Row = styled.div`
  display: flex;
  justify-content: center;

  & + & {
    margin-top: -${ROW_OVERLAP}px;
  }
`;

const Footer = styled.div`
  display: flex;
  justify-content: center;
  min-height: 18px;
`;

const IndexPile = ({
  group,
  expanded,
  onToggleExpanded,
  selected,
  onSelect,
  shardCounts,
  onShowIndex,
}: IndexViewProps) => {
  const { shown, toggle } = indexContent(group, expanded);
  // Fill from the bottom: the problems (first in `shown`) end up on the bottom row, where the pile starts.
  const sizes = expanded ? pileRows(shown.length).reverse() : [shown.length];
  const starts = sizes.map((_size, i) => sizes.slice(0, i).reduce((sum, size) => sum + size, 0));
  const rows = sizes.map((size, i) => shown.slice(starts[i], starts[i] + size)).reverse();

  return (
    <IndexBox $open={expanded} aria-label={`Index ${group.index}`}>
      <NameStrip title={group.index}>{group.index}</NameStrip>
      <Stack>
        {rows.map((row) => (
          <Row key={row.map(copyId).join()}>
            {row.map((copy) => (
              <CopyGlyph
                key={copyId(copy)}
                copy={copy}
                selected={selected}
                onSelect={onSelect}
                shardCounts={shardCounts}
                onShowIndex={onShowIndex}
              />
            ))}
          </Row>
        ))}
      </Stack>
      <Footer>
        {toggle && (
          <Button bsStyle="link" bsSize="xsmall" onClick={onToggleExpanded} aria-pressed={expanded}>
            {toggle}
          </Button>
        )}
      </Footer>
    </IndexBox>
  );
};

export default IndexPile;
