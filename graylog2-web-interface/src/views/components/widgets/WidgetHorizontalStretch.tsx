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
import styled, { css } from 'styled-components';

import Spinner from 'components/common/Spinner';
import { widgetDefinition } from 'views/logic/Widgets';
import { IconButton } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const StyledIconButton = styled(IconButton)<{ $stretched: boolean }>(({ $stretched }) => css`
  span {
    position: relative;
    left: ${$stretched ? '-1px' : 0};
  }
`);

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
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const onClick = useCallback(() => {
    const { col, row, height, width } = position;
    const { defaultWidth } = widgetDefinition(widgetType);

    onStretch({
      id: widgetId, col, row, height, width: width === Infinity ? defaultWidth : Infinity,
    });

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.SEARCH_WIDGET_HORIZONTAL_STRETCH, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-stretch-button',
    });
  }, [onStretch, pathname, position, sendTelemetry, widgetId, widgetType]);

  if (!position) {
    return <Spinner />;
  }

  const { width } = position;
  const stretched = width === Infinity;
  const icon = stretched ? 'compress' : 'width';
  const title = stretched ? 'Compress width' : 'Stretch width';

  return (
    <StyledIconButton onClick={onClick}
                      name={icon}
                      title={title}
                      $stretched={stretched}
                      rotation={stretched ? 90 : undefined} />
  );
};

export default WidgetHorizontalStretch;
