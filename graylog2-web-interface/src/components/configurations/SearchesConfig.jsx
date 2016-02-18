import React from 'react';
import { Input, Button } from 'react-bootstrap';
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
    return {
      query_time_range_limit: this._getConfigValue('query_time_range_limit'),
    };
  },

  _getConfigValue(field) {
    return this.props.config ? this.props.config[field] : null;
  },

  _isValidPeriod(duration) {
    const check = duration || this.state.query_time_range_limit;
    return moment.duration(check).asMilliseconds() > 0;
  },

  _validationState() {
    if (this._isValidPeriod()) {
      return undefined;
    } else {
      return 'error';
    }
  },

  _formatDuration() {
    return this._isValidPeriod() ? moment.duration(this.state.query_time_range_limit).humanize() : 'invalid';
  },

  _onPeriodUpdate(field) {
    return () => {
      const update = {};
      let period = this.refs[field].getValue().toUpperCase();

      if (!period.startsWith('P')) {
        period = `P${period}`;
      }

      update[field] = period;

      this.setState(update);
    };
  },

  _saveConfig() {
    this.props.updateConfig(this.state).then(() => {
      this._closeModal();
    });
  },

  _openModal() {
    this.refs.searchesConfigModal.open();
  },

  _closeModal() {
    this.refs.searchesConfigModal.close();
  },

  render() {
    const config = this.props.config;
    const duration = moment.duration(config.query_time_range_limit);

    return (
      <div>
        <h2>Search Configuration</h2>

        <dl className={style.deflist}>
          <dt>Query time range limit</dt>
          <dd>{config.query_time_range_limit} ({duration.format()})</dd>
          <dd>The maximum time users can query data in the past. This prevents users from accidentally creating queries which
            span a lot of data and would need a long time and many resources to complete (if at all).</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref="searchesConfigModal"
                            title="Update Search Configuration"
                            onSubmitForm={this._saveConfig}
                            submitButtonText="Save">
          <fieldset>
            <Input type="text"
                   ref="query_time_range_limit"
                   label="Query time range limit (ISO8601 Duration)"
                   onChange={this._onPeriodUpdate('query_time_range_limit')}
                   value={this.state.query_time_range_limit}
                   help={'The maximum time range for searches. (i.e. "P30D" for 30 days, "PT24H" for 24 hours)'}
                   addonAfter={this._formatDuration()}
                   bsStyle={this._validationState()}
                   autofocus
                   required />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default SearchesConfig;
