import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment-timezone';
import { get } from 'lodash';

import connect from 'stores/connect';
import Plotly from 'enterprise/custom-plotly';
import CombinedProvider from 'injection/CombinedProvider';
import createPlotlyComponent from 'react-plotly.js/factory';
import { CurrentQueryStore } from 'enterprise/stores/CurrentQueryStore';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import SearchActions from 'enterprise/actions/SearchActions';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const Plot = createPlotlyComponent(Plotly);

class GenericPlot extends React.Component {
  static propTypes = {
    chartData: PropTypes.array.isRequired,
    currentUser: PropTypes.shape({
      timezone: PropTypes.string.isRequired,
    }).isRequired,
    currentQuery: PropTypes.string.isRequired,
  };

  _onRelayout = (axis) => {
    if (!axis.autosize && get(this.figure, 'layout.xaxis.type') === 'date') {
      const { timezone } = this.props.currentUser;
      const { currentQuery } = this.props;

      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      const newTimerange = {
        type: 'absolute',
        from: moment.tz(from, timezone).toISOString(),
        to: moment.tz(to, timezone).toISOString(),
      };

      QueriesActions.timerange(currentQuery.id, newTimerange).then(SearchActions.executeWithCurrentState);
    }
  };

  _onUpdate = (figure) => {
    this.figure = figure;
  };

  render() {
    const { chartData } = this.props;
    const layout = {
      autosize: true,
      margin: {
        autoexpand: true,
        t: 10,
        l: 40,
        r: 10,
        b: 40,
        pad: 0,
      },
      yaxis: {
        fixedrange: true,
      },
      legend: {
        orientation: 'h',
        y: -0.14,
      },
      hoverlabel: {
        namelength: -1,
      },
    };

    const style = { height: 'calc(100% - 10px)', width: '100%' };

    const config = { displayModeBar: false };

    return (<Plot data={chartData}
                  useResizeHandler
                  layout={layout}
                  style={style}
                  onUpdate={this._onUpdate}
                  onRelayout={this._onRelayout}
                  config={config} />
    );
  }
}

export default connect(GenericPlot, {
  currentQuery: CurrentQueryStore,
  currentUser: CurrentUserStore,
});
