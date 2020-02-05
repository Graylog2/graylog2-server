import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import styled from 'styled-components';


import AddToDashboardMenu from 'components/dashboard/AddToDashboardMenu';
import { Icon } from 'components/common';

import StoreProvider from 'injection/StoreProvider';

import EventHandlersThrottler from 'util/EventHandlersThrottler';

import resultHistogram from 'legacy/result-histogram';

const SearchStore = StoreProvider.getStore('Search');

const ResultGraphContainer = styled.div`
  margin-right: 12px;
  margin-bottom: 25px;
`;

// Hue-manatee. We tried to be sorry, but aren't.
const LegacyHistogram = createReactClass({
  displayName: 'LegacyHistogram',

  propTypes: {
    formattedHistogram: PropTypes.array.isRequired,
    histogram: PropTypes.object.isRequired,
    stream: PropTypes.object,
    permissions: PropTypes.arrayOf(PropTypes.string).isRequired,
  },

  getInitialState() {
    return {};
  },

  componentDidMount() {
    this._renderHistogram(this.props.formattedHistogram);
    window.addEventListener('resize', this._onResize);
  },

  componentDidUpdate(prevProps) {
    if (JSON.stringify(this.props.formattedHistogram) !== JSON.stringify(prevProps.formattedHistogram)) {
      this._updateHistogram(this.props.formattedHistogram, prevProps.formattedHistogram);
    }
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._onResize);
  },

  WIDGET_TYPE: 'SEARCH_RESULT_CHART',
  RESOLUTIONS: ['year', 'quarter', 'month', 'week', 'day', 'hour', 'minute'],
  eventThrottler: new EventHandlersThrottler(),

  _onResize() {
    this.eventThrottler.throttle(() => resultHistogram.redrawResultGraph());
  },

  _renderHistogram(histogram) {
    resultHistogram.resetContainerElements(ReactDOM.findDOMNode(this));
    resultHistogram.setData(histogram, this.props.stream);
    resultHistogram.drawResultGraph();
  },

  _updateHistogram(histogram) {
    resultHistogram.updateData(histogram);
  },

  _resolutionChanged(newResolution) {
    return (event) => {
      event.preventDefault();
      SearchStore.resolution = newResolution;
    };
  },

  _getFirstHistogramValue() {
    if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
      return null;
    }

    return this.props.histogram.histogram_boundaries.from;
  },

  render() {
    if (SearchStore.resolution === undefined) {
      SearchStore.resolution = this.props.histogram.interval;
    }
    const resolutionLinks = this.RESOLUTIONS.map((resolution) => {
      let className = 'date-histogram-res-selector';
      if (this.props.histogram.interval === resolution) {
        className += ' selected-resolution';
      }
      const suffix = resolution === this.RESOLUTIONS[this.RESOLUTIONS.length - 1] ? '' : ',';
      return (
        <li key={resolution}>
          <a href="#"
             className={className}
             data-resolution={resolution}
             onClick={this._resolutionChanged(resolution)}>
            {resolution}
          </a>
          {suffix}
        </li>
      );
    });

    const resolutionSelector = (
      <ul className="graph-resolution-selector list-inline">
        <li><Icon name="clock-o" /></li>
        {resolutionLinks}
      </ul>
    );

    return (
      <div className="content-col">
        <div className="pull-right">
          <AddToDashboardMenu title="Add to dashboard"
                              widgetType={this.WIDGET_TYPE}
                              configuration={{ interval: this.props.histogram.interval }}
                              pullRight
                              permissions={this.props.permissions}
                              isStreamSearch={this.props.stream !== null} />
        </div>
        <h1>Histogram</h1>

        {resolutionSelector}

        <ResultGraphContainer>
          <div id="y_axis" />
          <div id="result-graph"
               data-from={this._getFirstHistogramValue()}
               data-to={this.props.histogram.histogram_boundaries.to} />
          <div id="result-graph-timeline" />
        </ResultGraphContainer>

      </div>
    );
  },
});

export default LegacyHistogram;
