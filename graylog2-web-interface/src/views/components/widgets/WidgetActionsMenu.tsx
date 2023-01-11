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

import type { BackendWidgetPosition } from 'views/types';
import ExportModal from 'views/components/export/ExportModal';
import MoveWidgetToTab from 'views/logic/views/MoveWidgetToTab';
import { loadDashboard } from 'views/logic/views/Actions';
import { IconButton } from 'components/common';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import SearchActions from 'views/actions/SearchActions';
import Search from 'views/logic/search/Search';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import IfSearch from 'views/components/search/IfSearch';
import { MenuItem } from 'components/bootstrap';
import { WidgetActions } from 'views/stores/WidgetStore';
import type Widget from 'views/logic/widgets/Widget';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import useView from 'views/hooks/useView';
import createSearch from 'views/logic/slices/createSearch';

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
) => {
  if (!dashboardId) {
    return;
  }

  const dashboardJson = await ViewManagementActions.get(dashboardId);
  const dashboard = View.fromJSON(dashboardJson);
  const search = await SearchActions.get(dashboardJson.search_id).then((searchJson) => Search.fromJSON(searchJson));
  const newDashboard = CopyWidgetToDashboard(widgetId, view, dashboard.toBuilder().search(search).build());

  if (newDashboard && newDashboard.search) {
    const newSearch = await createSearch(newDashboard.search);
    const newDashboardWithSearch = newDashboard.toBuilder().search(newSearch).build();
    await ViewManagementActions.update(newDashboardWithSearch);

    loadDashboard(newDashboardWithSearch.id);
  }

  setShowCopyToDashboard(false);
};

const _onMoveWidgetToTab = (
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
    SearchActions.create(newDashboard.search)
      .then((searchResponse) => {
        const updatedDashboard = newDashboard.toBuilder().search(searchResponse.search).build();

        return ViewActions.update(updatedDashboard);
      })
      .then(() => {
        setShowMoveWidgetToTab(false);

        return ViewActions.selectQuery(queryId);
      })
      .then(() => SearchActions.executeWithCurrentState());
  }
};

// eslint-disable-next-line no-alert
const defaultOnDeleteWidget = async (_widget: Widget, _view: View, title: string) => window.confirm(`Are you sure you want to remove the widget "${title}"?`);

const _onDelete = async (widget: Widget, view: View, title: string) => {
  const pluggableWidgetDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingWidget');

  const result = await iterateConfirmationHooks([...pluggableWidgetDeletionHooks, defaultOnDeleteWidget], widget, view, title);

  return result === true ? WidgetActions.remove(widget.id) : Promise.resolve();
};

const _onDuplicate = (widgetId: string, unsetWidgetFocusing: () => void, title: string) => {
  WidgetActions.duplicate(widgetId).then((newWidget) => {
    TitlesActions.set(TitleTypes.Widget, newWidget.id, `${title} (copy)`).then(() => {
      unsetWidgetFocusing();
    });
  });
};

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
  const { setWidgetFocusing, unsetWidgetFocusing } = useContext(WidgetFocusContext);
  const [showCopyToDashboard, setShowCopyToDashboard] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showMoveWidgetToTab, setShowMoveWidgetToTab] = useState(false);

  const onDuplicate = useCallback(() => _onDuplicate(widget.id, unsetWidgetFocusing, title), [unsetWidgetFocusing, title, widget.id]);
  const onCopyToDashboard = useCallback((widgetId: string, dashboardId: string) => _onCopyToDashboard(view, setShowCopyToDashboard, widgetId, dashboardId), [view]);
  const onMoveWidgetToTab = useCallback((widgetId: string, queryId: string, keepCopy: boolean) => _onMoveWidgetToTab(view, setShowMoveWidgetToTab, widgetId, queryId, keepCopy), [view]);
  const onDelete = useCallback(() => _onDelete(widget, view, title), [title, view, widget]);
  const focusWidget = useCallback(() => setWidgetFocusing(widget.id), [setWidgetFocusing, widget.id]);

  return (
    <Container>
      <IfInteractive>
        <IfDashboard>
          <ReplaySearchButton />
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
          <ExtraWidgetActions widget={widget} onSelect={() => {}} />
          <MenuItem divider />
          <MenuItem onSelect={onDelete}>
            Delete
          </MenuItem>
        </WidgetActionDropdown>

        {showCopyToDashboard && (
          <CopyToDashboard widgetId={widget.id}
                           onSubmit={onCopyToDashboard}
                           onCancel={() => setShowCopyToDashboard(false)} />
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
