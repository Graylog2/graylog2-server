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
import React, { useState } from 'react';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/bootstrap';
import { ShareButton, IfPermitted } from 'components/common';
import type View from 'views/logic/views/View';
import EntityShareModal from 'components/permissions/EntityShareModal';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

type Props = {
  dashboard: View,
  onDashboardDelete: (dashboard: View) => void,
}

const DashboardActions = ({ dashboard, onDashboardDelete }: Props) => {
  const [showShareModal, setShowShareModal] = useState(false);

  return (
    <>
      <ButtonToolbar>
        <ShareButton bsSize="xsmall"
                     entityId={dashboard.id}
                     entityType="dashboard"
                     onClick={() => setShowShareModal(true)} />
        <DropdownButton bsSize="xsmall"
                        title="More Actions"
                        data-testid={`dashboard-actions-dropdown-${dashboard.id}`}
                        id={`dashboard-actions-dropdown-${dashboard.id}`}
                        pullRight>
          <IfPermitted permissions={[`view:edit:${dashboard.id}`, 'view:edit']} anyPermissions>
            <MenuItem onSelect={() => onDashboardDelete(dashboard)}>Delete</MenuItem>
          </IfPermitted>
        </DropdownButton>
      </ButtonToolbar>
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
