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
import { Icon, Spinner } from 'components/common';
import QueryValidationActions from 'views/actions/QueryValidationActions';
import type { IconName } from 'components/common/Icon';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const StyledButton = styled(Button)<{ $dirty: boolean }>(({ theme, $dirty }) => css`
  position: relative;
  min-width: 63px;

  &&&.disabled {
    color: ${theme.utils.contrastingColor(theme.colors.variant.success)};
  }

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
  displaySpinner?: boolean,
};

const onButtonClick = (e: MouseEvent, disabled: Boolean, triggerTelemetry: () => void) => {
  if (disabled) {
    e.preventDefault();
    QueryValidationActions.displayValidationErrors();
  }

  triggerTelemetry();
};

const SearchButton = ({ dirty, disabled, glyph, displaySpinner }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const className = disabled ? 'disabled' : '';
  const title = dirty ? 'Perform search (changes were made after last search execution)' : 'Perform Search';

  const triggerTelemetry = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_BUTTON_CLICKED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'search-button',
      event_details: {
        disabled,
      },
    });
  };

  return (
    <StyledButton onClick={(e) => onButtonClick(e, disabled, triggerTelemetry)}
                  title={title}
                  className={className}
                  type="submit"
                  bsStyle="success"
                  $dirty={dirty}>
      {displaySpinner ? <Spinner delay={0} text="" /> : <Icon name={glyph} />}
    </StyledButton>
  );
};

SearchButton.defaultProps = {
  disabled: false,
  displaySpinner: false,
  dirty: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  disabled: PropTypes.bool,
  displaySpinner: PropTypes.bool,
  dirty: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
