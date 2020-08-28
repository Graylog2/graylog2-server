// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import styles from './EditableTitle.css';

const StyledStaticSpan = styled.span(({ theme }) => css`
  border: 1px solid ${theme.colors.global.contentBackground};
  padding: 2px 3px;
  font-size: ${theme.fonts.size.large};
  display: inline-block;
`);

const StyledInput = styled.input(({ theme }) => css`
  border: 1px solid ${theme.colors.input.border};
  background-color: ${theme.colors.input.background};
  color: ${theme.colors.input.color};
  border-radius: 4px;
  padding: 2px 3px;
  font-size: ${theme.fonts.size.large};
  
  :focus {
    border-color: ${theme.colors.input.borderFocus};
    //box-shadow: ${theme.colors.input.boxShadow};
    outline: none;
  }
`);

type Props = {
  disabled?: boolean,
  onChange: (newTitle: string) => void,
  value: string,
};

type State = {
  editing: boolean,
  value: string,
};

export default class EditableTitle extends React.Component<Props, State> {
  static propTypes = {
    disabled: PropTypes.bool,
    onChange: PropTypes.func,
    value: PropTypes.string.isRequired,
  };

  static defaultProps = {
    disabled: false,
    onChange: () => {},
  };

  constructor(props: Props) {
    super(props);
    const { value } = props;

    this.state = {
      editing: false,
      value,
    };
  }

  _toggleEditing = () => {
    const { disabled } = this.props;

    if (!disabled) {
      this.setState((state) => ({ editing: !state.editing }));
    }
  };

  _onBlur = () => {
    this._toggleEditing();
    this._submitValue();
  };

  _onChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    evt.preventDefault();
    this.setState({ value: evt.target.value });
  };

  _submitValue = () => {
    const { value } = this.state;
    const { onChange, value: propsValue } = this.props;

    if (value !== '') {
      onChange(value);
    } else {
      this.setState({ value: propsValue });
    }
  }

  _onSubmit = (e: SyntheticInputEvent<HTMLInputElement>) => {
    e.preventDefault();
    this._toggleEditing();
    this._submitValue();
  };

  render() {
    const { editing, value } = this.state;

    return editing ? (
      <span>
        <form onSubmit={this._onSubmit} className={styles.inlineForm}>
          {/* eslint-disable-next-line jsx-a11y/no-autofocus */}
          <StyledInput autoFocus
                       type="text"
                       value={value}
                       onBlur={this._onBlur}
                       onChange={this._onChange} />
        </form>
      </span>
    ) : <StyledStaticSpan onDoubleClick={this._toggleEditing} title="Double click the title to edit it.">{value}</StyledStaticSpan>;
  }
}
