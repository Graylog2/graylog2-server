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
import React, { useState, useCallback, useMemo, useRef } from 'react';

import { isPermitted } from 'util/PermissionsMixin';
import AppConfig from 'util/AppConfig';
import { DropdownButton, MenuItem, Button, ButtonGroup } from 'components/bootstrap';
import { Icon, ShareButton } from 'components/common';
import ExportModal from 'views/components/export/ExportModal';
import DebugOverlay from 'views/components/DebugOverlay';
import onSaveNewDashboard from 'views/logic/views/OnSaveNewDashboard';
import * as ViewPermissions from 'views/Permissions';
import useSearchPageLayout from 'hooks/useSearchPageLayout';
import View from 'views/logic/views/View';
import type User from 'logic/users/User';
import useCurrentUser from 'hooks/useCurrentUser';
import EntityShareModal from 'components/permissions/EntityShareModal';
import {
  executePluggableDashboardDuplicationHandler as executePluggableDuplicationHandler,
} from 'views/logic/views/pluggableSaveViewFormHandler';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import useView from 'views/hooks/useView';
import useIsNew from 'views/hooks/useIsNew';
import useHasUndeclaredParameters from 'views/logic/parameters/useHasUndeclaredParameters';
import useAppDispatch from 'stores/useAppDispatch';
import usePluginEntities from 'hooks/usePluginEntities';
import { updateView } from 'views/logic/slices/viewSlice';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import useHistory from 'routing/useHistory';
import SaveViewButton from 'views/components/searchbar/SaveViewButton';
import useHotkey from 'hooks/useHotkey';

import DashboardPropertiesModal from './dashboard/DashboardPropertiesModal';
import BigDisplayModeConfiguration from './dashboard/BigDisplayModeConfiguration';

const _isAllowedToEdit = (view: View, currentUser: User | undefined | null) => isPermitted(currentUser?.permissions, [ViewPermissions.View.Edit(view.id)])
  || (view.type === View.Type.Dashboard && isPermitted(currentUser?.permissions, [`dashboards:edit:${view.id}`]));

const DashboardActionsMenu = () => {
  const view = useView();
  const isNewView = useIsNew();
  const currentUser = useCurrentUser();
  const hasUndeclaredParameters = useHasUndeclaredParameters();
  const {
    viewActions: {
      save: {
        isShown: showSaveButton,
      }, saveAs: {
        isShown: showSaveNewButton,
      }, share: {
        isShown: showShareButton,
      }, actionsDropdown: {
        isShown: showDropDownButton,
      },
    },
  } = useSearchPageLayout();
  const pluggableSaveViewControls = useSaveViewFormControls();
  const [shareDashboardOpen, setShareViewOpen] = useState(false);
  const [debugOpen, setDebugOpen] = useState(false);
  const [saveNewDashboardOpen, setSaveNewDashboardOpen] = useState(false);
  const [editDashboardOpen, setEditDashboardOpen] = useState(false);
  const [exportOpen, setExportOpen] = useState(false);
  const allowedToEdit = _isAllowedToEdit(view, currentUser);
  const debugOverlay = AppConfig.gl2DevMode() && (
    <>
      <MenuItem divider />
      <MenuItem onSelect={() => setDebugOpen(true)} icon="code">
        Debug
      </MenuItem>
    </>
  );
  const dispatch = useAppDispatch();
  const history = useHistory();
  const pluggableDashboardActions = usePluginEntities('views.components.dashboardActions');
  const modalRefs = useRef({});
  const dashboardActions = useMemo(() => pluggableDashboardActions.map(({ component: PluggableDashboardAction, key }) => (
    <PluggableDashboardAction key={`dashboard-action-${key}`} dashboard={view} modalRef={() => modalRefs.current[key]} />
  )), [pluggableDashboardActions, view]);
  const dashboardActionModals = useMemo(() => pluggableDashboardActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal key={`dashboard-action-modal-${key}`} dashboard={view} ref={(r) => { modalRefs.current[key] = r; }} />
    )), [pluggableDashboardActions, view]);

  const _onSaveNewDashboard = useCallback(async (newDashboard: View) => {
    const isViewDuplication = !!view.id;

    if (isViewDuplication) {
      const dashboardWithPluginData = await executePluggableDuplicationHandler(newDashboard, currentUser.permissions, pluggableSaveViewControls);

      return dispatch(onSaveNewDashboard(dashboardWithPluginData, history));
    }

    return dispatch(onSaveNewDashboard(newDashboard, history));
  }, [currentUser.permissions, dispatch, history, pluggableSaveViewControls, view.id]);
  const _onSaveView = useCallback(() => dispatch(OnSaveViewAction(view)), [dispatch, view]);
  const _onUpdateView = useCallback((updatedView: View) => dispatch(updateView(updatedView)), [dispatch]);

  useHotkey({
    actionKey: 'save',
    callback: () => _onSaveView(),
    scope: 'dashboard',
    telemetryAppPathname: 'search',
  });

  return (
    <ButtonGroup>
      {showSaveButton && (
        <SaveViewButton title="Save dashboard"
                        onClick={_onSaveView}
                        disabled={isNewView || hasUndeclaredParameters || !allowedToEdit} />
      )}
      {showSaveNewButton && (
        <Button onClick={() => setSaveNewDashboardOpen(true)}
                disabled={hasUndeclaredParameters}
                title="Save as new dashboard">
          <Icon name="copy" /> Save as
        </Button>
      )}
      {showShareButton && (
        <ShareButton entityType="dashboard"
                     entityId={view.id}
                     onClick={() => setShareViewOpen(true)}
                     bsStyle="default"
                     disabledInfo={isNewView && 'Only saved dashboards can be shared.'} />
      )}
      {showDropDownButton && (
        <DropdownButton title={<Icon name="ellipsis-h" title="More Actions" />} id="query-tab-actions-dropdown" pullRight noCaret>
          {dashboardActions.length > 0 && (
            <>
              {dashboardActions}
              <MenuItem divider />
            </>
          )}
          <MenuItem onSelect={() => setEditDashboardOpen(true)} disabled={isNewView || !allowedToEdit} icon="edit">
            Edit metadata
          </MenuItem>
          <MenuItem onSelect={() => setExportOpen(true)} icon="cloud-download-alt">Export</MenuItem>
          {debugOverlay}
          <MenuItem divider />
          <BigDisplayModeConfiguration view={view} disabled={isNewView} />
        </DropdownButton>
      )}
      {debugOpen && <DebugOverlay show onClose={() => setDebugOpen(false)} />}
      {saveNewDashboardOpen && (
        <DashboardPropertiesModal show
                                  view={view.toBuilder().newId().build()}
                                  title="Save new dashboard"
                                  submitButtonText="Create dashboard"
                                  onClose={() => setSaveNewDashboardOpen(false)}
                                  onSave={(newDashboard) => _onSaveNewDashboard(newDashboard)} />
      )}
      {editDashboardOpen && (
        <DashboardPropertiesModal show
                                  view={view}
                                  title="Editing dashboard"
                                  submitButtonText="Update dashboard"
                                  onClose={() => setEditDashboardOpen(false)}
                                  onSave={_onUpdateView} />
      )}

      {shareDashboardOpen && (
      <EntityShareModal entityId={view.id}
                        entityType="dashboard"
                        entityTitle={view.title}
                        description="Search for a User or Team to add as collaborator on this dashboard."
                        onClose={() => setShareViewOpen(false)} />
      )}
      {exportOpen && <ExportModal view={view} closeModal={() => setExportOpen(false)} />}
      {dashboardActionModals}
    </ButtonGroup>
  );
};

export default DashboardActionsMenu;
