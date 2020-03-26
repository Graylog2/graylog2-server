// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Button } from 'components/graylog';
import { Spinner, Icon } from 'components/common';

const DirtyButton = styled(Button)`
  position: relative;
  &:after {
    position: absolute;
    content: '';
    height: 16px;
    width: 16px;
    top: -5px;
    right: -6px;
    border-radius: 50%;
    background-color: #ffc107;
  }
`;

type Props = {
  running: boolean,
  disabled: boolean,
  glyph: string,
  dirty: boolean,
};

const SearchButton = ({ running, disabled, glyph, dirty }: Props) => {
  const ButtonComponent = dirty ? DirtyButton : Button;
  return (
    <ButtonComponent type="submit"
                     bsStyle={running ? 'warning' : 'success'}
                     disabled={disabled}
                     className="pull-left search-button-execute">
      {running ? <Spinner fixedWidth pulse text="" /> : <Icon name={glyph} />}
    </ButtonComponent>
  );
};

SearchButton.defaultProps = {
  running: false,
  disabled: false,
  dirty: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  running: PropTypes.bool,
  disabled: PropTypes.bool,
  dirty: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
