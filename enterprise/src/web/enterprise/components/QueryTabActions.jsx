// @flow
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { DropdownButton, MenuItem } from 'react-bootstrap';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
import DebugOverlay from 'enterprise/components/DebugOverlay';
import { ViewStore } from 'enterprise/stores/ViewStore';
import { SearchMetadataStore } from 'enterprise/stores/SearchMetadataStore';
import SearchMetadata from 'enterprise/logic/search/SearchMetadata';
import ViewPropertiesModal from './views/ViewPropertiesModal';

const QueryTabActions = createReactClass({
  propTypes: {
    onSaveView: PropTypes.func.isRequired,
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

  handleSaveView(view) {
    this.props.onSaveView(view);
  },

  _isNewView(view) {
    return !view.title;
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
    return (
      <span>
        <DropdownButton title="View Actions" id="query-tab-actions-dropdown" bsStyle="info" pullRight>
          <MenuItem onSelect={this.handleEdit} disabled={isNewView}>Edit</MenuItem>
          <MenuItem onSelect={onSave} disabled={isNewView || hasUndeclaredParameters}>Save</MenuItem>
          <MenuItem onSelect={this.handleSaveAs} disabled={hasUndeclaredParameters}>Save as</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
        <ViewPropertiesModal view={view.toBuilder().newId().build()} title="Save new view" onSave={this.handleSaveView} show={this.state.saveAsViewOpen} onClose={this.handleSaveAsViewClose} />
        <ViewPropertiesModal view={view} title="Editing view" onSave={this.handleSaveView} show={this.state.editViewOpen} onClose={this.handleEditClose} />
      </span>
    );
  },
});

export default connect(QueryTabActions, { metadata: SearchMetadataStore, view: ViewStore });
