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

import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { loadDashboard } from 'views/logic/views/Actions';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { TitlesActions, TitleTypes } from 'views/stores/TitlesStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from 'views/logic/views/View';
import SearchActions from 'views/actions/SearchActions';
import CopyWidgetToDashboard from 'views/logic/views/CopyWidgetToDashboard';
import Search from 'views/logic/search/Search';
import MoveWidgetToTab from 'views/logic/views/MoveWidgetToTab';
import type { ViewStoreState } from 'views/stores/ViewStore';
import CSVExportModal from 'views/components/searchbar/csvexport/CSVExportModal';
import IfSearch from 'views/components/search/IfSearch';
import { MenuItem } from 'components/graylog';
import { IconButton } from 'components/common';
import { WidgetActions } from 'views/stores/WidgetStore';

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
  > * {
    margin-right: 5px;

    :last-child {
      margin-right: 0;
    }
  }
`;

const _updateDashboardWithNewSearch = (dashboard: View, dashboardId: string) => ({ search: newSearch }) => {
  const newDashboard = dashboard.toBuilder().search(newSearch).build();

  ViewManagementActions.update(newDashboard).then(() => loadDashboard(dashboardId));
};

const _onCopyToDashboard = (
  view: ViewStoreState,
  setShowCopyToDashboard: (show: boolean) => void,
  widgetId: string,
  dashboardId: string | undefined | null,
): void => {
  const { view: activeView } = view;

  if (!dashboardId) {
    return;
  }

  const addWidgetToDashboard = (dashboard: View) => (searchJson) => {
    const search = Search.fromJSON(searchJson);
    const newDashboard = CopyWidgetToDashboard(widgetId, activeView, dashboard.toBuilder().search(search).build());

    if (newDashboard && newDashboard.search) {
      SearchActions.create(newDashboard.search).then(_updateDashboardWithNewSearch(newDashboard, dashboardId));
    }
  };

  ViewManagementActions.get(dashboardId).then((dashboardJson) => {
    const dashboard = View.fromJSON(dashboardJson);

    SearchActions.get(dashboardJson.search_id).then(addWidgetToDashboard(dashboard));
  });

  setShowCopyToDashboard(false);
};

const _onMoveWidgetToTab = (view, setShowMoveWidgetToTab, widgetId, queryId, keepCopy) => {
  const { view: activeView } = view;

  if (!queryId) {
    return;
  }

  const newDashboard = MoveWidgetToTab(widgetId, queryId, activeView, keepCopy);

  if (newDashboard) {
    SearchActions.create(newDashboard.search).then((searchResponse) => {
      const updatedDashboard = newDashboard.toBuilder().search(searchResponse.search).build();

      ViewActions.update(updatedDashboard).then(() => {
        setShowMoveWidgetToTab(false);

        ViewActions.selectQuery(queryId).then(() => {
          SearchActions.executeWithCurrentState();
        });
      });
    });
  }
};

const _onDelete = (widgetId, title) => {
  // eslint-disable-next-line no-alert
  if (window.confirm(`Are you sure you want to remove the widget "${title}"?`)) {
    WidgetActions.remove(widgetId);
  }
};

const _onDuplicate = (widgetId, setFocusWidget, title) => {
  WidgetActions.duplicate(widgetId).then((newWidget) => {
    TitlesActions.set(TitleTypes.Widget, newWidget.id, `${title} (copy)`).then(() => {
      setFocusWidget(undefined);
    });
  });
};

type Props = {
  isFocused: boolean,
  onPositionsChange: () => void,
  position: WidgetPosition,
  title: string,
  toggleEdit: () => void
  view: ViewStoreState,
};

const WidgetActionsMenu = ({
  isFocused,
  onPositionsChange,
  position,
  title,
  toggleEdit,
  view,
}: Props) => {
  const widget = useContext(WidgetContext);
  const { setWidgetFocusing } = useContext(WidgetFocusContext);
  const [showCopyToDashboard, setShowCopyToDashboard] = useState(false);
  const [showCsvExport, setShowCsvExport] = useState(false);
  const [showMoveWidgetToTab, setShowMoveWidgetToTab] = useState(false);

  const onDuplicate = () => _onDuplicate(widget.id, setWidgetFocusing, title);
  const onCopyToDashboard = useCallback((widgetId, dashboardId) => _onCopyToDashboard(view, setShowCopyToDashboard, widgetId, dashboardId), [view]);
  const onMoveWidgetToTab = useCallback((widgetId, queryId, keepCopy) => _onMoveWidgetToTab(view, setShowMoveWidgetToTab, widgetId, queryId, keepCopy), [view]);

  return (
    <Container>
      <IfInteractive>
        <IfDashboard>
          <ReplaySearchButton />
        </IfDashboard>
        {isFocused && (
          <IconButton name="compress-arrows-alt"
                      title="Un-focus widget"
                      onClick={() => setWidgetFocusing(undefined)} />
        )}
        {!isFocused && (
          <>
            <IconButton name="expand-arrows-alt"
                        title="Focus this widget"
                        onClick={() => setWidgetFocusing(widget.id)} />
            <WidgetHorizontalStretch widgetId={widget.id}
                                     widgetType={widget.type}
                                     onStretch={onPositionsChange}
                                     position={position} />
          </>
        )}

        <WidgetActionDropdown>
          <MenuItem onSelect={toggleEdit}>
            Edit
          </MenuItem>
          <MenuItem onSelect={onDuplicate}>
            Duplicate
          </MenuItem>
          {widget.isExportable && (
            <MenuItem onSelect={() => setShowCsvExport(true)}>
              Export to CSV
            </MenuItem>
          )}
          <IfSearch>
            <MenuItem onSelect={() => setShowCopyToDashboard(true)}>
              Copy to Dashboard
            </MenuItem>
          </IfSearch>
          <IfDashboard>
            <MenuItem onSelect={() => setShowMoveWidgetToTab(true)}>
              Move to Page
            </MenuItem>
          </IfDashboard>
          <ExtraWidgetActions widget={widget} onSelect={() => {}} />
          <MenuItem divider />
          <MenuItem onSelect={() => _onDelete(widget.id, title)}>
            Delete
          </MenuItem>
        </WidgetActionDropdown>

        {showCopyToDashboard && (
          <CopyToDashboard widgetId={widget.id}
                           onSubmit={onCopyToDashboard}
                           onCancel={() => setShowCopyToDashboard(false)} />
        )}
        {showCsvExport && (
          <CSVExportModal view={view.view}
                          directExportWidgetId={widget.id}
                          closeModal={() => setShowCsvExport(false)} />
        )}
        {showMoveWidgetToTab && (
          <MoveWidgetToTabModal view={view.view}
                                widgetId={widget.id}
                                onCancel={() => setShowMoveWidgetToTab(false)}
                                onSubmit={onMoveWidgetToTab} />
        )}
      </IfInteractive>
    </Container>
  );
};

export default WidgetActionsMenu;
