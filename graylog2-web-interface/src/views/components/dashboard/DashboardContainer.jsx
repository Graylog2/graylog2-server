import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
import Immutable from 'immutable';

import { Col } from 'react-bootstrap';

import { Spinner } from 'components/common';
import DashboardWidgetGrid from 'enterprise/components/dashboard/DashboardWidgetGrid';
import { widgetDefinition } from 'enterprise/logic/Widget';
import { DashboardWidgetsActions } from 'enterprise/stores/DashboardWidgetsStore';
import EmptyDashboardHelpMessage from './EmptyDashboardHelpMessage';
import * as WidgetPropTypes from '../widgets/WidgetPropTypes';

const DashboardContainer = createReactClass({
  propTypes: {
    dashboardWidgets: PropTypes.shape({
      dashboardWidgets: PropTypes.instanceOf(Immutable.Map).isRequired,
    }).isRequired,
    widgetDefs: WidgetPropTypes.WidgetsMap.isRequired,
    widgetMapping: PropTypes.object,
    positions: WidgetPropTypes.PositionsMap.isRequired,
    results: PropTypes.object,
  },

  getDefaultProps() {
    return {
      widgetMapping: {},
      results: {},
    };
  },

  getInitialState() {
    return {};
  },

  updatePositions(newPositions) {
    DashboardWidgetsActions.positions(newPositions);
  },

  handlePositionsChange(positions) {
    const newPositions = {};
    positions.forEach(({ col, height, row, width, id }) => {
      newPositions[id] = { col, height, row, width };
    });
    this.updatePositions(newPositions);
  },

  handleWidgetDelete(widgetId, positions) {
    DashboardWidgetsActions.removeFromDashboard(widgetId);

    const newPositions = Object.assign({}, positions);
    delete newPositions[widgetId];
    this.updatePositions(newPositions);
  },

  renderWidgetGrid(widgetDefs, dashboardWidgets, widgetMapping, queryResults, positions) {
    const widgets = {};
    const data = {};
    let fields = new Immutable.Map();

    if (!dashboardWidgets || dashboardWidgets.isEmpty()) {
      // No dashboard widgets defined
      return <EmptyDashboardHelpMessage />;
    }

    const widgetsWithResults = dashboardWidgets.map((value, widgetId) => {
      let m = new Immutable.Map();
      m = m.set('widget', widgetDefs.get(widgetId));
      m = m.set('result', new Immutable.Map(queryResults[value.queryId]));
      return m;
    });

    widgetsWithResults.valueSeq().forEach((widgetWithResult) => {
      const widgetDef = widgetWithResult.get('widget');
      const result = widgetWithResult.get('result');
      const searchTypes = result.get('searchTypes');

      const widgetType = widgetDefinition(widgetDef.type);
      const dataTransformer = widgetType.searchResultTransformer || (x => x);
      const widgetData = (widgetMapping[widgetDef.id] || []).map(searchTypeId => searchTypes[searchTypeId]);
      if (widgetData) {
        widgets[widgetDef.id] = widgetDef;
        data[widgetDef.id] = dataTransformer(widgetData, widgetDef.toJSON());
        if (widgetDef.type === 'messages' && widgetData.fields) {
          fields = new Immutable.Map(widgetData.fields);
        }
      }
    });
    return (
      <DashboardWidgetGrid fields={fields}
                           locked={false}
                           widgets={widgets}
                           positions={positions}
                           data={data}
                           onWidgetDelete={widget => this.handleWidgetDelete(widget, positions)}
                           onPositionsChange={this.handlePositionsChange} />
    );
  },

  render() {
    const { widgetMapping, results } = this.props;
    const { dashboardWidgets, widgetDefs, positions } = this.props.dashboardWidgets;

    if (!results.results) {
      return <Col md={12}><Spinner /></Col>;
    }

    const widgetGrid = this.renderWidgetGrid(widgetDefs, dashboardWidgets, widgetMapping.toJS(), results.results, positions);

    return (
      <Col md={12}>
        {widgetGrid}
      </Col>
    );
  },
});

export default DashboardContainer;
