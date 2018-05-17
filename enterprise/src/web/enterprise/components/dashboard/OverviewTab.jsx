import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { SearchStore } from '../../stores/SearchStore';
import { DashboardWidgetsStore } from '../../stores/DashboardWidgetsStore';
import { SearchConfigStore } from '../../stores/SearchConfigStore';
import DashboardContainer from './DashboardContainer';

const OverviewTab = ({ configurations, dashboardWidgets, searches }) => {
  const results = searches && searches.result;
  const widgetMapping = searches && searches.widgetMapping;
  const searchConfig = configurations.searchesClusterConfig;
  return (
    <DashboardContainer results={results}
                        widgetMapping={widgetMapping}
                        dashboardWidgets={dashboardWidgets}
                        searchConfig={searchConfig} />
  );
};

export default connect(OverviewTab, { configurations: SearchConfigStore, dashboardWidgets: DashboardWidgetsStore, searches: SearchStore });
