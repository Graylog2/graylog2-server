import PropTypes from 'prop-types';
import React from 'react';
import { Button, Row, Col, Well } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import numeral from 'numeral';
import EntityListItem from 'components/common/EntityListItem';
import ExtractorUtils from 'util/ExtractorUtils';
import ActionsProvider from 'injection/ActionsProvider';
import Routes from 'routing/Routes';
const ExtractorsActions = ActionsProvider.getActions('Extractors');

class ExtractorsListItem extends React.Component {
  static propTypes = {
    extractor: PropTypes.object.isRequired,
    inputId: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
  };

  state = {
    showDetails: false,
  };

  _toggleDetails = () => {
    this.setState({ showDetails: !this.state.showDetails });
  };

  _deleteExtractor = () => {
    if (window.confirm(`Really remove extractor "${this.props.extractor.title}?"`)) {
      ExtractorsActions.delete.triggerPromise(this.props.inputId, this.props.extractor);
    }
  };

  _formatExtractorSubtitle = () => {
    return (
      <span>
        Trying to extract data from <em>{this.props.extractor.source_field}</em> into{' '}
        <em>{this.props.extractor.target_field}</em>,{' '}
        {this.props.extractor.cursor_strategy === 'cut' && 'not'}{' '}
        leaving the original intact.
      </span>
    );
  };

  _formatCondition = () => {
    if (this.props.extractor.condition_type === 'none') {
      return <div />;
    }

    return (
      <div className="configuration-section">
        <h4>Condition</h4>
        <ul>
          <li>
            Will only attempt to run if the message{' '}
            {this.props.extractor.condition_type === 'string' ? 'includes the string' : 'matches the regular expression'}{' '}
            <em>{this.props.extractor.condition_value}</em>
          </li>
        </ul>
      </div>
    );
  };

  _formatActions = () => {
    const actions = [];

    actions.push(
      <Button key={`extractor-details-${this.props.extractor.id}`} bsStyle="info" onClick={this._toggleDetails}>
        Details
      </Button>,
    );
    actions.push(
      <LinkContainer key={`edit-extractor-${this.props.extractor.id}`}
                     to={Routes.edit_input_extractor(this.props.nodeId, this.props.inputId, this.props.extractor.id)}>
        <Button bsStyle="info">Edit</Button>
      </LinkContainer>,
    );
    actions.push(<Button key={'delete-extractor-'} bsStyle="danger" onClick={this._deleteExtractor}>Delete</Button>);

    return actions;
  };

  _formatOptions = (options) => {
    const attributes = Object.keys(options);
    return attributes.map((attribute) => {
      return <li key={`${attribute}-${this.props.extractor.id}`}>{attribute}: {options[attribute]}</li>;
    });
  };

  _formatConfiguration = (extractorConfig) => {
    let formattedOptions = this._formatOptions(extractorConfig);
    if (formattedOptions.length === 0) {
      formattedOptions = <li>No configuration options</li>;
    }

    return (
      <div className="configuration-section">
        <h4>Configuration</h4>
        <ul>
          {formattedOptions}
        </ul>
      </div>
    );
  };

  _formatConverter = (key, converter) => {
    return (
      <li key={`converter-${key}`}>
        {converter.type}
        {converter.config && <ul>{this._formatOptions(converter.config)}</ul>}
      </li>
    );
  };

  _formatConverters = (converters) => {
    const converterKeys = Object.keys(converters);
    const formattedConverters = converterKeys.map(converterKey => this._formatConverter(converterKey, converters[converterKey]));
    if (formattedConverters.length === 0) {
      return <div />;
    }

    return (
      <div className="configuration-section">
        <h4>Converters</h4>
        <ul>
          {formattedConverters}
        </ul>
      </div>
    );
  };

  _formatTimingMetrics = (timing) => {
    return (
      <dl className="metric-def metric-timer">
        <dt>95th percentile:</dt>
        <dd>{numeral(timing['95th_percentile']).format('0,0.[00]')}&#956;s</dd>

        <dt>98th percentile:</dt>
        <dd>{numeral(timing['98th_percentile']).format('0,0.[00]')}&#956;s</dd>

        <dt>99th percentile:</dt>
        <dd>{numeral(timing['99th_percentile']).format('0,0.[00]')}&#956;s</dd>

        <dt>Standard deviation:</dt>
        <dd>{numeral(timing.std_dev).format('0,0.[00]')}&#956;s</dd>

        <dt>Mean:</dt>
        <dd>{numeral(timing.mean).format('0,0.[00]')}&#956;s</dd>

        <dt>Minimum:</dt>
        <dd>{numeral(timing.min).format('0,0.[00]')}&#956;s</dd>

        <dt>Maximum:</dt>
        <dd>{numeral(timing.max).format('0,0.[00]')}&#956;s</dd>
      </dl>
    );
  };

  _formatMetrics = (metrics) => {
    let totalRate;
    if (metrics.total.rate) {
      totalRate = (
        <div className="meter" style={{ marginBottom: 10 }}>
          {numeral(metrics.total.rate.total).format('0,0')} total invocations since boot,{' '}
          averages:{' '}
          {numeral(metrics.total.rate.one_minute).format('0,0.[00]')},{' '}
          {numeral(metrics.total.rate.five_minute).format('0,0.[00]')},{' '}
          {numeral(metrics.total.rate.fifteen_minute).format('0,0.[00]')}.
        </div>
      );
    }

    const conditionCounts = (
      <div className="meter" style={{ marginBottom: 10 }}>
        {metrics.condition_hits} hits,{' '}
        {metrics.condition_misses} misses
      </div>
    );

    let totalTime;
    if (metrics.total.time) {
      totalTime = this._formatTimingMetrics(metrics.total.time);
    } else {
      totalTime = 'No message passed through here yet.';
    }

    let conditionTime;
    if (metrics.condition.time) {
      conditionTime = this._formatTimingMetrics(metrics.condition.time);
    } else {
      conditionTime = 'No message passed through here yet.';
    }

    let executionTime;
    if (metrics.execution.time) {
      executionTime = this._formatTimingMetrics(metrics.execution.time);
    } else {
      executionTime = 'No message passed through here yet.';
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
        {conditionCounts}
        <Row>
          <Col md={6}>
            <h4 style={{ display: 'inline' }}>Total time</h4><br />
            {totalTime}
          </Col>
          <Col md={6}>
            <h4 style={{ display: 'inline' }}>Condition time</h4><br />
            {conditionTime}
          </Col>
          <Col md={6}>
            <h4 style={{ display: 'inline' }}>Execution time</h4><br />
            {executionTime}
          </Col>
          <Col md={6}>
            <h4 style={{ display: 'inline' }}>Converter time</h4><br />
            {convertersTime}
          </Col>
        </Row>
      </div>
    );
  };

  _formatDetails = () => {
    return (
      <div>
        <Col md={8}>
          <Well bsSize="small" className="configuration-well">
            {this._formatCondition()}
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
  };

  render() {
    return (
      <EntityListItem key={`entry-list-${this.props.extractor.id}`}
                      title={this.props.extractor.title}
                      titleSuffix={ExtractorUtils.getReadableExtractorTypeName(this.props.extractor.type)}
                      description={this._formatExtractorSubtitle()}
                      actions={this._formatActions()}
                      contentRow={this.state.showDetails ? this._formatDetails() : null} />
    );
  }
}

export default ExtractorsListItem;
