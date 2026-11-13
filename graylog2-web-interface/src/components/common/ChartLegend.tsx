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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import ColorPicker from 'components/common/ColorPicker';
import Popover from 'components/common/Popover';

const COLUMNS = 5;

const ColorHint = styled.div<{ $color: string; $clickable: boolean }>(
  ({ $color, $clickable }) => css`
    flex-shrink: 0;
    cursor: ${$clickable ? 'pointer' : 'default'};
    background-color: ${$color} !important; /* Needed for report generation */
    -webkit-print-color-adjust: exact !important; /* Needed for report generation */
    width: 12px;
    height: 12px;
  `,
);

const GridLegend = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: repeat(${COLUMNS}, 1fr);
    gap: ${theme.spacings.xxs} ${theme.spacings.xs};
    max-height: 100px;
    overflow: auto;
    padding: ${theme.spacings.xxs};
  `,
);

const WrapLegend = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.xxs} ${theme.spacings.xs};
    padding: ${theme.spacings.xxs};
  `,
);

const Entry = styled.div`
  display: flex;
  align-items: center;
`;

const Label = styled.div`
  margin-left: 5px;
  line-height: 1;
`;

export type ChartLegendItem = {
  /** Identifies the item, e.g. the name of its chart series. */
  key: string;
  label: React.ReactNode;
  color: string;
  /** A plain-text name of the item, e.g. for the color picker's heading. Defaults to the key. */
  title?: string;
};

export type ChartLegendColorPicker = {
  /** The colors to choose from, grouped by hue. */
  colors: Array<Array<string>>;
  onChange: (key: string, color: string) => void;
};

type EntryProps = {
  item: ChartLegendItem;
  colorPicker?: ChartLegendColorPicker;
};

const LegendEntry = ({ item: { key, label, color, title = key }, colorPicker = undefined }: EntryProps) => {
  const [showPopover, setShowPopover] = useState(false);

  if (!colorPicker) {
    return (
      <Entry>
        <ColorHint aria-label="Color Hint" $color={color} $clickable={false} />
        <Label>{label}</Label>
      </Entry>
    );
  }

  const onColorSelect = (newColor: string) => {
    colorPicker.onChange(key, newColor);
    setShowPopover(false);
  };

  return (
    <Entry>
      <Popover position="top" withArrow opened={showPopover}>
        <Popover.Target>
          <ColorHint
            aria-label="Color Hint"
            $color={color}
            $clickable
            onClick={() => setShowPopover((show) => !show)}
          />
        </Popover.Target>
        <Popover.Dropdown title={`Configuration for ${title}`}>
          <ColorPicker color={color} colors={colorPicker.colors} onChange={onColorSelect} />
        </Popover.Dropdown>
      </Popover>
      <Label>{label}</Label>
    </Entry>
  );
};

type Props = {
  items: Array<ChartLegendItem>;
  /** `grid` aligns the items in five columns and scrolls when they need more room, `wrap` lets them flow. */
  layout?: 'grid' | 'wrap';
  /** Lets the user change an item's color by clicking its color hint. */
  colorPicker?: ChartLegendColorPicker;
};

/** The legend of a chart: a color hint and a label per series. */
const ChartLegend = ({ items, layout = 'wrap', colorPicker = undefined }: Props) => {
  const Container = layout === 'grid' ? GridLegend : WrapLegend;

  return (
    <Container>
      {items.map((item) => (
        <LegendEntry key={item.key} item={item} colorPicker={colorPicker} />
      ))}
    </Container>
  );
};

export default ChartLegend;
