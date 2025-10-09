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
import styled, { css } from 'styled-components';
import isEqual from 'lodash/isEqual';

import type { BackendWidgetPosition, WidgetResults } from 'views/types';
import { widgetDefinition } from 'views/logic/Widgets';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import TimerangeInfo from 'views/components/widgets/TimerangeInfo';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import useWidgetResults from 'views/components/useWidgetResults';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { updateWidget, updateWidgetConfig, setWidgetTitle, updateDescription } from 'views/logic/slices/widgetActions';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import FullSizeContainer from 'views/components/aggregationbuilder/FullSizeContainer';
import type WidgetType from 'views/logic/widgets/Widget';
import {
  useSendWidgetEditTelemetry,
  useSendWidgetEditCancelTelemetry,
  useSendWidgetConfigUpdateTelemetry,
} from 'views/components/widgets/telemety';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import { setGlobalOverrideTimerange, setGlobalOverrideQuery } from 'views/logic/slices/searchExecutionSlice';
import type GlobalOverride from 'views/logic/search/GlobalOverride';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import EditWidgetFrame from './EditWidgetFrame';
import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';
import WidgetColorContext from './WidgetColorContext';
import WidgetErrorBoundary from './WidgetErrorBoundary';
import WidgetActionsMenu from './WidgetActionsMenu';
import WidgetWarmTierAlert from './WidgetWarmTierAlert';

import InteractiveContext from '../contexts/InteractiveContext';

export type Props = {
  id: string;
  widget: WidgetType;
  editing?: boolean;
  title: string;
  position: WidgetPosition;
  onPositionsChange: (position: BackendWidgetPosition) => void;
};

export type Result = {
  total: number;
  rows: Rows;
  effective_timerange: AbsoluteTimeRange;
};

const _visualizationForType = (type: string) => widgetDefinition(type).visualizationComponent;

const _editComponentForType = (type: string) => widgetDefinition(type).editComponent;

const _hasOwnEditSubmitButton = (type: string) => widgetDefinition(type).hasEditSubmitButton;

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);

  return useMemo(() => fieldTypes.currentQuery, [fieldTypes.currentQuery]);
};

const WidgetFooter = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.tiny};
    color: ${theme.colors.gray[30]};
    width: 100%;
    display: flex;
    justify-content: space-between;
    gap: 10px;
  `,
);

type VisualizationProps = Pick<Props, 'title' | 'id' | 'widget' | 'editing'> & {
  queryId: string;
  setLoadingState: (loading: boolean) => void;
  onToggleEdit: () => void;
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => Promise<void>;
  fields: FieldTypeMappingsList;
};

const Visualization = ({
  title,
  id,
  widget,
  fields,
  queryId,
  editing,
  setLoadingState,
  onToggleEdit,
  onWidgetConfigChange,
}: VisualizationProps) => {
  const VisComponent = useMemo(() => _visualizationForType(widget.type), [widget.type]);
  const { error: errors, widgetData: data } = useWidgetResults(id);

  if (errors && errors.length > 0) {
    return <ErrorWidget errors={errors} />;
  }

  if (data) {
    const { config, filter } = widget;

    return (
      <FullSizeContainer>
        {({ height, width }) => (
          <VisComponent
            config={config}
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
            id={id}
            height={height}
            width={width}
          />
        )}
      </FullSizeContainer>
    );
  }

  return <LoadingWidget />;
};

type EditWrapperProps = {
  children: React.ReactElement;
  config: WidgetConfig;
  editing: boolean;
  fields: FieldTypeMappingsList;
  id: string;
  onCancelEdit: () => void;
  onToggleEdit: () => void;
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => void;
  showQueryControls?: boolean;
  type: string;
};

export const EditWrapper = ({
  children,
  config,
  editing,
  fields,
  id,
  onToggleEdit,
  onCancelEdit,
  onWidgetConfigChange,
  type,
  showQueryControls = undefined,
}: EditWrapperProps) => {
  const EditComponent = useMemo(() => _editComponentForType(type), [type]);
  const hasOwnSubmitButton = _hasOwnEditSubmitButton(type);
  const dispatch = useViewsDispatch();
  const onSubmitEdit = useCallback(
    (newWidget: WidgetType, hasChanges: boolean) => {
      if (hasChanges) {
        return dispatch(updateWidget(newWidget.id, newWidget)).then(() => onToggleEdit());
      }

      onToggleEdit();

      return Promise.resolve();
    },
    [dispatch, onToggleEdit],
  );

  return editing ? (
    <EditWidgetFrame
      onSubmit={onSubmitEdit}
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

const WidgetDescription = ({ text }: { text: string }) => <TextOverflowEllipsis>{text}</TextOverflowEllipsis>;

type PreviousState =
  | {
      widget: WidgetType;
      globalOverride: GlobalOverride;
    }
  | {
      widget: undefined;
      globalOverride: undefined;
    };
const useUndoChangesOnCancel = (id: string, editing: boolean, widget: WidgetType) => {
  const globalOverride = useGlobalOverride();
  const [previousState, setPreviousState] = useState<PreviousState>(editing ? { widget, globalOverride } : undefined);
  const clearPreviousState = useCallback(() => setPreviousState(undefined), []);
  const { stopAutoRefresh } = useAutoRefresh();
  const dispatch = useViewsDispatch();
  const { setWidgetEditing, unsetWidgetEditing } = useContext(WidgetFocusContext);
  const sendWidgetEditTelemetry = useSendWidgetEditTelemetry();
  const sendWidgetEditCancelTelemetry = useSendWidgetEditCancelTelemetry();
  const onToggleEdit = useCallback(() => {
    if (editing) {
      unsetWidgetEditing();
      clearPreviousState();
    } else {
      sendWidgetEditTelemetry();
      stopAutoRefresh();
      setWidgetEditing(widget.id);
      setPreviousState({ widget, globalOverride });
    }
  }, [
    clearPreviousState,
    editing,
    globalOverride,
    sendWidgetEditTelemetry,
    setWidgetEditing,
    stopAutoRefresh,
    unsetWidgetEditing,
    widget,
  ]);
  const onCancelEdit = useCallback(() => {
    sendWidgetEditCancelTelemetry();

    if (previousState) {
      dispatch(updateWidget(id, previousState.widget));

      if (!isEqual(previousState.globalOverride?.timerange, globalOverride?.timerange)) {
        dispatch(setGlobalOverrideTimerange(previousState.globalOverride.timerange));
      }

      if (!isEqual(previousState.globalOverride?.query, globalOverride?.query)) {
        dispatch(setGlobalOverrideQuery(previousState.globalOverride.query.query_string));
      }
    }

    onToggleEdit();
  }, [
    dispatch,
    globalOverride?.query,
    globalOverride?.timerange,
    id,
    onToggleEdit,
    previousState,
    sendWidgetEditCancelTelemetry,
  ]);

  return useMemo(() => ({ onCancelEdit, onToggleEdit }), [onCancelEdit, onToggleEdit]);
};
const Widget = ({ id, editing = false, widget, title, position, onPositionsChange }: Props) => {
  const viewType = useViewType();
  const fields = useQueryFieldTypes();
  const [loading, setLoading] = useState(false);
  const { focusedWidget } = useContext(WidgetFocusContext);
  const dispatch = useViewsDispatch();
  const sendWidgetConfigUpdateTelemetry = useSendWidgetConfigUpdateTelemetry();
  const interactive = useContext(InteractiveContext);

  const isDashboard = viewType === View.Type.Dashboard;

  const { onCancelEdit, onToggleEdit } = useUndoChangesOnCancel(id, editing, widget);
  const onRenameWidget = useCallback((newTitle: string) => dispatch(setWidgetTitle(id, newTitle)), [dispatch, id]);
  const onUpdateDescription = useCallback(
    (newDescription: string) => dispatch(updateDescription(widget, newDescription)),
    [dispatch, widget],
  );

  const onWidgetConfigChange = useCallback(
    async (newWidgetConfig: WidgetConfig) => {
      sendWidgetConfigUpdateTelemetry();

      return dispatch(updateWidgetConfig(id, newWidgetConfig)).then(() => {});
    },
    [dispatch, id, sendWidgetConfigUpdateTelemetry],
  );
  const activeQuery = useActiveQueryId();

  const { config } = widget;
  const isFocused = focusedWidget?.id === id;
  const titleIcon = (
    <IfDashboard>{!editing && <WidgetWarmTierAlert widgetId={id} activeQuery={activeQuery} />}</IfDashboard>
  );

  return (
    <WidgetColorContext id={id}>
      <WidgetFrame widgetId={id}>
        <WidgetHeader
          description={widget.description}
          title={title}
          titleIcon={titleIcon}
          hideDragHandle={!interactive || isFocused}
          loading={loading}
          editing={editing}
          onRename={onRenameWidget}
          onUpdateDescription={onUpdateDescription}>
          {!editing ? (
            <WidgetActionsMenu
              isFocused={isFocused}
              toggleEdit={onToggleEdit}
              title={title}
              position={position}
              onPositionsChange={onPositionsChange}
            />
          ) : null}
        </WidgetHeader>
        <EditWrapper
          onToggleEdit={onToggleEdit}
          onCancelEdit={onCancelEdit}
          showQueryControls={isDashboard}
          onWidgetConfigChange={onWidgetConfigChange}
          config={config}
          editing={editing}
          fields={fields}
          id={id}
          type={widget.type}>
          <WidgetErrorBoundary>
            <Visualization
              id={id}
              editing={editing}
              queryId={activeQuery}
              widget={widget}
              fields={fields}
              title={title}
              setLoadingState={setLoading}
              onToggleEdit={onToggleEdit}
              onWidgetConfigChange={onWidgetConfigChange}
            />
          </WidgetErrorBoundary>
        </EditWrapper>
        <WidgetFooter>
          {interactive ? <span /> : <WidgetDescription text={widget.description} />}
          {(widget.returnsAllRecords || isDashboard) && !editing && (
            <TimerangeInfo
              widget={widget}
              activeQuery={activeQuery}
              widgetId={id}
              returnsAllRecords={widget.returnsAllRecords}
            />
          )}
        </WidgetFooter>
      </WidgetFrame>
    </WidgetColorContext>
  );
};

export default Widget;
