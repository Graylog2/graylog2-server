import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import ISODurationUtils from 'components/common/ISODurationUtils';

import ObjectUtils from 'util/ObjectUtils';

const TimeRangeOptionsForm = React.createClass({
  propTypes: {
    options: React.PropTypes.object,
    title: React.PropTypes.string.isRequired,
    help: React.PropTypes.any.isRequired,
    addButtonTitle: React.PropTypes.string,
    update: React.PropTypes.func.isRequired,
    validator: React.PropTypes.func,
  },

  mixins: [ISODurationUtils],

  getDefaultProps() {
    return {
      options: {},
      addButtonTitle: 'Add option',
      validator: () => true,
    };
  },

  getInitialState() {
    return {optionsList: this._buildOptionsState(this.props.options)};
  },

  componentWillReceiveProps(newProps) {
    this.setState({optionsList: this._buildOptionsState(newProps.options)});
  },

  // Converts the object to a list of objects to make form handling easier.
  _buildOptionsState(options) {
    let optionsList = null;

    if (options) {
      optionsList = Object.keys(options).map((key) => {
        return {period: key, description: options[key]};
      });
    }

    return optionsList;
  },

  _update(options) {
    const update = {};

    options.forEach((option) => {
      update[option.period] = option.description;
    });

    this.props.update(update);
  },

  _onAdd() {
    const options = ObjectUtils.clone(this.state.optionsList);

    if (options) {
      options.push({period: 'PT0S', description: ''});
      this._update(options);
    }
  },

  _onRemove(removedIdx) {
    return () => {
      const options = ObjectUtils.clone(this.state.optionsList);

      // Remove element at index
      options.splice(removedIdx, 1);

      this._update(options);
    };
  },

  _onChange(changedIdx, field) {
    return (e) => {
      const options = ObjectUtils.clone(this.state.optionsList);

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
  },


  _buildTimeRangeOptions() {
    return this.state.optionsList.map((option, idx) => {
      const period = option.period;
      const description = option.description;
      const errorStyle = this.durationStyle(period, this.props.validator, 'has-error');

      return (
        <div key={'timerange-option-' + idx}>
          <Row>
            <Col xs={4}>
              <div className={`input-group ${errorStyle}`}>
                <input type="text" className="form-control" value={period} onChange={this._onChange(idx, 'period')} />
                <span className="input-group-addon">
                  {this.formatDuration(period, this.props.validator)}
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
                  <i className="fa fa-trash" style={{cursor: 'pointer'}} onClick={this._onRemove(idx)} />
                </span>
              </div>
            </Col>
          </Row>
        </div>
      );
    });
  },

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
  },
});

export default TimeRangeOptionsForm;
