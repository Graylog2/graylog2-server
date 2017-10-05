import PropTypes from 'prop-types';
import React from 'react';
import { ControlLabel, FormGroup } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { MultiSelect } from 'components/common';

const QueryConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  _onStackedFieldChange(values) {
    this.props.onChange('stacked_fields', values);
  },
  render() {
    return (
      <div>
        <Input type="text"
               key="query"
               id="query"
               name="query"
               label="Search query"
               defaultValue={this.props.config.query}
               onChange={this.props.onChange}
               help="Search query that will be executed to get the widget value." />
        <Input type="number"
               id="limit"
               name="limit"
               label="Number of top/bottom values"
               required
               onChange={this.props.onChange}
               value={this.props.config.limit} />
        <Input type="number"
               id="data_table_limit"
               name="data_table_limit"
               label="Total table size"
               required
               onChange={this.props.onChange}
               value={this.props.config.data_table_limit} />
        <FormGroup>
          <ControlLabel>Sort options</ControlLabel>
          <Input type="radio"
                 name="sort_order"
                 label="Top values"
                 checked={this.props.config.sort_order === 'desc'}
                 value="desc"
                 onChange={this.props.onChange} />
          <Input type="radio"
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
  },
});

export default QueryConfiguration;
