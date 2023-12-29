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
import { useState, useContext, useCallback } from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { getPathnameWithoutId } from 'util/URLUtils';
import type { BackendWidgetPosition } from 'views/types';
import ExportModal from 'views/components/export/ExportModal';
import MoveWidgetToTab from 'views/logic/views/MoveWidgetToTab';
import { loadAsDashboard, loadDashboard } from 'views/logic/views/Actions';
import { IconButton } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import IfSearch from 'views/components/search/IfSearch';
import { MenuItem } from 'components/bootstrap';
import type Widget from 'views/logic/widgets/Widget';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import DrilldownContext from 'views/components/contexts/DrilldownContext';
import useView from 'views/hooks/useView';
import createSearch from 'views/logic/slices/createSearch';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery, updateView } from 'views/logic/slices/viewSlice';
import { duplicateWidget, removeWidget } from 'views/logic/slices/widgetActions';
import fetchSearch from 'views/logic/views/fetchSearch';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useParameters from 'views/hooks/useParameters';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import ExtractWidgetIntoNewView from 'views/logic/views/ExtractWidgetIntoNewView';

import ReplaySearchButton from './ReplaySearchButton';
import ExtraWidgetActions from './ExtraWidgetActions';
import CopyToDashboard from './CopyToDashboardForm';
import MoveWidgetToTabModal from './MoveWidgetToTabModal';
import WidgetActionDropdown from './WidgetActionDropdown';
import WidgetHorizontalStretch from './WidgetHorizontalStretch';

import IfInteractive from '../dashboard/IfInteractive';
import IfDashboard from '../dashboard/IfDashboard';
import WidgetFocusContext from '../contexts/WidgetFocusContext';
import WidgetContext from '../contexts/WidgetContext';

const Container = styled.div`
  > *:not(:last-child) {
    margin-right: 2px;
  }
`;

const _onCopyToDashboard = async (
  view: View,
  setShowCopyToDashboard: (show: boolean) => void,
  widgetId: string,
  dashboardId: string | undefined | null,
  history: HistoryFunction,
) => {
  if (!dashboardId) {
    return;
  }

  const dashboardJson = await ViewManagementActions.get(dashboardId);
  const dashboard = View.fromJSON(dashboardJson);
  const search = await fetchSearch(dashboardJson.search_id).then((searchJson) => Search.fromJSON(searchJson));
  const newDashboard = CopyWidgetToDashboard(widgetId, view, dashboard.toBuilder().search(search).build());

  if (newDashboard && newDashboard.search) {
    const newSearch = await createSearch(newDashboard.search);
    const newDashboardWithSearch = newDashboard.toBuilder().search(newSearch).build();
    await ViewManagementActions.update(newDashboardWithSearch);

    loadDashboard(history, newDashboardWithSearch.id);
  }

  setShowCopyToDashboard(false);
};

const _onCreateNewDashboard = async (view: View, widgetId: string, history: HistoryFunction) => {
  const newView = ExtractWidgetIntoNewView(view, widgetId);

  loadAsDashboard(history, newView);
};

const _onMoveWidgetToPage = async (
  dispatch: AppDispatch,
  view: View,
  setShowMoveWidgetToTab: (show: boolean) => void,
  widgetId: string,
  queryId: string,
  keepCopy: boolean,
) => {
  if (!queryId) {
    return;
  }

  const newDashboard = MoveWidgetToTab(widgetId, queryId, view, keepCopy);

  if (newDashboard) {
    const searchResponse = await createSearch(newDashboard.search);
    const updatedDashboard = newDashboard.toBuilder().search(searchResponse).build();
    await dispatch(updateView(updatedDashboard, true));
    setShowMoveWidgetToTab(false);
    await dispatch(selectQuery(queryId));
  }
};

// eslint-disable-next-line no-alert
const defaultOnDeleteWidget = async (_widget: Widget, _view: View, title: string) => window.confirm(`Are you sure you want to remove the widget "${title}"?`);

const _onDelete = (widget: Widget, view: View, title: string) => async (dispatch: AppDispatch) => {
  const pluggableWidgetDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingWidget');

  const result = await iterateConfirmationHooks([...pluggableWidgetDeletionHooks, defaultOnDeleteWidget], widget, view, title);

  return result === true ? dispatch(removeWidget(widget.id)) : Promise.resolve();
};

const _onDuplicate = (widgetId: string, unsetWidgetFocusing: () => void, title: string) => (dispatch: AppDispatch) => dispatch(duplicateWidget(widgetId, title)).then(() => unsetWidgetFocusing());

type Props = {
  isFocused: boolean,
  onPositionsChange: (position: BackendWidgetPosition) => void,
  position: WidgetPosition,
  title: string,
  toggleEdit: () => void
};

const WidgetActionsMenu = ({
  isFocused,
  onPositionsChange,
  position,
  title,
  toggleEdit,
}: Props) => {
  const widget = useContext(WidgetContext);
  const view = useView();
  const { query, timerange, streams } = useContext(DrilldownContext);
  const { setWidgetFocusing, unsetWidgetFocusing } = useContext(WidgetFocusContext);
  const [showCopyToDashboard, setShowCopyToDashboard] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showMoveWidgetToTab, setShowMoveWidgetToTab] = useState(false);
  const dispatch = useAppDispatch();
  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { parameters, parameterBindings } = useParameters();

  const onDuplicate = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.DUPLICATE, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-duplicate-button',
    });

    return dispatch(_onDuplicate(widget.id, unsetWidgetFocusing, title));
  }, [sendTelemetry, pathname, dispatch, widget.id, unsetWidgetFocusing, title]);
  const onCopyToDashboard = useCallback((widgetId: string, dashboardId: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.COPY_TO_DASHBOARD, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-copy-to-dashboard-button',
    });

    return _onCopyToDashboard(view, setShowCopyToDashboard, widgetId, dashboardId, history);
  }, [history, pathname, sendTelemetry, view]);

  const onCreateNewDashboard = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.CREATE_NEW_DASHBOARD, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-create-new-dashboard-button',
    });

    return _onCreateNewDashboard(view, widget.id, history);
  }, [sendTelemetry, pathname, view, widget.id, history]);

  const onMoveWidgetToTab = useCallback((widgetId: string, queryId: string, keepCopy: boolean) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.MOVE, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-move-button',
    });

    return _onMoveWidgetToPage(dispatch, view, setShowMoveWidgetToTab, widgetId, queryId, keepCopy);
  }, [dispatch, pathname, sendTelemetry, view]);
  const onDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.DELETED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-delete-button',
    });

    return dispatch(_onDelete(widget, view, title));
  }, [dispatch, pathname, sendTelemetry, title, view, widget]);
  const focusWidget = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.FOCUSED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search-widget',
      app_action_value: 'widget-focus-button',
    });

    return setWidgetFocusing(widget.id);
  }, [pathname, sendTelemetry, setWidgetFocusing, widget.id]);

  return (
    <Container>
      <IfInteractive>
        <IfDashboard>
          <ReplaySearchButton queryString={query.query_string}
                              timerange={timerange}
                              streams={streams}
                              parameterBindings={parameterBindings}
                              parameters={parameters} />
        </IfDashboard>
        {isFocused && (
          <IconButton name="compress-arrows-alt"
                      title="Un-focus widget"
                      onClick={unsetWidgetFocusing} />
        )}
        {!isFocused && (
          <>
            <WidgetHorizontalStretch widgetId={widget.id}
                                     widgetType={widget.type}
                                     onStretch={onPositionsChange}
                                     position={position} />
            <IconButton name="expand-arrows-alt"
                        title="Focus this widget"
                        onClick={focusWidget} />
          </>
        )}

        <IconButton name="edit"
                    title="Edit"
                    onClick={toggleEdit} />

        <WidgetActionDropdown>
          <MenuItem onSelect={onDuplicate}>
            Duplicate
          </MenuItem>
          <IfSearch>
            <MenuItem onSelect={() => setShowCopyToDashboard(true)}>
              Copy to Dashboard
            </MenuItem>
          </IfSearch>
          {widget.isExportable && <MenuItem onSelect={() => setShowExport(true)}>Export</MenuItem>}
          <IfDashboard>
            <MenuItem onSelect={() => setShowMoveWidgetToTab(true)}>
              Move to Page
            </MenuItem>
          </IfDashboard>
          <ExtraWidgetActions widget={widget} />
          <MenuItem divider />
          <MenuItem onSelect={onDelete}>
            Delete
          </MenuItem>
        </WidgetActionDropdown>

        {showCopyToDashboard && (
          <CopyToDashboard onCopyToDashboard={(dashboardId) => onCopyToDashboard(widget.id, dashboardId)}
                           onCancel={() => setShowCopyToDashboard(false)}
                           submitLoadingText="Copying widget..."
                           submitButtonText="Copy widget"
                           onCreateNewDashboard={onCreateNewDashboard} />
        )}

        {showExport && (
          <ExportModal view={view}
                       directExportWidgetId={widget.id}
                       closeModal={() => setShowExport(false)} />
        )}

        {showMoveWidgetToTab && (
          <MoveWidgetToTabModal view={view}
                                widgetId={widget.id}
                                onCancel={() => setShowMoveWidgetToTab(false)}
                                onSubmit={onMoveWidgetToTab} />
        )}
      </IfInteractive>
    </Container>
  );
};

export default WidgetActionsMenu;
