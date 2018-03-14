import PropTypes from 'prop-types';
import React from 'react';
import { ControlLabel, FormGroup } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { MultiSelect } from 'components/common';

class QuickValuesConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    isHistogram: PropTypes.bool,
  };

  static defaultProps = { isHistogram: false };

  _onStackedFieldChange = (values) => {
    this.props.onChange('stacked_fields', values);
  };

  render() {
    let dataTableLimitForm;
    if (!this.props.isHistogram) {
      dataTableLimitForm = (
        <Input type="number"
               id="data_table_limit"
               name="data_table_limit"
               label="Total table size"
               required
               onChange={this.props.onChange}
               value={this.props.config.data_table_limit} />
      );
    }

    return (
      <div>
        <Input type="number"
               id="limit"
               name="limit"
               label="Number of top/bottom values"
               required
               onChange={this.props.onChange}
               value={this.props.config.limit} />
        {dataTableLimitForm}
        <FormGroup>
          <ControlLabel>Sort options</ControlLabel>
          <Input id="sort-order-desc"
                 type="radio"
                 name="sort_order"
                 label="Top values"
                 checked={this.props.config.sort_order === 'desc'}
                 value="desc"
                 onChange={this.props.onChange} />
          <Input id="sort-order-asc"
                 type="radio"
                 name="sort_order"
                 label="Bottom values"
                 checked={this.props.config.sort_order === 'asc'}
                 value="asc"
                 onChange={this.props.onChange} />
        </FormGroup>

        <FormGroup>
          <ControlLabel>Stacked fields</ControlLabel>
          <MultiSelect allowCreate value={this.props.config.stacked_fields} onChange={this._onStackedFieldChange} />
        </FormGroup>
      </div>
    );
  }
}

export default QuickValuesConfiguration;
