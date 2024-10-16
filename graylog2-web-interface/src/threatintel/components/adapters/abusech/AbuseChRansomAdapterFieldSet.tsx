import React from 'react';

import ObjectUtils from 'util/ObjectUtils';
import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';

type AbuseChRansomAdapterFieldSetProps = {
  config: any;
  updateConfig: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  handleFormEvent: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  validationState: (...args: any[]) => void;
  // eslint-disable-next-line react/no-unused-prop-types
  validationMessage: (...args: any[]) => void;
};

class AbuseChRansomAdapterFieldSet extends React.Component<AbuseChRansomAdapterFieldSetProps, {
  [key: string]: any;
}> {
  _update = (value, unit, enabled, name) => {
    const config = ObjectUtils.clone(this.props.config);
    config[name] = enabled ? value : 0;
    config[`${name}_unit`] = unit;
    this.props.updateConfig(config);
  };

  updateRefreshInterval = (value, unit, enabled) => {
    this._update(value, unit, enabled, 'refresh_interval');
  };

  _onBlocklistTypeSelect = (id) => {
    const config = ObjectUtils.clone(this.props.config);
    config.blocklist_type = id;
    this.props.updateConfig(config);
  };

  render() {
    const { config } = this.props;
    const blocklistTypes = [
      { label: 'Domain blocklist', value: 'DOMAINS' },
      { label: 'URL blocklist', value: 'URLS' },
      { label: 'IP blocklist', value: 'IPS' },
    ];

    return (
      <fieldset>
        <Input label="Blocklist type"
               id="blocklist-type-selector"
               required
               autoFocus
               help="Select the type of the abuse.ch ransomware blocklist"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select the type of blocklist"
                  clearable={false}
                  options={blocklistTypes}
                  matchProp="label"
                  onChange={this._onBlocklistTypeSelect}
                  value={config.blocklist_type} />
        </Input>
        <TimeUnitInput label="Refresh blocklist"
                       help="If enabled, the abuse.ch ransomware blocklist is refreshed when it changed."
                       update={this.updateRefreshInterval}
                       value={config.refresh_interval}
                       unit={config.refresh_interval_unit || 'MINUTES'}
                       defaultEnabled={config.refresh_interval > 0}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9" />
      </fieldset>
    );
  }
}

export default AbuseChRansomAdapterFieldSet;
