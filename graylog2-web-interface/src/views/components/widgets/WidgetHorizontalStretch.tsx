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
import { useCallback } from 'react';
import PropTypes from 'prop-types';

import Spinner from 'components/common/Spinner';
import { widgetDefinition } from 'views/logic/Widgets';
import { IconButton } from 'components/common';
import { Position } from 'views/components/widgets/WidgetPropTypes';

type PositionType = {
  col: number,
  row: number,
  height: number,
  width: number,
};

type Props = {
  onStretch: (args: { id: string } & PositionType) => void,
  position: PositionType,
  widgetId: string,
  widgetType: string,
}

const WidgetHorizontalStretch = ({ onStretch, position, widgetId, widgetType }: Props) => {
  const onClick = useCallback(() => {
    const { col, row, height, width } = position;
    const { defaultWidth } = widgetDefinition(widgetType);

    onStretch({
      id: widgetId, col, row, height, width: width === Infinity ? defaultWidth : Infinity,
    });
  }, [onStretch, position, widgetId, widgetType]);

  if (!position) {
    return <Spinner />;
  }

  const { width } = position;
  const stretched = width === Infinity;
  const icon = stretched ? 'compress' : 'arrows-alt-h';
  const title = stretched ? 'Compress width' : 'Stretch width';

  return (
    <IconButton onClick={onClick} name={icon} title={title} />
  );
};

WidgetHorizontalStretch.propTypes = {
  widgetId: PropTypes.string.isRequired,
  widgetType: PropTypes.string.isRequired,
  position: PropTypes.shape(Position).isRequired,
  onStretch: PropTypes.func.isRequired,
};

export default WidgetHorizontalStretch;
