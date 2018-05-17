import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { SearchStore } from '../../stores/SearchStore';
import { DashboardWidgetsStore } from '../../stores/DashboardWidgetsStore';
import { SearchConfigStore } from '../../stores/SearchConfigStore';

class OverviewTab extends React.Component {
  static propTypes = {};
  render() {
    const { configurations, dashboardWidgets, searches } = this.props;
    const results = searches && searches.result;
    const widgetMapping = searches && searches.widgetMapping;
    const searchConfig = configurations.searchesClusterConfig;
    return (
      <DashboardContainer results={results}
                          widgetMapping={widgetMapping}
                          dashboardWidgets={dashboardWidgets}
                          searchConfig={searchConfig} />
    );
  }
};

export default connect(OverviewTab, { configurations: SearchConfigStore, dashboardWidgets: DashboardWidgetsStore, searches: SearchStore });
