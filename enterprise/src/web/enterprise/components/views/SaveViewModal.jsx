import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import ViewsStore from 'enterprise/stores/ViewsStore';
import WidgetStore from 'enterprise/stores/WidgetStore';
import SearchStore from 'enterprise/stores/SearchStore';
import SelectedFieldsStore from 'enterprise/stores/SelectedFieldsStore';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import { ViewActions } from '../../stores/ViewManagementStore';
import ViewPropertiesModal from './ViewPropertiesModal';

const SaveViewModal = ({ currentView, search, selectedFields, views, widgets, show, onClose, onSaveFinished }) => {
  const onSave = view => ViewActions.save(undefined, currentView, view, widgets, selectedFields, search).then(onSaveFinished);
  const view = views.get(currentView.selectedView);
  return (
    <ViewPropertiesModal view={view} title="Save new view" onSave={onSave} show={show} onClose={onClose} />
  );
};

SaveViewModal.propTypes = {
  onClose: PropTypes.func.isRequired,
  onSaveFinished: PropTypes.func.isRequired,
  show: PropTypes.bool,
};

SaveViewModal.defaultProps = {
  show: false,
};

export default connect(SaveViewModal, {
  currentView: CurrentViewStore,
  search: SearchStore,
  selectedFields: SelectedFieldsStore,
  views: ViewsStore,
  widgets: WidgetStore,
});
