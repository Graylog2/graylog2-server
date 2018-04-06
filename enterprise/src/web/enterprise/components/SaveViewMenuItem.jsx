import React from 'react';
import { MenuItem } from 'react-bootstrap';

import connect from 'stores/connect';
import { ViewActions } from 'enterprise/stores/ViewManagementStore';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import SelectedFieldsStore from 'enterprise/stores/SelectedFieldsStore';
import SearchStore from 'enterprise/stores/SearchStore';
import ViewsStore from 'enterprise/stores/ViewsStore';
import WidgetStore from 'enterprise/stores/WidgetStore';
import TitlesStore from 'enterprise/stores/TitlesStore';
import ViewPropertiesModal from 'enterprise/components/views/ViewPropertiesModal';

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
    const { currentView, search, selectedFields, titles, views, widgets } = this.props;
    ViewActions.save(undefined, currentView, views.get(currentView.selectedView), widgets, selectedFields, search, titles);
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
  titles: TitlesStore,
  views: ViewsStore,
  widgets: WidgetStore,
});