import React from 'react';
import PropTypes from 'prop-types';
import { merge } from 'lodash';

import Plotly from 'enterprise/custom-plotly';
import createPlotlyComponent from 'react-plotly.js/factory';

const Plot = createPlotlyComponent(Plotly);

class GenericPlot extends React.Component {
  static propTypes = {
    chartData: PropTypes.array.isRequired,
    layout: PropTypes.object,
    onZoom: PropTypes.func,
  };

  static defaultProps = {
    layout: {},
    onZoom: () => {},
  };

  _onRelayout = (axis) => {
    if (!axis.autosize) {
      const { onZoom } = this.props;
      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      return onZoom(from, to);
    }
    return true;
  };

  render() {
    const { chartData, layout } = this.props;
    const plotLayout = merge({
      autosize: true,
      margin: {
        autoexpand: true,
        t: 10,
        l: 40,
        r: 10,
        b: 40,
        pad: 0,
      },
      legend: {
        orientation: 'h',
        y: -0.14,
      },
      hoverlabel: {
        namelength: -1,
      },
    }, layout);

    const style = { height: 'calc(100% - 10px)', width: '100%' };

    const config = { displayModeBar: false };

    return (<Plot data={chartData}
                  useResizeHandler
                  layout={plotLayout}
                  style={style}
                  onRelayout={this._onRelayout}
                  config={config} />
    );
  }
}

export default GenericPlot;
