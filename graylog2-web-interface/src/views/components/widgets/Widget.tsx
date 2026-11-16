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

import type { BackendWidgetPosition } from 'views/types';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import TimerangeInfo from 'views/components/widgets/TimerangeInfo';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { updateWidget, updateWidgetConfig, setWidgetTitle, updateDescription } from 'views/logic/slices/widgetActions';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import type WidgetType from 'views/logic/widgets/Widget';
import {
  useSendWidgetEditTelemetry,
  useSendWidgetEditCancelTelemetry,
  useSendWidgetConfigUpdateTelemetry,
} from 'views/components/widgets/telemetry';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import { setGlobalOverrideTimerange, setGlobalOverrideQuery } from 'views/logic/slices/searchExecutionSlice';
import type GlobalOverride from 'views/logic/search/GlobalOverride';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';

import WidgetFrame from './WidgetFrame';
import WidgetHeader from './WidgetHeader';
import WidgetEditWrapper from './WidgetEditWrapper';
import WidgetVisualization from './WidgetVisualization';
import WidgetColorContext from './WidgetColorContext';
import WidgetErrorBoundary from './WidgetErrorBoundary';
import WidgetActionsMenu from './WidgetActionsMenu';
import WidgetWarmTierAlert from './WidgetWarmTierAlert';

import { useIsInteractiveMode } from '../contexts/InteractiveContext';

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
  const interactive = useIsInteractiveMode();

  const isDashboard = viewType === View.Type.Dashboard;

  const { onCancelEdit, onToggleEdit } = useUndoChangesOnCancel(id, editing, widget);
  const onRenameWidget = useCallback((newTitle: string) => dispatch(setWidgetTitle(id, newTitle)), [dispatch, id]);
  const onUpdateDescription = useCallback(
    (newDescription: string) => dispatch(updateDescription(widget, newDescription)),
    [dispatch, widget],
  );

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

  const onWidgetConfigChange = useCallback(
    async (newWidgetConfig: WidgetConfig) => {
      sendWidgetConfigUpdateTelemetry(
        newWidgetConfig instanceof AggregationWidgetConfig
          ? {
              visualizationType: newWidgetConfig.visualization,
              visualizationConfig: newWidgetConfig.visualizationConfig,
            }
          : {},
      );

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
        <WidgetEditWrapper
          onSubmit={onSubmitEdit}
          onCancelEdit={onCancelEdit}
          showQueryControls={isDashboard}
          onWidgetConfigChange={onWidgetConfigChange}
          config={config}
          editing={editing}
          fields={fields}
          id={id}
          type={widget.type}>
          <WidgetErrorBoundary>
            <WidgetVisualization
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
        </WidgetEditWrapper>
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
