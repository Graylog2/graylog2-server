import PropTypes from 'prop-types';
import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import ISODurationUtils from 'util/ISODurationUtils';

import ObjectUtils from 'util/ObjectUtils';

/**
 * Expects `this.props.options` to be an array of period/description objects. `[{period: 'PT1S', description: 'yo'}]`
 */
class TimeRangeOptionsForm extends React.Component {
  static propTypes = {
    options: PropTypes.array,
    title: PropTypes.string.isRequired,
    help: PropTypes.any.isRequired,
    addButtonTitle: PropTypes.string,
    update: PropTypes.func.isRequired,
    validator: PropTypes.func,
  };

  static defaultProps = {
    options: [],
    addButtonTitle: 'Add option',
    validator: () => true,
  };

  _update = (options) => {
    this.props.update(options);
  };

  _onAdd = () => {
    const options = ObjectUtils.clone(this.props.options);

    if (options) {
      options.push({ period: '', description: '' });
      this._update(options);
    }
  };

  _onRemove = (removedIdx) => {
    return () => {
      const options = ObjectUtils.clone(this.props.options);

      // Remove element at index
      options.splice(removedIdx, 1);

      this._update(options);
    };
  };

  _onChange = (changedIdx, field) => {
    return (e) => {
      const options = ObjectUtils.clone(this.props.options);

      options.forEach((o, idx) => {
        if (idx === changedIdx) {
          let value = e.target.value;

          if (field === 'period') {
            value = value.toUpperCase();
            if (!value.startsWith('P')) {
              value = `P${value}`;
            }
          }

          options[idx][field] = value;
        }
      });

      this._update(options);
    };
  };

  _buildTimeRangeOptions = () => {
    return this.props.options.map((option, idx) => {
      const period = option.period;
      const description = option.description;
      const errorStyle = ISODurationUtils.durationStyle(period, this.props.validator, 'has-error');

      return (
        <div key={`timerange-option-${idx}`}>
          <Row>
            <Col xs={4}>
              <div className={`input-group ${errorStyle}`}>
                <input type="text" className="form-control" value={period} onChange={this._onChange(idx, 'period')} />
                <span className="input-group-addon">
                  {ISODurationUtils.formatDuration(period, this.props.validator)}
                </span>
              </div>
            </Col>
            <Col xs={8}>
              <div className="input-group">
                <input type="text"
                       className="form-control"
                       placeholder="Add description..."
                       value={description}
                       onChange={this._onChange(idx, 'description')} />
                <span className="input-group-addon">
                  <i className="fa fa-trash" style={{ cursor: 'pointer' }} onClick={this._onRemove(idx)} />
                </span>
              </div>
            </Col>
          </Row>
        </div>
      );
    });
  };

  render() {
    return (
      <div className="form-group">
        <label className="control-label">{this.props.title}</label>
        <span className="help-block">{this.props.help}</span>
        <div className="wrapper">
          {this._buildTimeRangeOptions()}
        </div>
        <Button bsSize="xs" onClick={this._onAdd}>{this.props.addButtonTitle}</Button>
      </div>
    );
  }
}

export default TimeRangeOptionsForm;
