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
import * as React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { Icon, IfPermitted } from 'components/common';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

type Props = {
  onToggle?: (open: boolean) => void,
  className?: string,
  displayTitle?: boolean,
  bsSize?: string,
  header: string,
  disabled?: boolean,
  onChange?: (range: number) => void,
};

const ExternalIcon = styled(Icon)`
  margin-left: 6px;
`;

const AdminMenuItem = styled(MenuItem)(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
`);

const RangePresetDropdown = ({ disabled, onChange, onToggle, className, displayTitle, bsSize, header }: Props) => {
  const { config } = useSearchConfiguration();
  const sendTelemetry = useSendTelemetry();
  const availableOptions = config?.relative_timerange_options;
  const timeRangeLimit = moment.duration(config?.query_time_range_limit);
  const title = displayTitle && (availableOptions ? 'Preset Times' : 'Loading Ranges...');
  let options;

  if (availableOptions) {
    let all = null;

    options = Object.keys(availableOptions).map((key) => {
      const seconds = moment.duration(key).asSeconds();

      if (timeRangeLimit.asSeconds() > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
        return null;
      }

      const optionLabel = availableOptions[key].replace(/Search\sin(\sthe\slast)?\s/, '');
      const option = (
        <MenuItem eventKey={seconds} key={`relative-option-${key}`} disabled={disabled}>{optionLabel}</MenuItem>);

      // The "search in all messages" option should be the last one.
      if (key === 'PT0S') {
        all = option;

        return null;
      }

      return option;
    });

    if (all) {
      options.push(all);
    }
  } else {
    options = (<MenuItem eventKey="300" disabled>Loading...</MenuItem>);
  }

  const _onChange = (range) => {
    if (range !== null && range !== undefined) {
      sendTelemetry('input_value_change', {
        app_pathname: 'search',
        app_section: 'search-bar',
        app_action_value: 'relative-timerange-selector',
        event_details: { range },
      });

      onChange(parseInt(range, 10));
    }
  };

  return (
    <DropdownButton title={title}
                    id="relative-timerange-selector"
                    aria-label="Open time range preset select"
                    bsSize={bsSize}
                    className={className}
                    onToggle={onToggle}
                    onSelect={_onChange}>
      {header && (
        <MenuItem header>{header}</MenuItem>
      )}
      {options}
      <IfPermitted permissions="clusterconfigentry:edit">
        <MenuItem divider />
        <AdminMenuItem href="/system/configurations" target="_blank">Configure Ranges <ExternalIcon name="external-link-alt" />
        </AdminMenuItem>
      </IfPermitted>
    </DropdownButton>
  );
};

RangePresetDropdown.propTypes = {
  bsSize: PropTypes.string,
  className: PropTypes.string,
  disabled: PropTypes.bool,
  displayTitle: PropTypes.bool,
  header: PropTypes.string,
  onChange: PropTypes.func,
  onToggle: PropTypes.func,
};

RangePresetDropdown.defaultProps = {
  bsSize: 'small',
  className: undefined,
  disabled: false,
  onChange: undefined,
  onToggle: undefined,
  header: undefined,
  displayTitle: true,
};

export default RangePresetDropdown;
