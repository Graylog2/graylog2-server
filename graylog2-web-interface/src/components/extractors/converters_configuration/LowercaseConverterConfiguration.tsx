import React from 'react';

import { Input } from 'components/bootstrap';
import FormUtils from 'util/FormsUtils';

type LowercaseConverterConfigurationProps = {
  type: string;
  configuration: any;
  onChange: (...args: any[]) => void;
};

class LowercaseConverterConfiguration extends React.Component<LowercaseConverterConfigurationProps, {
  [key: string]: any;
}> {
  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());
  }

  _getConverterObject = () => ({ type: this.props.type, config: this.props.configuration });

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
               label="Transform value to lowercase"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />
      </div>
    );
  }
}

export default LowercaseConverterConfiguration;
