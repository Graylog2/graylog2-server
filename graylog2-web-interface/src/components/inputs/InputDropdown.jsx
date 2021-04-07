/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';

const StyledInputDropdown = styled(Input)`
  float: left;
  width: 400px;
  margin-right: 10px;
`;

class InputDropdown extends React.Component {
  static propTypes = {
    inputs: PropTypes.object,
    title: PropTypes.string,
    preselectedInputId: PropTypes.string,
    onLoadMessage: PropTypes.func,
    disabled: PropTypes.bool,
  };

  PLACEHOLDER = 'placeholder';

  _formatInput = (input) => {
    return <option key={input.id} value={input.id}>{input.title} ({input.type})</option>;
  };

  _sortByTitle = (input1, input2) => {
    return input1.title.localeCompare(input2.title);
  };

  _onLoadMessage = () => {
    this.props.onLoadMessage(this.state.selectedInput);
  };

  _formatStaticInput = (input) => {
    return (
      <StyledInputDropdown id={`${input.type}-select`} type="select" disabled>
        <option>{`${input.title} (${input.type})`}</option>
      </StyledInputDropdown>
    );
  };

  onSelectedInputChange = (event) => {
    this.setState({ selectedInput: event.target.value });
  };

  state = {
    selectedInput: this.props.preselectedInputId || this.PLACEHOLDER,
  };

  render() {
    const { selectedInput } = this.state;

    // When an input is pre-selected, show a static dropdown
    if (this.props.inputs && this.props.preselectedInputId) {
      return (
        <div>
          {this._formatStaticInput(this.props.inputs.get(this.props.preselectedInputId))}

          <Button bsStyle="info"
                  disabled={selectedInput === this.PLACEHOLDER}
                  onClick={this._onLoadMessage}>
            {this.props.title}
          </Button>
        </div>
      );
    }

    if (this.props.inputs) {
      const inputs = this.props.inputs.sort(this._sortByTitle).map(this._formatInput);

      return (
        <div>
          <StyledInputDropdown id="placeholder-select"
                               type="select"
                               value={selectedInput}
                               onChange={this.onSelectedInputChange}
                               placeholder={this.PLACEHOLDER}>
            <option value={this.PLACEHOLDER}>Select an input</option>
            {inputs.toArray()}
          </StyledInputDropdown>

          <Button bsStyle="info"
                  disabled={this.props.disabled || selectedInput === this.PLACEHOLDER}
                  onClick={this._onLoadMessage}>
            {this.props.title}
          </Button>
        </div>
      );
    }

    return <Spinner />;
  }
}

export default InputDropdown;
