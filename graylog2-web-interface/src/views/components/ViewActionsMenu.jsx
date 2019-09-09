// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { DropdownButton, MenuItem } from 'components/graylog';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
// $FlowFixMe: imports from core need to be fixed in flow
import StoreProvider from 'injection/StoreProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import PermissionsMixin from 'util/PermissionsMixin';
// $FlowFixMe: imports from core need to be fixed in flow
import AppConfig from 'util/AppConfig';

import DebugOverlay from 'views/components/DebugOverlay';
import { ViewStore } from 'views/stores/ViewStore';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import * as Permissions from 'views/Permissions';
import ViewPropertiesModal from './views/ViewPropertiesModal';
import ShareViewModal from './views/ShareViewModal';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const { isPermitted } = PermissionsMixin;

const ViewActionsMenu = createReactClass({
  propTypes: {
    currentUser: PropTypes.shape({
      currentUser: PropTypes.shape({
        permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
      }).isRequired,
    }).isRequired,
    onSaveView: PropTypes.func.isRequired,
    onSaveAsView: PropTypes.func.isRequired,
    metadata: PropTypes.shape({
      undeclared: ImmutablePropTypes.Set,
    }).isRequired,
    view: PropTypes.shape({
      view: PropTypes.object.isRequired,
    }).isRequired,
  },

  getInitialState() {
    return {
      debugOpen: false,
      editViewOpen: false,
      saveViewOpen: false,
      shareViewOpen: false,
    };
  },

  handleDebugOpen() {
    this.setState({ debugOpen: true });
  },

  handleDebugClose() {
    this.setState({ debugOpen: false });
  },

  handleEdit() {
    this.setState({ editViewOpen: true });
  },

  handleEditClose() {
    this.setState({ editViewOpen: false });
  },

  handleSaveAs() {
    this.setState({ saveAsViewOpen: true });
  },

  handleSaveAsViewClose() {
    this.setState({ saveAsViewOpen: false });
  },

  handleShareView() {
    this.setState({ shareViewOpen: true });
  },

  handleShareViewClose() {
    this.setState({ shareViewOpen: false });
  },

  handleSaveView(view) {
    this.props.onSaveView(view);
  },

  handleSaveAsView(view) {
    this.props.onSaveAsView(view);
  },

  _isNewView(view) {
    return !view.title;
  },

  _isAllowedToEdit(view) {
    const { currentUser } = this.props.currentUser;
    return isPermitted(currentUser.permissions, [Permissions.View.Edit(view.id)]);
  },

  _hasUndeclaredParameters(searchMetadata: SearchMetadata) {
    return searchMetadata.undeclared.size > 0;
  },

  render() {
    const { view } = this.props.view;
    const onSave = () => this.handleSaveView(view);
    const { metadata } = this.props;
    const hasUndeclaredParameters = this._hasUndeclaredParameters(metadata);
    const isNewView = this._isNewView(view);
    const allowedToEdit = this._isAllowedToEdit(view);
    const debugOverlay = AppConfig.gl2DevMode() && (
      <React.Fragment>
        <MenuItem divider />
        <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
      </React.Fragment>
    );
    return (
      <span>
        <DropdownButton title="View Actions" id="query-tab-actions-dropdown" bsStyle="info" pullRight>
          <MenuItem onSelect={this.handleEdit} disabled={isNewView || !allowedToEdit}>Edit</MenuItem>
          <MenuItem onSelect={onSave} disabled={isNewView || hasUndeclaredParameters || !allowedToEdit}>Save</MenuItem>
          <MenuItem onSelect={this.handleSaveAs} disabled={hasUndeclaredParameters}>Save as</MenuItem>
          <MenuItem onSelect={this.handleShareView} disabled={isNewView || !allowedToEdit}>Share</MenuItem>

          {debugOverlay}
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
        <ViewPropertiesModal view={view.toBuilder().newId().build()} title="Save new view" onSave={this.handleSaveAsView} show={this.state.saveAsViewOpen} onClose={this.handleSaveAsViewClose} />
        <ViewPropertiesModal view={view} title="Editing view" onSave={this.handleSaveView} show={this.state.editViewOpen} onClose={this.handleEditClose} />
        {this.state.shareViewOpen && <ShareViewModal view={view} show onClose={this.handleShareViewClose} />}
      </span>
    );
  },
});

export default connect(ViewActionsMenu, { metadata: SearchMetadataStore, view: ViewStore, currentUser: CurrentUserStore });
