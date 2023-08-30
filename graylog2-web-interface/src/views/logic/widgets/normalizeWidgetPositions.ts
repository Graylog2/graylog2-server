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

// @ts-ignore: Module react-grid-layout has no exported member utils
import { utils } from 'react-grid-layout';
import omitBy from 'lodash/omitBy';
import keyBy from 'lodash/keyBy';
import mapValues from 'lodash/mapValues';

import type { Layout, Position, LayoutItem } from 'components/common/ReactGridContainer';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type Widget from 'views/logic/widgets/Widget';

const layoutItemToPosition = ({ i, x, y, w, h }: LayoutItem) => ({
  id: i,
  col: x + 1,
  row: y + 1,
  height: h,
  width: w,
});
export const layoutToPositions = (layout: Layout): Position[] => layout
  .map(layoutItemToPosition);

const positionItemToLayout = ([id, position]: [string, WidgetPosition]) => {
  const { col, row, height, width } = position;

  return ({
    i: id,
    x: col ? Math.max(col - 1, 0) : 0,
    y: (row === undefined || row <= 0 ? Infinity : row - 1),
    h: height || 1,
    w: width || 1,
  });
};

export const positionsToLayout = (widgetPositions: { [key: string]: WidgetPosition }) => Object.entries(widgetPositions).map(positionItemToLayout);

export const normalizeWidgetPositions = (widgetPositions: { [key: string]: WidgetPosition }, widgetById: {[name: string]: Widget}): { [key: string]: WidgetPosition } => {
  const filtratedWidgetPositions = omitBy(widgetPositions, (_, id) => !widgetById[id]);
  const layout = positionsToLayout(filtratedWidgetPositions);
  const compactedLayout = utils.compact(layout, 'vertical', 12);

  const positions = keyBy(layoutToPositions(compactedLayout), 'id');

  return mapValues(positions, ({ col, row, width, height }) => WidgetPosition
    .builder()
    .row(row)
    .col(col)
    .width(width)
    .height(height)
    .build(),
  );
};
