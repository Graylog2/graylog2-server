import PropTypes from 'prop-types';
import React from 'react';
import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';

class AddSearchCountToDashboard extends React.Component {
  static propTypes = {
    searchInStream: PropTypes.object,
    permissions: PropTypes.array.isRequired,
    pullRight: PropTypes.bool,
  };

  SEARCH_COUNT_WIDGET_TYPE = 'SEARCH_RESULT_COUNT';
  STREAM_SEARCH_COUNT_WIDGET_TYPE = 'STREAM_SEARCH_RESULT_COUNT';

  render() {
    return (
      <AddToDashboardMenu title="Add count to dashboard"
                          pullRight={this.props.pullRight}
                          widgetType={this.props.searchInStream ? this.STREAM_SEARCH_COUNT_WIDGET_TYPE : this.SEARCH_COUNT_WIDGET_TYPE}
                          permissions={this.props.permissions} />
    );
  }
}

export default AddSearchCountToDashboard;
