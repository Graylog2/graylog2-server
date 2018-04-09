import React from 'react';
import Reflux from 'reflux';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import CurrentViewStore from 'enterprise/stores/CurrentViewStore';

import DebugOverlay from 'enterprise/components/DebugOverlay';
import ViewsStore from 'enterprise/stores/ViewsStore';
import ViewPropertiesModal from './views/ViewPropertiesModal';

const QueryTabActions = createReactClass({
  propTypes: {
    onSaveView: PropTypes.func.isRequired,
    onToggleDashboard: PropTypes.func.isRequired,
  },
  mixins: [
    Reflux.connect(CurrentViewStore, 'currentView'),
    Reflux.connect(ViewsStore, 'views'),
  ],

  getInitialState() {
    return {
      debugOpen: false,
      saveViewOpen: false,
    };
  },

  handleDashboardClick() {
    this.props.onToggleDashboard();
  },

  handleDebugOpen() {
    this.setState({ debugOpen: true });
  },

  handleDebugClose() {
    this.setState({ debugOpen: false });
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
    return !view.has('title');
  },

  render() {
    const { views, currentView } = this.state;
    const view = views.get(currentView.selectedView);
    const onSave = () => this.handleSaveView(view);
    return (
      <span>
        <DropdownButton title="View Actions">
          <MenuItem onSelect={this.handleDashboardClick}>Dashboard</MenuItem>
          <MenuItem onSelect={onSave} disabled={this._isNewView(view)}>Save</MenuItem>
          <MenuItem onSelect={this.handleSaveAs}>Save as</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this.handleDebugOpen}>Debug</MenuItem>
        </DropdownButton>
        <DebugOverlay show={this.state.debugOpen} onClose={this.handleDebugClose} />
        <ViewPropertiesModal view={view} title="Save new view" onSave={this.handleSaveView} show={this.state.saveAsViewOpen} onClose={this.handleSaveAsViewClose} />
      </span>
    );
  },
});

export default QueryTabActions;
