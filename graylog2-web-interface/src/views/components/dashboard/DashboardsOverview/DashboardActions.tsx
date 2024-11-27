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
import React, { useState, useCallback, useRef } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import UserNotification from 'util/UserNotification';
import { ShareButton } from 'components/common';
import { MenuItem, DeleteMenuItem } from 'components/bootstrap';
import type View from 'views/logic/views/View';
import EntityShareModal from 'components/permissions/EntityShareModal';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import usePluginEntities from 'hooks/usePluginEntities';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type FetchError from 'logic/errors/FetchError';
import { isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { MoreActions } from 'components/common/EntityDataTable';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';

// eslint-disable-next-line no-alert
const defaultDashboardDeletionHook = async (view: View) => window.confirm(`Are you sure you want to delete "${view.title}"?`);

const _extractErrorMessage = (error: FetchError) => ((error
  && error.additional
  && error.additional.body
  && error.additional.body.message) ? error.additional.body.message : error);

type Props = {
  dashboard: View,
  isEvidenceModal?: boolean,
}

const usePluggableDashboardActions = (dashboard: View) => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.dashboardActions');
  const availableActions = pluggableActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  );
  const actions = availableActions.map(({ component: PluggableDashboardAction, key }) => (
    <PluggableDashboardAction key={`dashboard-action-${key}`}
                              dashboard={dashboard}
                              modalRef={() => modalRefs.current[key]} />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal key={`dashboard-action-modal-${key}`}
                   dashboard={dashboard}
                   ref={(r) => { modalRefs.current[key] = r; }} />
    ));

  return ({ actions, actionModals });
};

const DashboardDeleteAction = ({ dashboard, refetchDashboards, isEvidenceModal = false }: { dashboard: View, refetchDashboards: () => void, isEvidenceModal?: boolean }) => {
  const { deselectEntity } = useSelectedEntities();
  const paginationQueryParameter = usePaginationQueryParameter();

  const onDashboardDelete = useCallback(async () => {
    const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

    const result = await iterateConfirmationHooks([...pluginDashboardDeletionHooks, defaultDashboardDeletionHook], dashboard);

    if (result) {
      ViewManagementActions.delete(dashboard).then(() => {
        UserNotification.success(`Deleting dashboard "${dashboard.title}" was successful!`, 'Success!');
        deselectEntity(dashboard.id);
        refetchDashboards();
        paginationQueryParameter.resetPage();
      }).catch((error) => {
        UserNotification.error(`Deleting dashboard failed: ${_extractErrorMessage(error)}`, 'Error!');
      });
    }
  }, [dashboard, deselectEntity, refetchDashboards, paginationQueryParameter]);

  return isEvidenceModal ? null : (
    <DeleteMenuItem onClick={onDashboardDelete} />
  );
};

const DashboardActions = ({ dashboard, isEvidenceModal = false }: Props) => {
  const [showShareModal, setShowShareModal] = useState(false);
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableDashboardActions(dashboard);
  const currentUser = useCurrentUser();
  const { refetch } = useTableFetchContext();

  const moreActions = [
    pluggableActions.length ? pluggableActions : null,
    pluggableActions.length && !isEvidenceModal ? <MenuItem divider key="divider" /> : null,
    isAnyPermitted(currentUser.permissions, [`view:edit:${dashboard.id}`, 'view:edit'])
      ? <DashboardDeleteAction dashboard={dashboard} refetchDashboards={refetch} key="delete-action" isEvidenceModal={isEvidenceModal} />
      : null,
  ].filter(Boolean);

  return (
    <>
      {isEvidenceModal || (
        <ShareButton bsSize="xsmall"
                     entityId={dashboard.id}
                     entityType="dashboard"
                     onClick={() => setShowShareModal(true)} />
      )}
      {(!!moreActions.length && isEvidenceModal)
        ? moreActions[0]
        : (
          <MoreActions>
            {moreActions}
          </MoreActions>
        )}
      {showShareModal && (
        <EntityShareModal entityId={dashboard.id}
                          entityType="dashboard"
                          description={`Search for a User or Team to add as collaborator on this ${ViewTypeLabel({ type: dashboard.type })}.`}
                          entityTitle={dashboard.title}
                          onClose={() => setShowShareModal(false)} />
      )}
      {pluggableActionModals}
    </>
  );
};

export default DashboardActions;
