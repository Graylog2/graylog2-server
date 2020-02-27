// @flow strict
import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { withRouter } from 'react-router';

import { DropdownButton, MenuItem, Button, ButtonGroup } from 'components/graylog';
import { Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import PermissionsMixin from 'util/PermissionsMixin';
import AppConfig from 'util/AppConfig';

import onSaveView from 'views/logic/views/OnSaveViewAction';
import onSaveAsView from 'views/logic/views/OnSaveAsViewAction';
import DebugOverlay from 'views/components/DebugOverlay';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import * as Permissions from 'views/Permissions';
import View from 'views/logic/views/View';
import ViewPropertiesModal from './views/ViewPropertiesModal';
import ShareViewModal from './views/ShareViewModal';
import IfDashboard from './dashboard/IfDashboard';
import BigDisplayModeConfiguration from './dashboard/BigDisplayModeConfiguration';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const { isPermitted } = PermissionsMixin;

const _isAllowedToEdit = (view: View, currentUser) => isPermitted(currentUser.permissions, [Permissions.View.Edit(view.id)])
  || (view.type === View.Type.Dashboard && isPermitted(currentUser.permissions, [`dashboards:edit:${view.id}`]));

const _hasUndeclaredParameters = (searchMetadata: SearchMetadata) => searchMetadata.undeclared.size > 0;

const ViewActionsMenu = ({ view, isNewView, metadata, currentUser, router }) => {
  const [shareViewOpen, setShareViewOpen] = useState(false);
  const [debugViewOpen, setDebugViewOpen] = useState(false);
  const [saveNewOpen, setSaveAsOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const hasUndeclaredParameters = _hasUndeclaredParameters(metadata);
  const allowedToEdit = _isAllowedToEdit(view, currentUser);
  const debugOverlay = AppConfig.gl2DevMode() && (
    <React.Fragment>
      <MenuItem divider />
      <MenuItem onSelect={() => setDebugViewOpen(true)}>
        <Icon name="code" /> Debug
      </MenuItem>
    </React.Fragment>
  );
  return (
    <ButtonGroup>
      <Button onClick={() => onSaveView(view)}
              disabled={isNewView || hasUndeclaredParameters || !allowedToEdit}
              data-testid="dashboard-save-button">
        <Icon name="save" /> Save
      </Button>
      <Button onClick={() => setSaveAsOpen(true)}
              disabled={hasUndeclaredParameters}
              data-testid="dashboard-save-as-button">
        <Icon name="copy" /> Save as
      </Button>
      <DropdownButton title={<Icon name="ellipsis-h" />} id="query-tab-actions-dropdown" pullRight noCaret>
        <MenuItem onSelect={() => setEditOpen(true)} disabled={isNewView || !allowedToEdit}>
          <Icon name="edit" /> Edit
        </MenuItem>
        <MenuItem onSelect={() => setShareViewOpen(true)} disabled={isNewView || !allowedToEdit}>
          <Icon name="share-alt" /> Share
        </MenuItem>
        {debugOverlay}
        <IfDashboard>
          <MenuItem divider />
          <BigDisplayModeConfiguration view={view} disabled={isNewView} />
        </IfDashboard>
      </DropdownButton>
      {debugViewOpen && <DebugOverlay show onClose={() => setDebugViewOpen(false)} />}
      {saveNewOpen && (
        <ViewPropertiesModal show
                             view={view.toBuilder().newId().build()}
                             title="Save new dashboard"
                             onClose={() => setSaveAsOpen(false)}
                             onSave={newView => onSaveAsView(newView, router)} />
      )}
      {editOpen && (
        <ViewPropertiesModal show
                             view={view}
                             title="Editing dashboard"
                             onClose={() => setEditOpen(false)}
                             onSave={updatedView => onSaveView(updatedView, router)} />
      )}
      {shareViewOpen && <ShareViewModal show view={view} onClose={() => setShareViewOpen(false)} />}
    </ButtonGroup>
  );
};

ViewActionsMenu.propTypes = {
  currentUser: PropTypes.shape({
    currentUser: PropTypes.shape({
      permissions: PropTypes.arrayOf(PropTypes.string),
    }),
  }),
  router: PropTypes.any.isRequired,
  metadata: PropTypes.shape({
    undeclared: ImmutablePropTypes.Set,
  }).isRequired,
  view: PropTypes.instanceOf(View).isRequired,
  isNewView: PropTypes.bool.isRequired,
};

ViewActionsMenu.defaultProps = {
  currentUser: {
    currentUser: {
      permissions: [],
    },
  },
};

export default connect(
  withRouter(ViewActionsMenu),
  { metadata: SearchMetadataStore, view: ViewStore, currentUser: CurrentUserStore },
  ({ view: { view, isNew }, currentUser: { currentUser }, ...rest }) => ({ currentUser, view, isNewView: isNew, ...rest }),
);
