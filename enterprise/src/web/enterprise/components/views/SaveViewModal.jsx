import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import ViewsStore from 'enterprise/stores/ViewsStore';
import WidgetStore from 'enterprise/stores/WidgetStore';
import SearchStore from 'enterprise/stores/SearchStore';
import SelectedFieldsStore from 'enterprise/stores/SelectedFieldsStore';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import { ViewActions } from 'enterprise/stores/ViewManagementStore';
import ViewPropertiesModal from 'enterprise/components/views/ViewPropertiesModal';
import TitlesStore from 'enterprise/stores/TitlesStore';

const SaveViewModal = ({ currentView, search, selectedFields, titles, views, widgets, show, onClose, onSaveFinished }) => {
  const onSave = view => ViewActions.save(undefined, currentView, view, widgets, selectedFields, search, titles)
    .then(onSaveFinished);
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
  titles: TitlesStore,
  views: ViewsStore,
  widgets: WidgetStore,
});
