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
import React, { useState, useContext } from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import connect from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import AppConfig from 'util/AppConfig';
import { DropdownButton, MenuItem, Button, ButtonGroup } from 'components/graylog';
import { Icon, ShareButton } from 'components/common';
import CSVExportModal from 'views/components/export/CSVExportModal';
import DebugOverlay from 'views/components/DebugOverlay';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import onSaveAsView from 'views/logic/views/OnSaveAsViewAction';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import * as Permissions from 'views/Permissions';
import View from 'views/logic/views/View';
import type { UserJSON } from 'logic/users/User';
import CurrentUserContext from 'contexts/CurrentUserContext';
import EntityShareModal from 'components/permissions/EntityShareModal';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

import ViewPropertiesModal from './views/ViewPropertiesModal';
import IfDashboard from './dashboard/IfDashboard';
import BigDisplayModeConfiguration from './dashboard/BigDisplayModeConfiguration';

const _isAllowedToEdit = (view: View, currentUser: UserJSON | undefined | null) => isPermitted(currentUser?.permissions, [Permissions.View.Edit(view.id)])
  || (view.type === View.Type.Dashboard && isPermitted(currentUser?.permissions, [`dashboards:edit:${view.id}`]));

const _hasUndeclaredParameters = (searchMetadata: SearchMetadata) => searchMetadata.undeclared.size > 0;

const ViewActionsMenu = ({ view, isNewView, metadata }) => {
  const currentUser = useContext(CurrentUserContext);
  const [shareViewOpen, setShareViewOpen] = useState(false);
  const [debugOpen, setDebugOpen] = useState(false);
  const [saveAsViewOpen, setSaveAsViewOpen] = useState(false);
  const [editViewOpen, setEditViewOpen] = useState(false);
  const [csvExportOpen, setCsvExportOpen] = useState(false);
  const hasUndeclaredParameters = _hasUndeclaredParameters(metadata);
  const allowedToEdit = _isAllowedToEdit(view, currentUser);
  const viewTypeLabel = ViewTypeLabel({ type: view.type });
  const debugOverlay = AppConfig.gl2DevMode() && (
    <>
      <MenuItem divider />
      <MenuItem onSelect={() => setDebugOpen(true)}>
        <Icon name="code" /> Debug
      </MenuItem>
    </>
  );

  return (
    <ButtonGroup>
      <Button onClick={() => onSaveView(view)}
              disabled={isNewView || hasUndeclaredParameters || !allowedToEdit}
              data-testid="dashboard-save-button">
        <Icon name="save" /> Save
      </Button>
      <Button onClick={() => setSaveAsViewOpen(true)}
              disabled={hasUndeclaredParameters}
              data-testid="dashboard-save-as-button">
        <Icon name="copy" /> Save as
      </Button>
      <ShareButton entityType="dashboard"
                   entityId={view.id}
                   onClick={() => setShareViewOpen(true)}
                   bsStyle="default"
                   disabledInfo={isNewView && 'Only saved dashboards can be shared.'} />
      <DropdownButton title={<Icon name="ellipsis-h" />} id="query-tab-actions-dropdown" pullRight noCaret>
        <MenuItem onSelect={() => setEditViewOpen(true)} disabled={isNewView || !allowedToEdit}>
          <Icon name="edit" /> Edit metadata
        </MenuItem>
        <MenuItem onSelect={() => setCsvExportOpen(true)}><Icon name="cloud-download-alt" /> Export to CSV</MenuItem>
        {debugOverlay}
        <IfDashboard>
          <MenuItem divider />
          <BigDisplayModeConfiguration view={view} disabled={isNewView} />
        </IfDashboard>
      </DropdownButton>
      {debugOpen && <DebugOverlay show onClose={() => setDebugOpen(false)} />}
      {saveAsViewOpen && (
        <ViewPropertiesModal show
                             view={view.toBuilder().newId().build()}
                             title="Save new dashboard"
                             onClose={() => setSaveAsViewOpen(false)}
                             onSave={(newView) => onSaveAsView(newView)} />
      )}
      {editViewOpen && (
        <ViewPropertiesModal show
                             view={view}
                             title="Editing dashboard"
                             onClose={() => setEditViewOpen(false)}
                             onSave={onSaveView} />
      )}

      {shareViewOpen && (
        <EntityShareModal entityId={view.id}
                          entityType="dashboard"
                          entityTitle={view.title}
                          description={`Search for a User or Team to add as collaborator on this ${viewTypeLabel}.`}
                          onClose={() => setShareViewOpen(false)} />
      )}
      {csvExportOpen && <CSVExportModal view={view} closeModal={() => setCsvExportOpen(false)} />}
    </ButtonGroup>
  );
};

ViewActionsMenu.propTypes = {
  metadata: PropTypes.shape({
    undeclared: ImmutablePropTypes.setOf(PropTypes.string),
  }).isRequired,
  view: PropTypes.object.isRequired,
  isNewView: PropTypes.bool.isRequired,
};

export default connect(
  ViewActionsMenu,
  { metadata: SearchMetadataStore, view: ViewStore },
  ({ view: { view, isNew }, ...rest }) => ({ view, isNewView: isNew, ...rest }),
);
