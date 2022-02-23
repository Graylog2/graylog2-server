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
import { useCallback, useContext, useMemo, useState } from 'react';
import type * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { BackendWidgetPosition, WidgetResults } from 'views/types';
import { useStore } from 'stores/connect';
import { widgetDefinition } from 'views/logic/Widgets';
import type { Widgets } from 'views/stores/WidgetStore';
import { WidgetActions } from 'views/stores/WidgetStore';
import { TitlesActions } from 'views/stores/TitlesStore';
import { ViewStore } from 'views/stores/ViewStore';
import { RefreshActions } from 'views/stores/RefreshStore';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import WidgetModel from 'views/logic/widgets/Widget';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import TimerangeInfo from 'views/components/widgets/TimerangeInfo';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import useWidgetResults from 'views/components/useWidgetResults';

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
  widget: WidgetModel,
  editing: boolean,
  fields: Immutable.List<FieldTypeMapping>,
  title: string,
  position: WidgetPosition,
  onPositionsChange: (position: BackendWidgetPosition) => void,
};

export type Result = {
  total: number,
  rows: Rows,
  effective_timerange: AbsoluteTimeRange,
};

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

type VisualizationProps = Pick<Props, 'title' | 'id' | 'widget' | 'fields' | 'editing'> & {
  queryId: string,
  setLoadingState: (loading: boolean) => void,
  onToggleEdit: () => void,
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => Promise<Widgets>,
};

const Visualization = ({ title, id, widget, fields, queryId, editing, setLoadingState, onToggleEdit, onWidgetConfigChange }: VisualizationProps) => {
  const VisComponent = useMemo(() => _visualizationForType(widget.type), [widget.type]);
  const { error: errors, widgetData: data } = useWidgetResults(id);

  if (errors && errors.length > 0) {
    return <ErrorWidget errors={errors} />;
  }

  if (data) {
    const { config, filter } = widget;

    return (
      <VisComponent config={config}
                    data={data as WidgetResults}
                    editing={editing}
                    fields={fields}
                    filter={filter}
                    queryId={queryId}
                    onConfigChange={onWidgetConfigChange}
                    setLoadingState={setLoadingState}
                    title={title}
                    toggleEdit={onToggleEdit}
                    type={widget.type}
                    id={id} />
    );
  }

  return <LoadingWidget />;
};

type EditWrapperProps = {
  children: React.ReactElement,
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
  const EditComponent = useMemo(() => _editComponentForType(type), [type]);

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
  ) : children;
};

const Widget = ({ id, editing, widget, fields, title, position, onPositionsChange }: Props) => {
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
      WidgetActions.update(id, oldWidget).then(() => {});
    }

    onToggleEdit();
  }, [id, oldWidget, onToggleEdit]);
  const onRenameWidget = useCallback((newTitle: string) => TitlesActions.set('widget', id, newTitle), [id]);
  const onWidgetConfigChange = useCallback((newWidgetConfig: WidgetConfig) => WidgetActions.updateConfig(id, newWidgetConfig), [id]);
  const activeQuery = useStore(ViewStore, ({ activeQuery: currentQuery }) => currentQuery);

  const { config } = widget;
  const isFocused = focusedWidget?.id === id;

  return (
    <WidgetColorContext id={id}>
      <WidgetFrame widgetId={id}>
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
                           editing={editing}
                           queryId={activeQuery}
                           widget={widget}
                           fields={fields}
                           title={title}
                           setLoadingState={setLoading}
                           onToggleEdit={onToggleEdit}
                           onWidgetConfigChange={onWidgetConfigChange} />
          </WidgetErrorBoundary>
        </EditWrapper>
        <WidgetFooter>
          <IfDashboard>
            {!editing && <TimerangeInfo widget={widget} activeQuery={activeQuery} widgetId={id} />}
          </IfDashboard>
        </WidgetFooter>
      </WidgetFrame>
    </WidgetColorContext>
  );
};

Widget.propTypes = {
  editing: PropTypes.bool,
  fields: PropTypes.any.isRequired,
  id: PropTypes.string.isRequired,
  onPositionsChange: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  widget: PropTypes.instanceOf(WidgetModel).isRequired,
};

Widget.defaultProps = {
  editing: false,
};

export default Widget;
