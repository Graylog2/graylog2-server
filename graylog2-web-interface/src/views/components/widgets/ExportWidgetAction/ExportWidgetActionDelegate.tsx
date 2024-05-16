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
import React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { WidgetMenuActionComponentProps } from 'views/components/widgets/Types';
import ExportWidgetPlug from 'views/components/widgets/ExportWidgetAction/ExportWidgetPlug';

const ExportWidgetActionDelegate = ({ widget, contexts, disabled }: WidgetMenuActionComponentProps) => {
  const exportAction = usePluginEntities('views.components.widgets.exportAction')?.[0];
  const ExportActionComponent = exportAction && exportAction();
  if (!ExportActionComponent) return <ExportWidgetPlug />;

  return <ExportActionComponent widget={widget} contexts={contexts} disabled={disabled} />;
};

export default ExportWidgetActionDelegate;
