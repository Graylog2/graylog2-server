import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { URLWhiteListInput } from 'components/common';
import FormsUtils from 'util/FormsUtils';

class HttpNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
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

  static defaultConfig = {
    url: '',
  }

  render() {
    const { config, validation } = this.props;

    return (
      <>
        <URLWhiteListInput label="URL"
                           onChange={this.handleChange}
                           validationState={validation.errors.url ? 'error' : null}
                           validationMessage={lodash.get(validation, 'errors.url[0]', 'The URL to POST to when an Event occurs.')}
                           url={config.url} />
      </>
    );
  }
}

export default HttpNotificationForm;
