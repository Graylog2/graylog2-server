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
import { useTranslation } from 'react-i18next';
import { PluginStore } from 'graylog-web-plugin/plugin';

import i18n from 'i18n';
import UserNotification from 'util/UserNotification';
import { ShareButton } from 'components/common';
import { MenuItem, DeleteMenuItem } from 'components/bootstrap';
import type View from 'views/logic/views/View';
import EntityShareModal from 'components/permissions/EntityShareModal';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import usePluginEntities from 'hooks/usePluginEntities';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type FetchError from 'logic/errors/FetchError';
import usePermissions from 'hooks/usePermissions';
import useGetPermissionsByScope from 'hooks/useScopePermissions';
import { MoreActions } from 'components/common/EntityDataTable';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import { deleteView } from 'views/api/views';

const defaultDashboardDeletionHook = async (view: View) =>
  // eslint-disable-next-line no-alert
  window.confirm(i18n.t('delete.confirm', { ns: 'dashboards', title: view.title }));

const _extractErrorMessage = (error: FetchError) =>
  error && error.additional && error.additional.body && error.additional.body.message
    ? error.additional.body.message
    : error;

type Props = {
  dashboard: View;
  hideDelete?: boolean;
  hideShare?: boolean;
};

const usePluggableDashboardActions = (dashboard: View) => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.dashboardActions');

  const availableActions = pluggableActions.filter((action) => (action.useCondition ? !!action.useCondition() : true));
  const actions = availableActions.map(({ component: PluggableDashboardAction, key }) => (
    <PluggableDashboardAction
      key={`dashboard-action-${key}`}
      dashboard={dashboard}
      modalRef={() => modalRefs.current[key]}
    />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal
        key={`dashboard-action-modal-${key}`}
        dashboard={dashboard}
        ref={(r) => {
          modalRefs.current[key] = r;
        }}
      />
    ));

  return { actions, actionModals };
};

const DashboardDeleteAction = ({
  dashboard,
  refetchDashboards,
  hideDelete = false,
}: {
  dashboard: View;
  refetchDashboards: () => void;
  hideDelete?: boolean;
}) => {
  const { t } = useTranslation('dashboards');
  const { deselectEntity } = useSelectedEntities();
  const paginationQueryParameter = usePaginationQueryParameter();

  const onDashboardDelete = useCallback(async () => {
    const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

    const result = await iterateConfirmationHooks(
      [...pluginDashboardDeletionHooks, defaultDashboardDeletionHook],
      dashboard,
    );

    if (result) {
      deleteView(dashboard)
        .then(() => {
          UserNotification.success(t('delete.success', { title: dashboard.title }), t('delete.success_title'));
          deselectEntity(dashboard.id);
          refetchDashboards();
          paginationQueryParameter.resetPage();
        })
        .catch((error) => {
          UserNotification.error(t('delete.failed', { error: _extractErrorMessage(error) }), t('delete.failed_title'));
        });
    }
  }, [dashboard, deselectEntity, refetchDashboards, paginationQueryParameter, t]);

  return hideDelete ? null : <DeleteMenuItem onClick={onDashboardDelete} />;
};

const DashboardActions = ({ dashboard, hideDelete = false, hideShare = false }: Props) => {
  const { t } = useTranslation('dashboards');
  const [showShareModal, setShowShareModal] = useState(false);
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableDashboardActions(dashboard);
  const { isAnyPermitted } = usePermissions();
  const { refetch } = useTableFetchContext();
  const { scopePermissions } = useGetPermissionsByScope({ _scope: dashboard.scope });
  const hideDeleteAction = hideDelete || !scopePermissions?.is_mutable;

  const moreActions = [
    pluggableActions.length ? pluggableActions : null,
    pluggableActions.length && !hideDeleteAction ? <MenuItem divider key="divider" /> : null,
    scopePermissions?.is_mutable && isAnyPermitted([`view:edit:${dashboard.id}`, 'view:edit']) ? (
      <DashboardDeleteAction
        dashboard={dashboard}
        refetchDashboards={refetch}
        key="delete-action"
        hideDelete={hideDelete}
      />
    ) : null,
  ].filter(Boolean);

  return (
    <>
      {hideShare || (
        <ShareButton
          bsSize="xsmall"
          entityId={dashboard.id}
          entityType="dashboard"
          onClick={() => setShowShareModal(true)}
        />
      )}
      <MoreActions>{moreActions}</MoreActions>

      {showShareModal && (
        <EntityShareModal
          entityId={dashboard.id}
          entityType="dashboard"
          description={t('search:dashboard_actions.share_description')}
          entityTitle={dashboard.title}
          onClose={() => setShowShareModal(false)}
        />
      )}
      {pluggableActionModals}
    </>
  );
};

export default DashboardActions;
