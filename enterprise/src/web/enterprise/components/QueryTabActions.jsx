import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import connect from 'stores/connect';
import DebugOverlay from 'enterprise/components/DebugOverlay';
import { ViewStore } from 'enterprise/stores/ViewStore';
import ViewPropertiesModal from './views/ViewPropertiesModal';

const QueryTabActions = createReactClass({
  propTypes: {
    onSaveView: PropTypes.func.isRequired,
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

  render() {
    const { view } = this.props.view;
    const onSave = () => this.handleSaveView(view);
    return (
      <span>
        <DropdownButton title="View Actions" id="query-tab-actions-dropdown" bsStyle="info">
          <MenuItem onSelect={this.handleEdit} disabled={this._isNewView(view)}>Edit</MenuItem>
          <MenuItem onSelect={onSave} disabled={this._isNewView(view)}>Save</MenuItem>
          <MenuItem onSelect={this.handleSaveAs}>Save as</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
        <ViewPropertiesModal view={view} title="Save new view" onSave={this.handleSaveView} show={this.state.saveAsViewOpen} onClose={this.handleSaveAsViewClose} />
        <ViewPropertiesModal view={view} title="Editing view" onSave={this.handleSaveView} show={this.state.editViewOpen} onClose={this.handleEditClose} />
      </span>
    );
  },
});

export default connect(QueryTabActions, { view: ViewStore });
