import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

class HttpNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static defaultConfig = {
    url: '',
  };

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);
    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  render() {
    const { config, validation } = this.props;

    return (
      <React.Fragment>
        <Input id="notification-http-url"
               name="url"
               label="URL"
               type="text"
               bsStyle={validation.errors.url ? 'error' : null}
               help={validation.errors.url || 'The URL to POST to when an Event occurs.'}
               value={config.url || ''}
               onChange={this.handleChange}
               required />
      </React.Fragment>
    );
  }
}

export default HttpNotificationForm;
