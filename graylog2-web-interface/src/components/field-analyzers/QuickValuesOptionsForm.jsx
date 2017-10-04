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
    onCancel: PropTypes.func.isRequired,
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

  _onCancel(event) {
    event.preventDefault();
    this.props.onCancel();
  },

  _onSave(e) {
    e.preventDefault();
    this.props.onSave(this.state);
  },

  render() {
    return (
      <Row>
        <Col md={6}>
          <form className="form" onSubmit={this._onSave}>
            <fieldset style={{ paddingLeft: 15 }}>
              <Input type="number"
                     id="limit"
                     name="limit"
                     label="Number of top/bottom values"
                     autoFocus
                     required
                     onChange={this._onChange}
                     value={this.state.limit} />
              <Input type="number"
                     id="tableSize"
                     name="tableSize"
                     label="Total table size"
                     required
                     onChange={this._onChange}
                     value={this.state.tableSize} />
              <Input label="Sort options">
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
              </Input>

              <Button type="submit" bsStyle="success" bsSize="small">Update</Button>
              {' '}
              <Button bsSize="small" onClick={this._onCancel}>Cancel</Button>
            </fieldset>
          </form>
        </Col>
      </Row>
    );
  },
});

export default QuickValuesOptionsForm;
