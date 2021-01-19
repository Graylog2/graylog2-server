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
import { Field } from 'formik';
import styled, { css } from 'styled-components';

import { Icon, IfPermitted } from 'components/common';
import { DropdownButton, MenuItem } from 'components/graylog';
import useSearchConfiguration from 'hooks/useSearchConfiguration';

import TimerangeSelector from '../TimerangeSelector';

type Props = {
  disabled?: boolean,
  onChange?: (range: number) => void,
};

const StyledTimerangeSelector = styled(TimerangeSelector)`
  align-self: flex-end;
`;

const ExternalIcon = styled(Icon)`
  margin-left: 6px;
`;

const AdminMenuItem = styled(MenuItem)(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
`);

export default function ConfiguredRelativeTimeRangeSelector({ disabled, onChange }: Props) {
  const { config } = useSearchConfiguration();
  const availableOptions = config?.relative_timerange_options;
  const timeRangeLimit = moment.duration(config?.query_time_range_limit);
  let options;

  if (availableOptions) {
    let all = null;

    options = Object.keys(availableOptions).map((key) => {
      const seconds = moment.duration(key).asSeconds();

      if (timeRangeLimit.asSeconds() > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
        return null;
      }

      const optionLabel = availableOptions[key].replace(/Search\sin(\sthe\slast)?\s/, '');
      const option = (<MenuItem eventKey={seconds} key={`relative-option-${key}`} disabled={disabled}>{optionLabel}</MenuItem>);

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

  return (
    <Field name="timerange-existing-range">
      {() => {
        const _onChange = (newValue) => {
          onChange(Number.parseInt(newValue, 10));
        };

        return (
          <StyledTimerangeSelector className="relative">

            <DropdownButton title={availableOptions ? 'Preset Times' : 'Loading Ranges...'}
                            id="relative-timerange-selector"
                            bsSize="small"
                            onSelect={_onChange}>
              {options}
              <IfPermitted permissions="clusterconfigentry:edit">
                <MenuItem divider />
                <AdminMenuItem href="/system/configurations" target="_blank">Configure Ranges <ExternalIcon name="external-link-alt" /></AdminMenuItem>
              </IfPermitted>
            </DropdownButton>

          </StyledTimerangeSelector>
        );
      }}
    </Field>
  );
}

ConfiguredRelativeTimeRangeSelector.propTypes = {
  onChange: PropTypes.func,
  disabled: PropTypes.bool,
};

ConfiguredRelativeTimeRangeSelector.defaultProps = {
  disabled: false,
  onChange: () => {},
};
