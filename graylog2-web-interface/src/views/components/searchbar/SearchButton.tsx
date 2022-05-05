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
import React from 'react';
import PropTypes from 'prop-types';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { IconName } from 'components/common/Icon';

const StyledButton = styled(Button)(({ theme, $dirty }: { theme: DefaultTheme, $dirty: boolean }) => css`
  position: relative;
  margin-right: 12px;
  min-width: 61px;

  ${$dirty ? css` 
    &::after {
      position: absolute;
      content: '';
      height: 16px;
      width: 16px;
      top: -5px;
      right: -6px;
      border-radius: 50%;
      background-color: ${theme.colors.variant.warning};
    }
  ` : ''}
`);

type Props = {
  disabled: boolean,
  glyph: IconName,
  dirty: boolean,
};

const onButtonClick = (e: MouseEvent, disabled: Boolean) => {
  if (disabled) {
    e.preventDefault();
    QueryValidationActions.displayValidationErrors();
  }
};

const SearchButton = ({ dirty, disabled, glyph }: Props) => {
  const className = disabled ? 'disabled' : '';
  const title = dirty ? 'Perform search (changes were made after last search execution)' : 'Perform Search';

  return (
    <StyledButton onClick={(e) => onButtonClick(e, disabled)}
                  title={title}
                  className={className}
                  type="submit"
                  bsStyle="success"
                  $dirty={dirty}>
      <Icon name={glyph} />
    </StyledButton>
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
