import React, {PropTypes} from 'react';
import {Button, Row, Col, Well} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';

import EntityListItem from 'components/common/EntityListItem';
import ExtractorUtils from 'util/ExtractorUtils';
import Routes from 'routing/Routes';

const ExtractorsListItem = React.createClass({
  propTypes: {
    extractor: PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      showDetails: false,
    };
  },
  _toggleDetails() {
    this.setState({showDetails: !this.state.showDetails});
  },
  _deleteExtractor() {
    if (window.confirm(`Really remove extractor "${this.props.extractor.title}?"`)) {
      alert('Delete extractor');
    }
  },
  _formatExtractorSubtitle() {
    return (
      <span>
        Trying to extract data from <em>{this.props.extractor.source_field}</em> into{' '}
        <em>{this.props.extractor.target_field}</em>,{' '}
        {this.props.extractor.cursor_strategy === 'cut' && 'not'}{' '}
        leaving the original intact.
      </span>
    );
  },
  _formatActions() {
    const actions = [];

    actions.push(
      <Button key={`extractor-details-${this.props.extractor.id}`} bsStyle="info" onClick={this._toggleDetails}>
        Details
      </Button>
    );
    actions.push(
      <LinkContainer key={`edit-extractor-${this.props.extractor.id}`}
                     to={Routes.edit_input_extractor('', '', this.props.extractor.id)}>
        <Button bsStyle="info">Edit</Button>
      </LinkContainer>
    );
    actions.push(<Button key={`delete-extractor-`} bsStyle="danger" onClick={this._deleteExtractor}>Delete</Button>);

    return actions;
  },
  _formatOptions(options) {
    const attributes = Object.keys(options);
    return attributes.map(attribute => {
      return <li key={`${attribute}-${this.props.extractor.id}`}>{attribute}: {options[attribute]}</li>;
    });
  },
  _formatConfiguration(extractorConfig) {
    let formattedOptions = this._formatOptions(extractorConfig);
    if (formattedOptions.length === 0) {
      formattedOptions = <div>No configuration options</div>;
    }

    return (
      <div>
        <h4>Configuration</h4>
        <ul>
          {formattedOptions}
        </ul>
      </div>
    );
  },
  _formatConverter(key, converter) {
    return (
      <li key={`converter-${key}`}>
        {converter.type}
        {converter.config && <ul>{this._formatOptions(converter.config)}</ul>}
      </li>
    );
  },
  _formatConverters(converters) {
    const converterKeys = Object.keys(converters);
    const formattedConverters = converterKeys.map(converterKey => this._formatConverter(converterKey, converters[converterKey]));
    if (formattedConverters.length === 0) {
      return <div></div>;
    }

    return (
      <div style={{marginTop: 10}}>
        <h4>Converters</h4>
        <ul>
          {formattedConverters}
        </ul>
      </div>
    );
  },
  _formatTimingMetrics(timing) {
    return (
      <dl className="metric-def metric-timer">
        <dt>95th percentile:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing['95th_percentile']}</span>&#956;s</dd>

        <dt>98th percentile:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing['98th_percentile']}</span>&#956;s</dd>

        <dt>99th percentile:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing['99th_percentile']}</span>&#956;s</dd>

        <dt>Standard deviation:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing.std_dev}</span>&#956;s</dd>

        <dt>Mean:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing.mean}</span>&#956;s</dd>

        <dt>Minimum:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing.min}</span>&#956;s</dd>

        <dt>Maximum:</dt>
        <dd><span className="number-format" data-format="0,0.[00]">{timing.max}</span>&#956;s</dd>
      </dl>
    );
  },
  _formatMetrics(metrics) {
    let totalRate;
    if (metrics.total.rate) {
      totalRate = (
        <div className="meter" style={{marginBottom: 10}}>
          {metrics.total.rate.total} total invocations since boot,{' '}
          averages:{' '}
          {metrics.total.rate.one_minute}, {metrics.total.rate.five_minute}, {metrics.total.rate.fifteen_minute}.
        </div>
      );
    }

    let totalTime;
    if (metrics.total.time) {
      totalTime = this._formatTimingMetrics(metrics.total.time);
    } else {
      totalTime = 'No message passed through here yet.';
    }

    let convertersTime;
    if (metrics.converters.time) {
      convertersTime = this._formatTimingMetrics(metrics.converters.time);
    } else {
      convertersTime = 'No message passed through here yet.';
    }

    return (
      <div>
        {totalRate}
        <Row>
          <Col md={6}>
            <h3 style={{display: 'inline'}}>Total time</h3><br />
            {totalTime}
          </Col>

          <Col md={6}>
            <h3 style={{display: 'inline'}}>Converter time</h3><br />
            {convertersTime}
          </Col>
        </Row>
      </div>
    );
  },
  _formatDetails() {
    return (
      <div>
        <Col md={12}>
          {this.props.extractor.condition_type !== 'none' &&
            <span>
              <strong>Condition:</strong>{' '}
              Will only attempt to run if the message{' '}
              {this.props.extractor.condition_type === 'string' ? 'includes the string' : 'matches the regular expression'}{' '}
              <em>{this.props.extractor.condition_value}</em>.
            </span>
          }
        </Col>
        <Col md={8}>
          <Well bsSize="small" className="configuration-well">
            {this._formatConfiguration(this.props.extractor.extractor_config)}
            {this._formatConverters(this.props.extractor.converters)}
          </Well>
        </Col>
        <Col md={4}>
          <div className="graylog-input-metrics">
            <h3>Metrics</h3>
            {this._formatMetrics(this.props.extractor.metrics)}
          </div>
        </Col>
      </div>
    );
  },
  render() {
    // TODO:
    // - Delete extractors
    // - Edit extractors
    // - Format metrics

    return (
      <EntityListItem key={`entry-list-${this.props.extractor.id}`}
                      title={this.props.extractor.title}
                      titleSuffix={ExtractorUtils.getReadableExtractorTypeName(this.props.extractor.type)}
                      description={this._formatExtractorSubtitle()}
                      actions={this._formatActions()}
                      contentRow={this.state.showDetails ? this._formatDetails() : null} />
    );
  },
});

export default ExtractorsListItem;
