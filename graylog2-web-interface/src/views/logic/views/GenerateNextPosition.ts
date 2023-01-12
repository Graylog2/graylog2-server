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
import * as Immutable from 'immutable';

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import GetPositionForNewWidget from 'views/logic/views/GetPositionForNewWidget';

import type Widget from '../widgets/Widget';

const incrementRow = (position: WidgetPosition) => position.row + 1;
const MAXIMUM_GRID_SIZE = 12;

const fillMatrix = (matrix, row, start, end) => {
  const newMatrix = [...matrix];

  if (!matrix[row - 1]) {
    newMatrix[row - 1] = Array(MAXIMUM_GRID_SIZE);
  }

  newMatrix[row - 1].fill(true, start - 1, end);

  return newMatrix;
};

export default (
  widgetPositions: Immutable.Map<string, WidgetPosition>,
  widgets: Immutable.List<Widget>,
): Immutable.OrderedMap<string, WidgetPosition> => {
  return Immutable.OrderedMap(
    widgets
      .reduce<{
      newWidgets: Immutable.List<[string, WidgetPosition]>,
      matrixArray: Array<any>
    }>(({ newWidgets, matrixArray }, widget, index) => {
      if (!widgetPositions.has(widget.id)) {
        const positions = GetPositionForNewWidget(widget);

        return ({
          newWidgets: newWidgets.push([widget.id, positions]),
          matrixArray: fillMatrix(matrixArray, positions.row, positions.col, positions.width),
        });
      }

      if (index === 0) {
        const positions = GetPositionForNewWidget(widget);

        return ({
          newWidgets: newWidgets.push([widget.id, widgetPositions.get(widget.id)]),
          matrixArray: fillMatrix(matrixArray, positions.row, positions.col, positions.width),
        });
      }

      const prevWidgetPositions = newWidgets.get(index - 1)[1];
      const curWidgetPositions = widgetPositions.get(widget.id);
      // const spaceAfterWidget = MAXIMUM_GRID_SIZE - (prevWidgetPositions.col + prevWidgetPositions.width - 1);
      // const notEnoughSpace = spaceAfterWidget < curWidgetPositions.width;
      const curStart = curWidgetPositions.col;
      const curEnd = curWidgetPositions.col + curWidgetPositions.width - 1;
      const prevStart = prevWidgetPositions.col;
      const prevEnd = prevWidgetPositions.col + prevWidgetPositions.width - 1;
      console.log({ matrixArray });
      const isPlaceBusy = matrixArray[curWidgetPositions.row - 1] && matrixArray[curWidgetPositions.row - 1].slice(curStart - 1, curEnd).some((item) => !!item);

      console.log(widget.id, { widget, isPlaceBusy }, {
        curRow: curWidgetPositions.row,
        prevRow: prevWidgetPositions.row,
        'curWidgetPositions.row <= prevWidgetPositions.row': curWidgetPositions.row <= prevWidgetPositions.row,
        QQ: ((curWidgetPositions.col >= prevWidgetPositions.col)
            && (curWidgetPositions.col <= (prevWidgetPositions.col + prevWidgetPositions.width - 1))),
        WW: ((prevWidgetPositions.col >= curWidgetPositions.col)
            && (prevWidgetPositions.col <= (curWidgetPositions.col + curWidgetPositions.width - 1))),
      });

      const updatedPositions = isPlaceBusy
        ? widgetPositions.get(widget.id).toBuilder().row(incrementRow(curWidgetPositions)).build()
        : widgetPositions.get(widget.id);

      return ({
        newWidgets: newWidgets.push([widget.id, updatedPositions]),
        matrixArray: fillMatrix(matrixArray, updatedPositions.row, updatedPositions.col, updatedPositions.col + updatedPositions.width - 1),
      });
    }, { newWidgets: Immutable.List(), matrixArray: [] }).newWidgets,
  );
};
