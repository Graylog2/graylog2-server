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

import type Widget from 'views/logic/widgets/Widget';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type { Widgets } from 'views/stores/WidgetStore';
import { singleton } from 'logic/singleton';

type ApplySearchControlsChanges = (widget: Widget) => Widget | undefined;
type ApplyElementConfigurationChanges = (widgetConfig: WidgetConfig) => WidgetConfig | undefined;

type WidgetEditApplyAllChangesContextValue = {
  applyAllWidgetChanges: () => Promise<Widgets | void>,
  bindApplyElementConfigurationChanges: (updateFn: ApplyElementConfigurationChanges) => void,
  bindApplySearchControlsChanges: (updateFn: ApplySearchControlsChanges) => void,
}

const WidgetEditApplyAllChangesContext = React.createContext<WidgetEditApplyAllChangesContextValue>({
  applyAllWidgetChanges: () => Promise.resolve(),
  bindApplyElementConfigurationChanges: () => {},
  bindApplySearchControlsChanges: () => {},
});

export default singleton('contexts.WidgetEditApplyAllChangesContext', () => WidgetEditApplyAllChangesContext);
