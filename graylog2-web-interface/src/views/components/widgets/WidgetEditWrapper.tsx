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
import { useMemo } from 'react';

import { widgetDefinition } from 'views/logic/Widgets';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type WidgetType from 'views/logic/widgets/Widget';

import EditWidgetFrame from './EditWidgetFrame';

type Props = {
  children: React.ReactElement;
  config: WidgetConfig;
  editing: boolean;
  fields: FieldTypeMappingsList;
  id: string;
  onCancelEdit: () => void;
  onSubmit: (newWidget: WidgetType, hasChanges: boolean) => Promise<void>;
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => void;
  showQueryControls?: boolean;
  type: string;
};

const WidgetEditWrapper = ({
  children,
  config,
  editing,
  fields,
  id,
  onCancelEdit,
  onSubmit,
  onWidgetConfigChange,
  type,
  showQueryControls = undefined,
}: Props) => {
  const EditComponent = useMemo(() => widgetDefinition(type).editComponent, [type]);
  const hasOwnSubmitButton = widgetDefinition(type).hasEditSubmitButton;

  return editing ? (
    <EditWidgetFrame
      onSubmit={onSubmit}
      onCancel={onCancelEdit}
      displaySubmitActions={!hasOwnSubmitButton}
      showQueryControls={showQueryControls}>
      <EditComponent
        config={config}
        fields={fields}
        editing={editing}
        id={id}
        type={type}
        onCancel={onCancelEdit}
        onChange={onWidgetConfigChange}>
        {children}
      </EditComponent>
    </EditWidgetFrame>
  ) : (
    children
  );
};

export default WidgetEditWrapper;
