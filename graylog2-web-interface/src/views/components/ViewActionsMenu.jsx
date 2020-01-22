// @flow strict
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { DropdownButton, MenuItem } from 'components/graylog';

import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import PermissionsMixin from 'util/PermissionsMixin';
import AppConfig from 'util/AppConfig';

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

const _isAllowedToEdit = (view: View, currentUser) => isPermitted(currentUser.permissions, [Permissions.View.Edit(view.id)]);

const _hasUndeclaredParameters = (searchMetadata: SearchMetadata) => searchMetadata.undeclared.size > 0;

const ViewActionsMenu = ({ view, isNewView, metadata, onSaveView, onSaveAsView, currentUser }) => {
  const [shareViewOpen, setShareViewOpen] = useState(false);
  const [debugOpen, setDebugOpen] = useState(false);
  const [saveAsViewOpen, setSaveAsViewOpen] = useState(false);
  const [editViewOpen, setEditViewOpen] = useState(false);

  const hasUndeclaredParameters = _hasUndeclaredParameters(metadata);
  const allowedToEdit = _isAllowedToEdit(view, currentUser);
  const debugOverlay = AppConfig.gl2DevMode() && (
    <React.Fragment>
      <MenuItem divider />
      <MenuItem onSelect={() => setDebugOpen(true)}>Debug</MenuItem>
    </React.Fragment>
  );
  return (
    <React.Fragment>
      <DropdownButton title="Actions" id="query-tab-actions-dropdown" bsStyle="info" pullRight>
        <MenuItem onSelect={() => setEditViewOpen(true)} disabled={isNewView || !allowedToEdit}>Edit</MenuItem>
        <MenuItem onSelect={() => onSaveView(view)}
                  disabled={isNewView || hasUndeclaredParameters || !allowedToEdit}>
          Save
        </MenuItem>
        <MenuItem onSelect={() => setSaveAsViewOpen(true)} disabled={hasUndeclaredParameters}>Save as</MenuItem>
        <MenuItem onSelect={() => setShareViewOpen(true)} disabled={isNewView || !allowedToEdit}>Share</MenuItem>

        {debugOverlay}

        <IfDashboard>
          <MenuItem divider />
          <BigDisplayModeConfiguration view={view} disabled={isNewView} />
        </IfDashboard>
      </DropdownButton>
      <DebugOverlay show={debugOpen} onClose={() => setDebugOpen(false)} />
      {saveAsViewOpen && (
        <ViewPropertiesModal view={view.toBuilder().newId().build()}
                             title="Save new dashboard"
                             onSave={onSaveAsView}
                             show
                             onClose={() => setSaveAsViewOpen(false)} />
      )}
      {editViewOpen && (
        <ViewPropertiesModal view={view}
                             title="Editing dashboard"
                             onSave={onSaveView}
                             show
                             onClose={() => setEditViewOpen(false)} />
      )}
      {shareViewOpen && <ShareViewModal view={view} show onClose={() => setShareViewOpen(false)} />}
    </React.Fragment>
  );
};

ViewActionsMenu.propTypes = {
  currentUser: PropTypes.shape({
    currentUser: PropTypes.shape({
      permissions: PropTypes.arrayOf(PropTypes.string),
    }),
  }),
  onSaveView: PropTypes.func.isRequired,
  onSaveAsView: PropTypes.func.isRequired,
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
  ViewActionsMenu,
  { metadata: SearchMetadataStore, view: ViewStore, currentUser: CurrentUserStore },
  ({ view: { view, isNew }, currentUser: { currentUser }, ...rest }) => ({ currentUser, view, isNewView: isNew, ...rest }),
);
