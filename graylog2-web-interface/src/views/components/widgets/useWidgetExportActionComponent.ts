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
import usePluginEntities from 'hooks/usePluginEntities';
import { type WidgetActionType } from 'views/components/widgets/Types';
import type Widget from 'views/logic/widgets/Widget';

const useWidgetExportActionComponent = (widget: Widget) => {
  const exportActions = usePluginEntities('views.widgets.exportAction');

  const widgetExportAction = exportActions && exportActions
    // eslint-disable-next-line react-hooks/rules-of-hooks
    .filter(({ action, useCondition }) => (typeof useCondition === 'function' && useCondition()) && action && (typeof action.isHidden !== 'function' || !action.isHidden(widget)))
    .map(({ action }: { action: WidgetActionType }) => action);

  return widgetExportAction?.[0]?.component;
};

export default useWidgetExportActionComponent;
