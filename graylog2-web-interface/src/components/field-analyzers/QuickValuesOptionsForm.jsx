import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, Row } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { MultiSelect, Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';
import SearchUtils from 'util/SearchUtils';

import style from './QuickValuesOptionsForm.css';

const QuickValuesOptionsForm = React.createClass({
  propTypes: {
    limit: PropTypes.number.isRequired,
    tableSize: PropTypes.number.isRequired,
    order: PropTypes.string.isRequired,
    field: PropTypes.string.isRequired,
    stackedFields: PropTypes.string,
    stackedFieldsOptions: PropTypes.arrayOf(PropTypes.object).isRequired,
    interval: PropTypes.string,
    isHistogram: PropTypes.bool.isRequired,
    onSave: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      limit: this.props.limit,
      tableSize: this.props.tableSize,
      order: this.props.order,
      stackedFields: this.props.stackedFields,
      interval: this.props.interval,
    };
  },

  _changeConfig(key, value) {
    const state = _.cloneDeep(this.state);
    state[key] = value;
    this.setState(state);
  },

  _onChange(event) {
    this._changeConfig(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _onStackedFieldChange(values) {
    this._changeConfig('stackedFields', values);
  },

  _onIntervalChange(value) {
    this._changeConfig('interval', value);
  },

  _onCancel() {
    this.props.onCancel();
  },

  _onSave(e) {
    e.preventDefault();
    this.props.onSave(this.state);
  },

  render() {
    const fieldOptions = this.props.stackedFieldsOptions
      .filter(field => !field.name.startsWith('gl2_')) // Do not include Graylog internal fields
      .filter(field => field.name !== this.props.field) // Do not include the main QuickValues field
      .map((field) => {
        return { value: field.name, label: field.name };
      });

    let tableSizeForm = null;
    let intervalForm = null;
    if (this.props.isHistogram) {
      const intervalOptions = SearchUtils.histogramIntervals().map((interval) => {
        return { value: interval, label: interval };
      });
      intervalForm = (
        <FormGroup>
          <ControlLabel>Interval</ControlLabel>
          <Select options={intervalOptions}
                  value={this.state.interval}
                  onChange={this._onIntervalChange} />
        </FormGroup>
      );
    } else {
      tableSizeForm = (
        <Input type="number"
               id="tableSize"
               name="tableSize"
               label="Total table size"
               required
               onChange={this._onChange}
               value={this.state.tableSize} />
      );
    }

    return (
      <Row>
        <Col md={6}>
          <form className="form" onSubmit={this._onSave}>
            <fieldset className={style.optionsFieldSet}>
              <Input type="number"
                     id="limit"
                     name="limit"
                     label="Number of top/bottom values"
                     autoFocus
                     required
                     onChange={this._onChange}
                     value={this.state.limit} />

              {tableSizeForm}

              <FormGroup>
                <ControlLabel>Sort options</ControlLabel>
                <Input type="radio"
                       name="order"
                       label="Top values"
                       checked={this.state.order === 'desc'}
                       value="desc"
                       onChange={this._onChange} />
                <Input type="radio"
                       name="order"
                       label="Bottom values"
                       checked={this.state.order === 'asc'}
                       value="asc"
                       onChange={this._onChange} />
              </FormGroup>

              <FormGroup>
                <ControlLabel>Stacked fields</ControlLabel>
                <MultiSelect options={fieldOptions}
                             value={this.state.stackedFields}
                             onChange={this._onStackedFieldChange} />
              </FormGroup>

              {intervalForm}

              <ButtonToolbar>
                <Button type="submit" bsStyle="success" bsSize="small">Update</Button>
                <Button bsSize="small" onClick={this._onCancel}>Cancel</Button>
              </ButtonToolbar>
            </fieldset>
          </form>
        </Col>
      </Row>
    );
  },
});

export default QuickValuesOptionsForm;
