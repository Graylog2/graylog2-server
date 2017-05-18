import React, { PropTypes } from 'react';
import ObjectUtils from 'util/ObjectUtils';

import { Input } from 'components/bootstrap';
import { TimeUnitInput } from 'components/common';

const GuavaCacheFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
// eslint-disable-next-line react/no-unused-prop-types
    validationState: PropTypes.func.isRequired,
// eslint-disable-next-line react/no-unused-prop-types
    validationMessage: PropTypes.func.isRequired,
  },

  _update(value, unit, enabled, name) {
    const config = ObjectUtils.clone(this.props.config);
    config[name] = enabled ? value : 0;
    config[`${name}_unit`] = unit;
    this.props.updateConfig(config);
  },

  updateAfterAccess(value, unit, enabled) {
    this._update(value, unit, enabled, 'expire_after_access');
  },

  updateAfterWrite(value, unit, enabled) {
    this._update(value, unit, enabled, 'expire_after_write');
  },

  updateRefresh(value, unit, enabled) {
    this._update(value, unit, enabled, 'refresh_after_write');
  },

  render() {
    const config = this.props.config;

    return (<fieldset>
      <Input type="text"
             id="max_size"
             name="max_size"
             label="Maximum entries"
             autoFocus
             required
             onChange={this.props.handleFormEvent}
             help="The limit of the number of entries the cache keeps in memory."
             value={config.max_size}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <TimeUnitInput label="Expire after access"
                     help="If enabled, entries are removed from the cache after the specified time from when they were last used."
                     update={this.updateAfterAccess}
                     value={config.expire_after_access}
                     unit={config.expire_after_access_unit || 'SECONDS'}
                     enabled={config.expire_after_access > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
      <TimeUnitInput label="Expire after write"
                     help="If enabled, entries are removed from the cache after the specified time from when they were first used."
                     update={this.updateAfterWrite}
                     value={config.expire_after_write}
                     unit={config.expire_after_write_unit || 'SECONDS'}
                     enabled={config.expire_after_write > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
      <TimeUnitInput label="Refresh after write"
                     help="If enabled, entries are refreshed from the data adapter after the specified time from when they were first used."
                     update={this.updateRefresh}
                     value={config.refresh_after_write}
                     unit={config.refresh_after_write_unit || 'SECONDS'}
                     enabled={config.refresh_after_write > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
    </fieldset>);
  },
});

export default GuavaCacheFieldSet;
