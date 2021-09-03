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
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { BackendWidgetPosition } from 'views/types';

import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import ExportSettingsContextProvider from 'views/components/ExportSettingsContextProvider';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import { useStore } from 'stores/connect';
import { TitlesStore } from 'views/stores/TitlesStore';
import defaultTitle from 'views/components/defaultTitle';
import { WidgetStore } from 'views/stores/WidgetStore';
import TitleTypes from 'views/stores/TitleTypes';

import { Position, WidgetDataMap, WidgetErrorsMap } from './widgets/WidgetPropTypes';
import Widget from './widgets/Widget';
import DrilldownContextProvider from './contexts/DrilldownContextProvider';
import WidgetFieldTypesContextProvider from './contexts/WidgetFieldTypesContextProvider';

type Props = {
  data: {},
  editing: boolean,
  errors: { [widgetId: string]: Array<{ description: string }> },
  fields: Immutable.List<TFieldTypeMapping>,
  onPositionsChange: (position: BackendWidgetPosition) => void,
  onWidgetSizeChange: (widgetId?: string, dimensions?: { height: number, width: number }) => void,
  position: WidgetPosition,
  widgetId: string,
  widgetDimension: { height: number | null | undefined, width: number | null | undefined },
};

const WidgetComponent = ({
  data,
  editing,
  errors,
  fields,
  onPositionsChange = () => undefined,
  onWidgetSizeChange = () => {},
  position,
  widgetId,
  widgetDimension: { height, width },
}: Props) => {
  const widget = useStore(WidgetStore, (state) => state.get(widgetId));
  const widgetData = data[widgetId];
  const widgetErrors = errors[widgetId] || [];
  const viewType = useContext(ViewTypeContext);
  const title = useStore(TitlesStore, (titles) => titles.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget)) as string);

  const WidgetFieldTypesIfDashboard = viewType === View.Type.Dashboard ? WidgetFieldTypesContextProvider : React.Fragment;

  return (
    <DrilldownContextProvider widget={widget}>
      <WidgetContext.Provider value={widget}>
        <AdditionalContext.Provider value={{ widget }}>
          <ExportSettingsContextProvider>
            <WidgetFieldTypesIfDashboard>
              <Widget data={widgetData}
                      editing={editing}
                      errors={widgetErrors}
                      fields={fields}
                      height={height}
                      id={widget.id}
                      onPositionsChange={onPositionsChange}
                      onSizeChange={onWidgetSizeChange}
                      position={position}
                      title={title}
                      widget={widget}
                      width={width} />
            </WidgetFieldTypesIfDashboard>
          </ExportSettingsContextProvider>
        </AdditionalContext.Provider>
      </WidgetContext.Provider>
    </DrilldownContextProvider>
  );
};

WidgetComponent.propTypes = {
  data: WidgetDataMap.isRequired,
  editing: PropTypes.bool.isRequired,
  errors: WidgetErrorsMap.isRequired,
  fields: PropTypes.object.isRequired,
  onPositionsChange: PropTypes.func,
  onWidgetSizeChange: PropTypes.func,
  position: PropTypes.shape(Position).isRequired,
  widgetDimension: PropTypes.object.isRequired,
};

WidgetComponent.defaultProps = {
  onPositionsChange: () => {},
  onWidgetSizeChange: () => {},
};

export default WidgetComponent;
