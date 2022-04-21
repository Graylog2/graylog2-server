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
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { IconName } from 'components/common/Icon';

const StyledButton = styled(Button)`
  margin-right: 12px;
  min-width: 61px;
`;

const DirtyButton = styled(StyledButton)(({ theme }) => css`
  position: relative;

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

const COMMON_PROPS = {
  type: 'submit',
  bsStyle: 'success',
};

const DirtySearchButton = ({ glyph, className, disabled }: { disabled: boolean, glyph: IconName, className: string }) => (
  <DirtyButton onClick={(e) => onButtonClick(e, disabled)}
               title="Perform search (changes were made after last search execution)"
               className={className}
               {...COMMON_PROPS}>
    <Icon name={glyph} />
  </DirtyButton>
);

const CleanSearchButton = ({ disabled, glyph, className }: { disabled: boolean, glyph: IconName, className: string }) => (
  <StyledButton onClick={(e) => onButtonClick(e, disabled)}
                title="Perform search"
                className={className}
                {...COMMON_PROPS}>
    <Icon name={glyph} />
  </StyledButton>
);

const SearchButton = ({ dirty, disabled, ...rest }: Props) => {
  const className = disabled ? 'disabled' : '';

  return dirty
    ? <DirtySearchButton className={className} disabled={disabled} {...rest} />
    : <CleanSearchButton className={className} disabled={disabled} {...rest} />;
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
