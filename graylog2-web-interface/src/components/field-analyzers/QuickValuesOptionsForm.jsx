import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { Button, Col, Row } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

const QuickValuesOptionsForm = React.createClass({
  propTypes: {
    limit: PropTypes.number.isRequired,
    tableSize: PropTypes.number.isRequired,
    order: PropTypes.string.isRequired,
    onSave: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      limit: this.props.limit,
      tableSize: this.props.tableSize,
      order: this.props.order,
    };
  },

  _onChange(event) {
    const state = _.cloneDeep(this.state);
    state[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState(state);
  },

  _onOrderChange(value) {
    return (e) => {
      e.preventDefault();
      this._onChange(FormsUtils.inputEvent('order', value));
    };
  },

  _onSave(e) {
    e.preventDefault();
    this.props.onSave(this.state);
  },

  render() {
    return (
      <Row>
        <form className="form form-horizontal" onSubmit={this._onSave}>
          <fieldset>
            <Col md={10}>
              <Input type="number"
                     id="limit"
                     name="limit"
                     label="Number of top/bottom values"
                     autoFocus
                     required
                     onChange={this._onChange}
                     value={this.state.limit}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
              <Input type="number"
                     id="tableSize"
                     name="tableSize"
                     label="Total table size"
                     required
                     onChange={this._onChange}
                     value={this.state.tableSize}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
              <Input type="radio"
                     name="order"
                     label="Top values"
                     checked={this.state.order === 'desc'}
                     onChange={this._onOrderChange('desc')}
                     wrapperClassName="col-md-offset-3 col-md-9" />
              <Input type="radio"
                     name="order"
                     label="Bottom values"
                     checked={this.state.order === 'asc'}
                     onChange={this._onOrderChange('asc')}
                     wrapperClassName="col-md-offset-3 col-md-9" />

              <Input wrapperClassName="col-sm-offset-3 col-sm-9">
                <Button type="submit" bsStyle="success" bsSize="small">Update</Button>
              </Input>
            </Col>
          </fieldset>
        </form>
      </Row>
    );
  },
});

export default QuickValuesOptionsForm;
