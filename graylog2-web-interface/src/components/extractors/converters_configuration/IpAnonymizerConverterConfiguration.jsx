import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import FormUtils from 'util/FormsUtils';

class IpAnonymizerConverterConfiguration extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());
  }

  _getConverterObject = () => {
    return { type: this.props.type, config: this.props.configuration };
  };

  _toggleConverter = (event) => {
    let converter;
    if (FormUtils.getValueFromInput(event.target) === true) {
      converter = this._getConverterObject();
    }

    this.props.onChange(this.props.type, converter);
  };

  render() {
    return (
      <div className="xtrc-converter">
        <Input type="checkbox"
               id={`enable-${this.props.type}-converter`}
               label="Anonymize IPv4 addresses by replacing last octet"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />
      </div>
    );
  }
}

export default IpAnonymizerConverterConfiguration;
