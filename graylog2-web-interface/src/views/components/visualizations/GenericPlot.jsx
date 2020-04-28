// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { withTheme } from 'styled-components';
import { merge } from 'lodash';
import { Overlay, RootCloseWrapper } from 'react-overlays';
import { type ThemeInterface } from 'theme';

import { Popover } from 'components/graylog';
import ColorPicker from 'components/common/ColorPicker';
import Plot from 'views/components/visualizations/plotly/AsyncPlot';

import ChartColorContext from './ChartColorContext';
// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style/useable!css!./GenericPlot.css';
import InteractiveContext from '../contexts/InteractiveContext';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

type LegendConfig = {
  name: string,
  target: HTMLElement,
  color?: string,
};

type ChartMarker = {
  colors?: Array<string>,
  color?: string,
};

export type ChartConfig = {
  name: string,
  labels: Array<string>,
  line?: ChartMarker,
  marker?: ChartMarker,
};

export type ColorMap = {
  [string]: string,
};

export type ChartColor = {
  line?: ChartMarker,
  marker?: ChartMarker,
};

type Props = {
  chartData: Array<*>,
  getChartColor?: (Array<ChartConfig>, string) => ?string,
  layout: {},
  onZoom: (string, string) => boolean,
  setChartColor?: (ChartConfig, ColorMap) => ChartColor,
  theme: ThemeInterface,
};

type State = {
  legendConfig?: LegendConfig,
};

type Axis = {
  autosize: boolean,
  [string]: string,
};

const nonInteractiveLayout = {
  yaxis: { fixedrange: true },
  xaxis: { fixedrange: true },
  hovermode: false,
};

class GenericPlot extends React.Component<Props, State> {
  static propTypes = {
    chartData: PropTypes.array.isRequired,
    layout: PropTypes.object,
    onZoom: PropTypes.func,
    getChartColor: PropTypes.func,
    setChartColor: PropTypes.func,
  };

  static defaultProps = {
    layout: {},
    onZoom: () => true,
    getChartColor: undefined,
    setChartColor: undefined,
  };

  state = {};

  componentDidMount() {
    styles.use();
  }

  componentWillUnmount() {
    styles.unuse();
  }

  _onRelayout = (axis: Axis) => {
    if (!axis.autosize && axis['xaxis.range[0]'] && axis['xaxis.range[1]']) {
      const { onZoom } = this.props;
      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      return onZoom(from, to);
    }
    return true;
  };

  _onLegendClick = (e: any) => {
    const name = e.node.textContent;
    const target = e.node.querySelector('g.layers');
    const { getChartColor } = this.props;
    if (getChartColor) {
      const color = getChartColor(e.fullData, name);
      /* $FlowFixMe color is already declared as optional */
      this.setState({ legendConfig: { name, target, color } });
    }
    return false;
  };

  _onColorSelect = (setColor: (string, string) => Promise<*>, name: string, newColor: string) => setColor(name, newColor)
    .then(this._onCloseColorPopup);

  // eslint-disable-next-line no-unused-vars
  _onCloseColorPopup = () => this.setState({ legendConfig: undefined });

  render() {
    const { chartData, layout, setChartColor, theme } = this.props;
    const defaultLayout = {
      autosize: true,
      showlegend: true,
      margin: {
        t: 10,
        l: 40,
        r: 10,
        b: 0,
        pad: 0,
      },
      legend: {
        orientation: 'h',
      },
      hoverlabel: {
        namelength: -1,
      },
      yaxis: {
        automargin: true,
        tickfont: {
          color: theme.color.gray[50],
        },
      },
      xaxis: {
        automargin: true,
        tickfont: {
          color: theme.color.gray[50],
        },
      },
    };
    const plotLayout = merge({}, defaultLayout, layout);

    const style = { height: '100%', width: '100%' };

    const config = { displayModeBar: false, doubleClick: false, responsive: true };

    const { legendConfig } = this.state;

    return (
      <ChartColorContext.Consumer>
        {({ colors, setColor }) => {
          const newChartData = chartData.map((chart) => {
            if (setChartColor && colors) {
              const conf = setChartColor(chart, colors);
              if (conf.line || conf.marker) {
                return merge(chart, conf);
              }
              return chart;
            }

            return chart;
          });
          return (
            <InteractiveContext.Consumer>
              {(interactive) => (
                <RenderCompletionCallback.Consumer>
                  {(onRenderComplete) => (
                    <>
                      <Plot data={newChartData}
                            useResizeHandler
                            layout={interactive ? plotLayout : merge({}, nonInteractiveLayout, plotLayout)}
                            style={style}
                            onAfterPlot={onRenderComplete}
                            onClick={interactive ? null : () => false}
                            onLegendClick={interactive ? this._onLegendClick : () => false}
                            onRelayout={interactive ? this._onRelayout : () => false}
                            config={config} />
                      {legendConfig && (
                        <RootCloseWrapper event="mousedown"
                                          onRootClose={this._onCloseColorPopup}>
                          <Overlay show
                                   placement="top"
                                   target={legendConfig.target}>
                            <Popover id="legend-config"
                                     title={`Configuration for ${legendConfig.name}`}
                                     className={styles.locals.customPopover}>
                              <ColorPicker color={legendConfig.color}
                                           onChange={(newColor) => this._onColorSelect(setColor, legendConfig.name, newColor)} />
                            </Popover>
                          </Overlay>
                        </RootCloseWrapper>
                      )}
                    </>
                  )}
                </RenderCompletionCallback.Consumer>
              )}
            </InteractiveContext.Consumer>
          );
        }}
      </ChartColorContext.Consumer>
    );
  }
}

export default withTheme(GenericPlot);
