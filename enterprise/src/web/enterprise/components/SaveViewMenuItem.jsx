import React from 'react';
import { MenuItem } from 'react-bootstrap';

import connect from 'stores/connect';
import { ViewActions } from '../stores/ViewManagementStore';
import CurrentViewStore from '../stores/CurrentViewStore';
import SelectedFieldsStore from '../stores/SelectedFieldsStore';
import SearchStore from '../stores/SearchStore';
import ViewsStore from '../stores/ViewsStore';
import WidgetStore from '../stores/WidgetStore';

class SaveViewMenuItem extends React.Component {
  static propTypes = {};

  _onClick = () => {
    // eslint-disable-next-line react/prop-types
    const { currentView, search, selectedFields, views, widgets } = this.props;
    ViewActions.save(undefined, 'Sample View', currentView, views.get(currentView.selectedView), widgets, selectedFields, search);
  };

  render() {
    return <MenuItem onSelect={this._onClick}>Save View</MenuItem>;
  }
}

export default connect(SaveViewMenuItem, {
  currentView: CurrentViewStore,
  search: SearchStore,
  selectedFields: SelectedFieldsStore,
  views: ViewsStore,
  widgets: WidgetStore,
});