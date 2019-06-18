import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { SearchStore } from '../../stores/SearchStore';
import { DashboardWidgetsStore } from '../../stores/DashboardWidgetsStore';
import { SearchConfigStore } from '../../stores/SearchConfigStore';
import DashboardContainer from './DashboardContainer';
import { ImmutableWidgetsMap } from '../widgets/WidgetPropTypes';

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

OverviewTab.propTypes = {
  configurations: PropTypes.shape({
    searchesClusterConfig: PropTypes.object.isRequired,
  }).isRequired,
  dashboardWidgets: ImmutableWidgetsMap.isRequired,
  searches: PropTypes.shape({
    widgetMapping: PropTypes.object.isRequired,
  }).isRequired,
};

export default connect(OverviewTab, { configurations: SearchConfigStore, dashboardWidgets: DashboardWidgetsStore, searches: SearchStore });
