import React from 'react';
import { Input, Button } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { IfPermitted, Select } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

import style from '!style!css!components/configurations/ConfigurationStyles.css';

const GeoIpResolverConfig = React.createClass({
  propTypes: {
    config: React.PropTypes.object.isRequired,
    updateConfig: React.PropTypes.func.isRequired,
  },

  getInitialState() {
    return this._getStateFromProps(this.props);
  },

  componentWillReceiveProps(newProps) {
    this.setState(this._getStateFromProps(newProps));
  },

  _getStateFromProps(props) {
    return {
      config: {
        enabled: this._getPropConfigValue(props, 'enabled', false),
        db_type: this._getPropConfigValue(props, 'db_type', 'GEOLITE2_CITY'),
        db_path: this._getPropConfigValue(props, 'db_path', '/tmp/GeoLite2-City.mmdb'),
        run_before_extractors: this._getPropConfigValue(props, 'run_before_extractors', false),
      },
    };
  },

  _getPropConfigValue(props, field, defaultValue = null) {
    return props.config ? props.config[field] || defaultValue : defaultValue;
  },

  _updateConfigField(field, value) {
    const update = ObjectUtils.clone(this.state.config);
    update[field] = value;
    this.setState({config: update});
  },

  _onCheckboxClick(field, ref) {
    return () => {
      this._updateConfigField(field, this.refs[ref].getChecked());
    };
  },

  _onSelect(field) {
    return (selection) => {
      this._updateConfigField(field, selection);
    };
  },

  _onUpdate(field) {
    return (e) => {
      this._updateConfigField(field, e.target.value);
    };
  },

  _openModal() {
    this.refs.geoIpConfigModal.open();
  },

  _closeModal() {
    this.refs.geoIpConfigModal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    this.props.updateConfig(this.state.config).then(() => {
      this._closeModal();
    });
  },

  _availableDatabaseTypes() {
    return [
      {value: 'GEOLITE2_CITY', label: 'GeoLite2 City'},
      {value: 'GEOLITE2_COUNTRY', label: 'GeoLite2 Country'},
    ];
  },

  _activeDatabaseType(type) {
    return this._availableDatabaseTypes().filter((t) => t.value === type)[0].label;
  },

  render() {
    return (
      <div>
        <h3>GeoIP Filter</h3>

        <dl className={style.deflist}>
          <dt>Enabled:</dt>
          <dd>{this.state.config.enabled === true ? 'yes' : 'no'}</dd>
          <dt>DB type:</dt>
          <dd>{this._activeDatabaseType(this.state.config.db_type)}</dd>
          <dt>DB path:</dt>
          <dd>{this.state.config.db_path}</dd>
          <dt>Run before extractors:</dt>
          <dd>{this.state.config.run_before_extractors === true ? 'yes' : 'no'}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref="geoIpConfigModal"
                            title="Update GeoIP Filter Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <Input type="checkbox"
                   ref="configEnabled"
                   label="Enable GeoIP filter"
                   name="enabled"
                   checked={this.state.config.enabled}
                   onChange={this._onCheckboxClick('enabled', 'configEnabled')}/>
            <div className="form-group">
              <label className="control-label">
                Select the MaxMind GeoLite2 database type
              </label>
            <Select placeholder="Select MaxMind GeoLite2 database type"
                    options={this._availableDatabaseTypes()}
                    matchProp="value"
                    value={this.state.config.db_type}
                    onValueChange={this._onDbTypeSelect}/>
            </div>
            <Input type="text"
                   label="Path to the MaxMind GeoLite2 database"
                   name="db_path"
                   value={this.state.config.db_path}
                   onChange={this._onUpdate('db_path')}/>
            <Input type="checkbox"
                   ref="configRunBeforeExtractors"
                   label="Run GeoIP filter before running extractors"
                   help="If this is enabled, the GeoIP extractor will run before any extractors have been executed. WARNING: Server restart required to activate change!"
                   name="run_before_extractors"
                   checked={this.state.config.run_before_extractors}
                   onChange={this._onCheckboxClick('run_before_extractors', 'configRunBeforeExtractors')}/>
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default GeoIpResolverConfig;
