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
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import WidgetClass from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import ExportSettingsContextProvider from 'views/components/ExportSettingsContextProvider';

import { WidgetDataMap, WidgetErrorsMap } from './widgets/WidgetPropTypes';
import Widget from './widgets/Widget';
import DrilldownContextProvider from './contexts/DrilldownContextProvider';

type Props = {
  data: WidgetDataMap,
  errors: WidgetErrorsMap,
  fields: Immutable.List<TFieldTypeMapping>,
  onPositionsChange: (position?: WidgetPosition) => void,
  onWidgetSizeChange: (widgetId?: string, dimensions?: { height: number, width: number }) => void,
  position: WidgetPosition,
  title: string,
  widget: WidgetClass & { data: string };
  widgetDimension: { height: number | null | undefined, width: number | null | undefined },
};

const WidgetComponent = ({
  data,
  errors,
  fields,
  onPositionsChange = () => undefined,
  onWidgetSizeChange = () => {},
  position,
  title,
  widget,
  widgetDimension: { height, width },
}: Props) => {
  const dataKey = widget.data || widget.id;
  const widgetData = data[dataKey];
  const widgetErrors = errors[widget.id] || [];

  return (
    <DrilldownContextProvider widget={widget}>
      <WidgetContext.Provider value={widget}>
        <AdditionalContext.Provider value={{ widget }}>
          <ExportSettingsContextProvider>
            <Widget id={widget.id}
                    widget={widget}
                    data={widgetData}
                    errors={widgetErrors}
                    height={height}
                    position={position}
                    width={width}
                    fields={fields}
                    onPositionsChange={onPositionsChange}
                    onSizeChange={onWidgetSizeChange}
                    title={title} />
          </ExportSettingsContextProvider>
        </AdditionalContext.Provider>
      </WidgetContext.Provider>
    </DrilldownContextProvider>
  );
};

WidgetComponent.propTypes = {
  widget: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  errors: PropTypes.object.isRequired,
  widgetDimension: PropTypes.object.isRequired,
  title: PropTypes.string.isRequired,
  position: PropTypes.object.isRequired,
  onPositionsChange: PropTypes.func,
  fields: PropTypes.object.isRequired,
  onWidgetSizeChange: PropTypes.func,
};

WidgetComponent.defaultProps = {
  onPositionsChange: () => {},
  onWidgetSizeChange: () => {},
};

export default WidgetComponent;
