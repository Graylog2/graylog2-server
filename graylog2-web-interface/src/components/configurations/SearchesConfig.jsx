import React from 'react';
import { Input, Button, Row, Col } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { IfPermitted } from 'components/common';

import moment from 'moment';
import {} from 'moment-duration-format';

import style from '!style!css!components/configurations/ConfigurationStyles.css';

const SearchesConfig = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    const queryTimeRangeLimit = this._getPropConfigValue('query_time_range_limit');
    const relativeTimerangeOptions = this._getPropConfigValue('relative_timerange_options');

    return {
      config: {
        query_time_range_limit: queryTimeRangeLimit,
        relative_timerange_options: relativeTimerangeOptions,
      },
      timerangeOptionsList: this._createTimerangeOptionsState(relativeTimerangeOptions),
      limitEnabled: moment.duration(queryTimeRangeLimit).asMilliseconds() > 0,
    };
  },

  _getPropConfigValue(field) {
    return this.props.config ? this.props.config[field] : null;
  },

  // Converts the object to a list of objects to make form handling easier.
  _createTimerangeOptionsState(options) {
    let optionsList = null;

    if (options) {
      optionsList = Object.keys(options).map((key) => {
        return {period: key, description: options[key]};
      });
    }

    return optionsList;
  },

  _updateTimerangeOptionsState(options) {
    const config = this.state.config;
    const newTimerangeOptions = {};

    options.forEach((option) => {
      newTimerangeOptions[option.period] = option.description;
    });

    config.relative_timerange_options = newTimerangeOptions;

    this.setState({config: config, timerangeOptionsList: options});
  },

  _isValidPeriod(duration) {
    const check = duration || this.state.config.query_time_range_limit;
    return moment.duration(check).asMilliseconds() >= 0;
  },

  _validationState() {
    if (this._isValidPeriod()) {
      return undefined;
    } else {
      return 'error';
    }
  },

  _formatDuration(duration) {
    return this._isValidPeriod() ? moment.duration(duration).humanize() : 'invalid';
  },

  _onPeriodUpdate(field) {
    return () => {
      const update = this.state.config;
      let period = this.refs[field].getValue().toUpperCase();

      if (!period.startsWith('P')) {
        period = `P${period}`;
      }

      update[field] = period;

      this.setState({config: update});
    };
  },

  _onChecked() {
    const config = this.state.config;

    if (this.state.limitEnabled) {
      // If currently enabled, disable by setting the limit to 0 seconds.
      config.query_time_range_limit = 'PT0S';
    } else {
      // If currently not enabled, set a default of 30 days.
      config.query_time_range_limit = 'P30D';
    }

    this.setState({config: config, limitEnabled: !this.state.limitEnabled});
  },

  _onTimerangeOptionRemove(removedIdx) {
    return () => {
      const options = this.state.timerangeOptionsList;

      // Remove element at index
      options.splice(removedIdx, 1);

      this._updateTimerangeOptionsState(options);
    };
  },

  _onTimerangeOptionAdd() {
    const options = this.state.timerangeOptionsList;

    if (options) {
      options.push({period: 'PT1S', description: ''});
      this._updateTimerangeOptionsState(options);
    }
  },

  _onTimerangeOptionChange(changedIdx, field) {
    return (e) => {
      const options = this.state.timerangeOptionsList;

      options.forEach((o, idx) => {
        if (idx === changedIdx) {
          let value = e.target.value;

          if (field === 'period') {
            value = value.toUpperCase();
            if (!value.startsWith('P')) {
              value = `P${value}`;
            }
          }

          options[idx][field] = value;
        }
      });

      this._updateTimerangeOptionsState(options);
    };
  },

  _isEnabled() {
    return this.state.limitEnabled;
  },

  _saveConfig() {
    this.props.updateConfig(this.state.config).then(() => {
      this._closeModal();
    });
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _openModal() {
    this.refs.searchesConfigModal.open();
  },

  _closeModal() {
    this.refs.searchesConfigModal.close();
  },

  render() {
    const config = this.state.config;
    const duration = moment.duration(config.query_time_range_limit);
    const limit = this._isEnabled() ? `${config.query_time_range_limit} (${duration.format()})` : 'disabled';

    let timerangeOptions = null;
    let timerangeOptionsSummary = null;
    if (this.state.timerangeOptionsList) {
      timerangeOptions = this.state.timerangeOptionsList.map((option, idx) => {
        const period = option.period;
        const description = option.description;

        return (
          <div key={'timerange-option-' + idx}>
            <Row>
              <Col xs={4}>
                <div className="input-group">
                  <input type="text"
                         className="form-control"
                         value={period}
                         onChange={this._onTimerangeOptionChange(idx, 'period')} />
                  <span className="input-group-addon">
                    {moment.duration(period).format()}
                  </span>
                </div>
              </Col>
              <Col xs={8}>
                <div className="input-group">
                  <input type="text"
                         className="form-control"
                         placeholder="Add description..."
                         value={description}
                         onChange={this._onTimerangeOptionChange(idx, 'description')} />
                  <span className="input-group-addon">
                    <i className="fa fa-trash" style={{cursor: 'pointer'}} onClick={this._onTimerangeOptionRemove(idx)} />
                  </span>
                </div>
              </Col>
            </Row>
          </div>
        );
      });
      timerangeOptionsSummary = this.state.timerangeOptionsList.map((option, idx) => {
        const period = option.period;
        const description = option.description;

        return (
          <span key={'timerange-options-summary-' + idx}>
            <dt>{period}</dt>
            <dd>{description}</dd>
          </span>
        );
      });
    }

    return (
      <div>
        <h2>Search Configuration</h2>

        <dl className={style.deflist}>
          <dt>Query time range limit</dt>
          <dd>{limit}</dd>
          <dd>The maximum time users can query data in the past. This prevents users from accidentally creating queries which
            span a lot of data and would need a long time and many resources to complete (if at all).</dd>
        </dl>

        <strong>Relative time range options</strong>

        <dl className={style.deflist}>
          {timerangeOptionsSummary}
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref="searchesConfigModal"
                            title="Update Search Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <Input type="checkbox" label="Enable query limit"
                   name="enabled"
                   checked={this._isEnabled()}
                   onChange={this._onChecked}/>
            {this._isEnabled() &&
            <Input type="text"
                   ref="query_time_range_limit"
                   label="Query time range limit (ISO8601 Duration)"
                   onChange={this._onPeriodUpdate('query_time_range_limit')}
                   value={config.query_time_range_limit}
                   help={'The maximum time range for searches. (i.e. "P30D" for 30 days, "PT24H" for 24 hours)'}
                   addonAfter={this._formatDuration(this.state.config.query_time_range_limit)}
                   bsStyle={this._validationState()}
                   required/>
            }

            <div className="form-group">
              <label className="control-label">Relative Timerange Options</label>
              <span className="help-block">
                Configure the available options for the <strong>relative</strong> time range selector as <strong>ISO8601 duration</strong>.
              </span>
              <div className="wrapper">
                {timerangeOptions}
              </div>
              <Button bsSize="xs" onClick={this._onTimerangeOptionAdd}>Add option</Button>
            </div>
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default SearchesConfig;
