import React, { PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import FormUtils from 'util/FormsUtils';

const SplitAndCountConverterConfiguration = React.createClass({
  propTypes: {
    type: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());
  },
  _getConverterObject(configuration) {
    return { type: this.props.type, config: configuration || this.props.configuration };
  },
  _toggleConverter(event) {
    let converter;
    if (FormUtils.getValueFromInput(event.target) === true) {
      converter = this._getConverterObject();
    }

    this.props.onChange(this.props.type, converter);
  },
  _onChange(key) {
    return (event) => {
      const newConfig = this.props.configuration;
      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(this.props.type, this._getConverterObject(newConfig));
    };
  },
  render() {
    const splitByHelpMessage = (
      <span>
        The Split & Count converter is splitting the extracted part by the defined character and stores the token{' '}
        count as field. <strong>Example:</strong> <em>?fields=first_name,last_name,zip</em> split by <em>,</em>{' '}
        results in <em>3</em>. You just counted the requested fields of a GET user REST request.
      </span>
    );

    return (
      <div className="xtrc-converter">
        <Input type="checkbox"
               ref="converterEnabled"
               id={`enable-${this.props.type}-converter`}
               label="Split & Count"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />
        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input type="text"
                     id={`${this.props.type}_converter_split_by`}
                     label="Split by"
                     defaultValue={this.props.configuration.split_by}
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     onChange={this._onChange('split_by')}
                     required={this.refs.converterEnabled && this.refs.converterEnabled.getChecked()}
                     help={splitByHelpMessage} />
            </div>
          </Col>
        </Row>
      </div>
    );
  },
});

export default SplitAndCountConverterConfiguration;
