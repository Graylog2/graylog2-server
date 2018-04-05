import React from 'react';
import { MenuItem } from 'react-bootstrap';

import connect from 'stores/connect';
import { ViewActions } from '../stores/ViewManagementStore';
import CurrentViewStore from '../stores/CurrentViewStore';
import SelectedFieldsStore from '../stores/SelectedFieldsStore';
import SearchStore from '../stores/SearchStore';
import ViewsStore from '../stores/ViewsStore';
import WidgetStore from '../stores/WidgetStore';
import ViewPropertiesModal from './views/ViewPropertiesModal';

class SaveViewMenuItem extends React.Component {
  static propTypes = {};

  constructor(props) {
    super(props);
    this.state = {
      open: false,
    };
  }

  _onClick = () => {
    this.setState({ open: true });
  };

  _onSave = () => {
    // eslint-disable-next-line react/prop-types
    const { currentView, search, selectedFields, views, widgets } = this.props;
    ViewActions.save(undefined, currentView, views.get(currentView.selectedView), widgets, selectedFields, search);
  };

  render() {
    const { currentView } = this.props;
    const { open } = this.state;
    return (
      <span>
        <MenuItem onSelect={this._onClick}>Save View</MenuItem>
        <ViewPropertiesModal title="Saving view" view={currentView} show={open} />
      </span>
    );
  }
}

export default connect(SaveViewMenuItem, {
  currentView: CurrentViewStore,
  search: SearchStore,
  selectedFields: SelectedFieldsStore,
  views: ViewsStore,
  widgets: WidgetStore,
});