import React from 'react';
import { Button } from 'react-bootstrap';

import connect from 'stores/connect';
import { ViewActions } from '../stores/ViewManagementStore';
import CurrentViewStore from '../stores/CurrentViewStore';
import SelectedFieldsStore from '../stores/SelectedFieldsStore';
import SearchStore from '../stores/SearchStore';
import ViewsStore from '../stores/ViewsStore';
import WidgetStore from '../stores/WidgetStore';

class SaveViewButton extends React.Component {
  static propTypes = {};

  _onClick = () => {
    // eslint-disable-next-line react/prop-types
    const { currentView, search, selectedFields, views, widgets } = this.props;
    ViewActions.save(undefined, 'Sample View', currentView, views.get(currentView.selectedView), widgets, selectedFields, search);
  };

  render() {
    return <Button onClick={this._onClick}>Save View</Button>;
  }
}

export default connect(SaveViewButton, {
  currentView: CurrentViewStore,
  search: SearchStore,
  selectedFields: SelectedFieldsStore,
  views: ViewsStore,
  widgets: WidgetStore,
});