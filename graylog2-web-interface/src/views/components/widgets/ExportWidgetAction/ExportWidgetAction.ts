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

import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import type Widget from 'views/logic/widgets/Widget';
import type { WidgetActionType } from 'views/components/widgets/Types';
import ExportWidgetActionDelegate from 'views/components/widgets/ExportWidgetAction/ExportWidgetActionDelegate';
import EventsWidget from 'views/logic/widgets/events/EventsWidget';

const ExportWidgetAction: WidgetActionType = {
  type: 'export-widget-action',
  position: 'menu',
  isHidden: (w: Widget) => ![AggregationWidget.type, EventsWidget.type].includes(w.type),
  component: ExportWidgetActionDelegate,
};

export default ExportWidgetAction;
