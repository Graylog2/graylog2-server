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

import type { BackendWidgetPosition } from 'views/types';
import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import ExportSettingsContextProvider from 'views/components/ExportSettingsContextProvider';
import View from 'views/logic/views/View';
import defaultTitle from 'views/components/defaultTitle';
import TitleTypes from 'views/stores/TitleTypes';
import useViewType from 'views/hooks/useViewType';
import type WidgetType from 'views/logic/widgets/Widget';
import useWidget from 'views/hooks/useWidget';
import useActiveViewState from 'views/hooks/useActiveViewState';

import Widget from './widgets/Widget';
import DrilldownContextProvider from './contexts/DrilldownContextProvider';
import WidgetFieldTypesContextProvider from './contexts/WidgetFieldTypesContextProvider';

type Props = {
  editing: boolean,
  onPositionsChange?: (position: BackendWidgetPosition) => void
  position: WidgetPosition,
  widgetId: string,
};

const useTitle = (widget: WidgetType) => {
  const activeViewState = useActiveViewState();

  return activeViewState?.titles?.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget)) as string;
};

const WidgetComponent = ({
  editing,
  onPositionsChange = () => undefined,
  position,
  widgetId,
}: Props) => {
  const widget = useWidget(widgetId);
  const viewType = useViewType();
  const title = useTitle(widget);
  const additionalContext = useMemo(() => ({ widget }), [widget]);

  const WidgetFieldTypesIfDashboard = viewType === View.Type.Dashboard ? WidgetFieldTypesContextProvider : React.Fragment;

  return (
    <DrilldownContextProvider widget={widget}>
      <WidgetContext.Provider value={widget}>
        <AdditionalContext.Provider value={additionalContext}>
          <ExportSettingsContextProvider>
            <WidgetFieldTypesIfDashboard>
              <Widget editing={editing}
                      id={widget.id}
                      onPositionsChange={onPositionsChange}
                      position={position}
                      title={title}
                      widget={widget} />
            </WidgetFieldTypesIfDashboard>
          </ExportSettingsContextProvider>
        </AdditionalContext.Provider>
      </WidgetContext.Provider>
    </DrilldownContextProvider>
  );
};

export default WidgetComponent;
