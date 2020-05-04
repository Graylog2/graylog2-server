// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';

const StyledButton: StyledComponent<{}, void, Button> = styled(Button)`
  margin-right: 7px;
`;

const DirtyButton: StyledComponent<{}, void, Button> = styled(StyledButton)`
  position: relative;

  ::after {
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
  disabled: boolean,
  glyph: string,
  dirty: boolean,
};

const SearchButton = ({ disabled, glyph, dirty }: Props) => {
  const ButtonComponent = dirty ? DirtyButton : StyledButton;
  const title = dirty ? 'Perform search (changes were made after last search execution)' : 'Perform search';
  return (
    <ButtonComponent type="submit"
                     bsStyle="success"
                     disabled={disabled}
                     title={title}
                     className="pull-left">
      <Icon name={glyph} />
    </ButtonComponent>
  );
};

SearchButton.defaultProps = {
  disabled: false,
  dirty: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  disabled: PropTypes.bool,
  dirty: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
