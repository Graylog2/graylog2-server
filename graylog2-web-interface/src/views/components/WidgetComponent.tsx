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
import { useContext, useMemo } from 'react';
import PropTypes from 'prop-types';
import type * as Immutable from 'immutable';

import type { BackendWidgetPosition } from 'views/types';
import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import ExportSettingsContextProvider from 'views/components/ExportSettingsContextProvider';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import View from 'views/logic/views/View';
import { useStore } from 'stores/connect';
import { TitlesStore } from 'views/stores/TitlesStore';
import defaultTitle from 'views/components/defaultTitle';
import { WidgetStore } from 'views/stores/WidgetStore';
import TitleTypes from 'views/stores/TitleTypes';

import { Position } from './widgets/WidgetPropTypes';
import Widget from './widgets/Widget';
import DrilldownContextProvider from './contexts/DrilldownContextProvider';
import WidgetFieldTypesContextProvider from './contexts/WidgetFieldTypesContextProvider';

type Props = {
  editing: boolean,
  fields: Immutable.List<TFieldTypeMapping>,
  onPositionsChange: (position: BackendWidgetPosition) => void,
  position: WidgetPosition,
  widgetId: string,
};

const WidgetComponent = ({
  editing,
  fields,
  onPositionsChange = () => undefined,
  position,
  widgetId,
}: Props) => {
  const widget = useStore(WidgetStore, (state) => state.get(widgetId));
  const viewType = useContext(ViewTypeContext);
  const title = useStore(TitlesStore, (titles) => titles?.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget)) as string);
  const additionalContext = useMemo(() => ({ widget }), [widget]);

  const WidgetFieldTypesIfDashboard = viewType === View.Type.Dashboard ? WidgetFieldTypesContextProvider : React.Fragment;

  return (
    <DrilldownContextProvider widget={widget}>
      <WidgetContext.Provider value={widget}>
        <AdditionalContext.Provider value={additionalContext}>
          <ExportSettingsContextProvider>
            <WidgetFieldTypesIfDashboard>
              <Widget editing={editing}
                      fields={fields}
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

WidgetComponent.propTypes = {
  editing: PropTypes.bool.isRequired,
  fields: PropTypes.object.isRequired,
  onPositionsChange: PropTypes.func,
  position: PropTypes.shape(Position).isRequired,
};

WidgetComponent.defaultProps = {
  onPositionsChange: () => {},
};

export default WidgetComponent;
