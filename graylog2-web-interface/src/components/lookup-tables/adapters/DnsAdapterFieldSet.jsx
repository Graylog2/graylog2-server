import PropTypes from 'prop-types';
import React from 'react';
import ObjectUtils from 'util/ObjectUtils';

import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';


class DnsAdapterFieldSet extends React.Component {
  static propTypes = {
    config: PropTypes.shape({
      request_timeout: PropTypes.number.isRequired,
      server_ips: PropTypes.string,
    }).isRequired,
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
    validationMessage: PropTypes.func.isRequired,
    validationState: PropTypes.func.isRequired,
  };

  _onLookupTypeSelect = (id) => {
    const config = ObjectUtils.clone(this.props.config);
    config.lookup_type = id;
    this.props.updateConfig(config);
  };

  updateCacheTTLOverride = (value, unit, enabled) => {
    this._updateCacheTTLOverride(value, unit, enabled, 'cache_ttl_override');
  };

  _updateCacheTTLOverride = (value, unit, enabled, fieldPrefix) => {
    const config = ObjectUtils.clone(this.props.config);

    // If Cache TTL Override box is checked, then save the value. If not, then do not save it.
    if (enabled && value) {
      config[fieldPrefix] = enabled && value ? value : null;
      config[`${fieldPrefix}_enabled`] = enabled;
    } else {
      config[fieldPrefix] = null;
      config[`${fieldPrefix}_enabled`] = false;
    }

    config[`${fieldPrefix}_unit`] = enabled ? unit : null;
    this.props.updateConfig(config);
  };

  render() {
    const { config } = this.props;
    const lookupTypes = [
      { label: 'Resolve hostname to IPv4 address (A)', value: 'A' },
      { label: 'Resolve hostname to IPv6 address (AAAA)', value: 'AAAA' },
      { label: 'Resolve hostname to IPv4 and IPv6 addresses (A and AAAA)', value: 'A_AAAA' },
      { label: 'Reverse lookup (PTR)', value: 'PTR' },
      { label: 'Text lookup (TXT)', value: 'TXT' },
    ];

    return (
      <fieldset>
        <Input label="DNS Lookup Type"
               id="lookup-type"
               required
               autoFocus
               help="Select the type of DNS lookup to perform."
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select the type of DNS lookup"
                  clearable={false}
                  options={lookupTypes}
                  matchProp="label"
                  onChange={this._onLookupTypeSelect}
                  value={config.lookup_type} />
        </Input>
        <Input type="text"
               id="server_ips"
               name="server_ips"
               label="DNS Server IP Address"
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('server_ips', 'An optional comma-separated list of DNS server IP addresses.')}
               bsStyle={this.props.validationState('server_ips')}
               value={config.server_ips}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="number"
               id="request_timeout"
               name="request_timeout"
               label="DNS Request Timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('request_timeout', 'DNS request timeout in milliseconds.')}
               bsStyle={this.props.validationState('request_timeout')}
               value={config.request_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <TimeUnitInput label="Cache TTL Override"
                       help="If enabled, the TTL for this adapter&amp;s cache will be overridden with the specified value."
                       update={this.updateCacheTTLOverride}
                       value={config.cache_ttl_override}
                       unit={config.cache_ttl_override_unit || 'MINUTES'}
                       enabled={config.cache_ttl_override_enabled}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9" />
      </fieldset>
    );
  }
}

export default DnsAdapterFieldSet;
