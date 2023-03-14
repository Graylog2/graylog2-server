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
import React, { useState, useCallback } from 'react';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ShareButton } from 'components/common';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import AddEvidence from 'components/security/investigations/AddEvidence';
import type View from 'views/logic/views/View';
import EntityShareModal from 'components/permissions/EntityShareModal';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

// eslint-disable-next-line no-alert
const defaultDashboardDeletionHook = async (view: View) => window.confirm(`Are you sure you want to delete "${view.title}"?`);

type Props = {
  dashboard: View,
  refetchDashboards: () => void,
}

const DeleteItem = styled.span(({ theme }: { theme: DefaultTheme }) => css`
  color: ${theme.colors.variant.danger};
`);

const DashboardActions = ({ dashboard, refetchDashboards }: Props) => {
  const [showShareModal, setShowShareModal] = useState(false);
  const paginationQueryParameter = usePaginationQueryParameter();

  const onDashboardDelete = useCallback(async () => {
    const pluginDashboardDeletionHooks = PluginStore.exports('views.hooks.confirmDeletingDashboard');

    const result = await iterateConfirmationHooks([...pluginDashboardDeletionHooks, defaultDashboardDeletionHook], dashboard);

    if (result) {
      await ViewManagementActions.delete(dashboard);
      refetchDashboards();
      paginationQueryParameter.resetPage();
    }
  }, [dashboard, refetchDashboards, paginationQueryParameter]);

  return (
    <>
      <ShareButton bsSize="xsmall"
                   entityId={dashboard.id}
                   entityType="dashboard"
                   onClick={() => setShowShareModal(true)} />
      <DropdownButton bsSize="xsmall"
                      title="More"
                      id={`more-dropdown-${dashboard.id}`}
                      pullRight>
        <AddEvidence id={dashboard.id} type="dashboards">
          {({ investigationSelected }) => (
            <MenuItem disabled={!investigationSelected} icon="puzzle-piece">
              Add to investigation
            </MenuItem>
          )}
        </AddEvidence>
        <MenuItem divider />
        <MenuItem onClick={onDashboardDelete}>
          <DeleteItem role="button">Delete</DeleteItem>
        </MenuItem>
      </DropdownButton>
      {showShareModal && (
        <EntityShareModal entityId={dashboard.id}
                          entityType="dashboard"
                          description={`Search for a User or Team to add as collaborator on this ${ViewTypeLabel({ type: dashboard.type })}.`}
                          entityTitle={dashboard.title}
                          onClose={() => setShowShareModal(false)} />
      )}
    </>
  );
};

export default DashboardActions;
