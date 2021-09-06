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
import { useCallback, useContext, useState } from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { BackendWidgetPosition } from 'views/types';

import connect from 'stores/connect';
import { widgetDefinition } from 'views/logic/Widgets';
import { WidgetActions, Widgets } from 'views/stores/WidgetStore';
import { TitlesActions } from 'views/stores/TitlesStore';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import TimerangeInfo from 'views/components/widgets/TimerangeInfo';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import { WidgetErrorsList } from 'views/components/widgets/WidgetPropTypes';
import WidgetConfig from 'views/logic/widgets/WidgetConfig';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import EditWidgetFrame from './EditWidgetFrame';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';
import WidgetColorContext from './WidgetColorContext';
import WidgetErrorBoundary from './WidgetErrorBoundary';
import WidgetActionsMenu from './WidgetActionsMenu';

import InteractiveContext from '../contexts/InteractiveContext';

export type Props = {
  id: string,
  view: ViewStoreState,
  widget: WidgetModel,
  data?: { [key: string]: Result },
  editing?: boolean,
  errors?: Array<{ description: string }>,
  fields: Immutable.List<FieldTypeMapping>,
  height?: number,
  width?: number,
  title: string,
  position: WidgetPosition,
  onSizeChange: () => void,
  onPositionsChange: (position: BackendWidgetPosition) => void,
};

export type Result = {
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};

export type OnVisualizationConfigChange = (newConfig: VisualizationConfig) => void;

const _visualizationForType = (type) => {
  return widgetDefinition(type).visualizationComponent;
};

const _editComponentForType = (type) => {
  return widgetDefinition(type).editComponent;
};

const WidgetFooter = styled.div`
  width: 100%;
  display: flex;
  justify-content: flex-end;
`;

type VisualizationProps = Omit<Props, 'view'> & {
  queryId: string,
  setLoadingState: (loading: boolean) => void,
  onToggleEdit: () => void,
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => Promise<Widgets>,
};

const Visualization = ({ data, errors, title, id, widget, height, width, fields, queryId, editing, setLoadingState, onToggleEdit, onWidgetConfigChange }: VisualizationProps) => {
  if (errors && errors.length > 0) {
    return <ErrorWidget errors={errors} />;
  }

  if (data) {
    const { config, filter } = widget;
    const VisComponent = _visualizationForType(widget.type);

    return (
      <VisComponent config={config}
                    data={data}
                    editing={editing}
                    fields={fields}
                    filter={filter}
                    height={height}
                    queryId={queryId}
                    onConfigChange={onWidgetConfigChange}
                    setLoadingState={setLoadingState}
                    title={title}
                    toggleEdit={onToggleEdit}
                    type={widget.type}
                    width={width}
                    id={id} />
    );
  }

  return <LoadingWidget />;
};

type EditWrapperProps = {
  children: React.ReactNode,
  config: WidgetConfig,
  editing: boolean,
  fields: FieldTypeMappingsList,
  id: string,
  onToggleEdit: () => void,
  onCancelEdit: () => void,
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => void,
  type: string,
};

const EditWrapper = ({ children, config, editing, fields, id, onToggleEdit, onCancelEdit, onWidgetConfigChange, type }: EditWrapperProps) => {
  const EditComponent = _editComponentForType(type);

  return editing ? (
    <EditWidgetFrame onFinish={onToggleEdit} onCancel={onCancelEdit}>
      <EditComponent config={config}
                     fields={fields}
                     editing={editing}
                     id={id}
                     type={type}
                     onChange={onWidgetConfigChange}>
        {children}
      </EditComponent>
    </EditWidgetFrame>
  ) : <>{children}</>;
};

const Widget = ({ id, data, errors, editing, widget, fields, onSizeChange, title, position, onPositionsChange, view }: Props) => {
  const [loading, setLoading] = useState(false);
  const [oldWidget, setOldWidget] = useState(editing ? widget : undefined);
  const { focusedWidget, setWidgetEditing, unsetWidgetEditing } = useContext(WidgetFocusContext);
  const onToggleEdit = useCallback(() => {
    if (editing) {
      unsetWidgetEditing();
      setOldWidget(undefined);
    } else {
      RefreshActions.disable();
      setWidgetEditing(widget.id);
      setOldWidget(widget);
    }
  }, [editing, setWidgetEditing, unsetWidgetEditing, widget]);
  const onCancelEdit = useCallback(() => {
    if (oldWidget) {
      WidgetActions.update(id, oldWidget);
    }

    onToggleEdit();
  }, [id, oldWidget, onToggleEdit]);
  const onRenameWidget = useCallback((newTitle: string) => TitlesActions.set('widget', id, newTitle), [id]);
  const onWidgetConfigChange = useCallback((newWidgetConfig: WidgetConfig) => WidgetActions.updateConfig(id, newWidgetConfig), [id]);

  const { config } = widget;
  const isFocused = focusedWidget?.id === id;

  return (
    <WidgetColorContext id={id}>
      <WidgetFrame widgetId={id} onSizeChange={onSizeChange}>
        <InteractiveContext.Consumer>
          {(interactive) => (
            <WidgetHeader title={title}
                          hideDragHandle={!interactive || isFocused}
                          loading={loading}
                          onRename={onRenameWidget}>
              {!editing ? (
                <WidgetActionsMenu isFocused={isFocused}
                                   toggleEdit={onToggleEdit}
                                   title={title}
                                   view={view}
                                   position={position}
                                   onPositionsChange={onPositionsChange} />
              ) : null}
            </WidgetHeader>
          )}
        </InteractiveContext.Consumer>
        <EditWrapper onToggleEdit={onToggleEdit}
                     onCancelEdit={onCancelEdit}
                     onWidgetConfigChange={onWidgetConfigChange}
                     config={config}
                     editing={editing}
                     fields={fields}
                     id={id}
                     type={widget.type}>
          <WidgetErrorBoundary>
            <Visualization id={id}
                           data={data}
                           errors={errors}
                           queryId={view.activeQuery}
                           widget={widget}
                           fields={fields}
                           title={title}
                           position={position}
                           onSizeChange={onSizeChange}
                           onPositionsChange={onPositionsChange}
                           setLoadingState={setLoading}
                           onToggleEdit={onToggleEdit}
                           onWidgetConfigChange={onWidgetConfigChange} />
          </WidgetErrorBoundary>
        </EditWrapper>
        <WidgetFooter>
          <IfDashboard>
            {!editing && <TimerangeInfo widget={widget} activeQuery={view.activeQuery} widgetId={id} />}
          </IfDashboard>
        </WidgetFooter>
      </WidgetFrame>
    </WidgetColorContext>
  );
};

Widget.propTypes = {
  data: PropTypes.any,
  editing: PropTypes.bool,
  errors: WidgetErrorsList,
  fields: PropTypes.any.isRequired,
  height: PropTypes.number,
  id: PropTypes.string.isRequired,
  onPositionsChange: PropTypes.func.isRequired,
  onSizeChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  widget: PropTypes.instanceOf(WidgetModel).isRequired,
  width: PropTypes.number,
};

Widget.defaultProps = {
  height: 1,
  width: 1,
  data: undefined,
  errors: undefined,
  editing: false,
};

export default connect(Widget, { view: ViewStore });
