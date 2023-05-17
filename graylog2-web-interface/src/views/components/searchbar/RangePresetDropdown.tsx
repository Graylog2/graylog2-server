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
import React, { useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { Icon, IfPermitted } from 'components/common';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useUserDateTime from 'hooks/useUserDateTime';
import { onInitializingTimerange } from 'views/components/TimerangeForForm';
import type {
  TimeRange,
  KeywordTimeRange,
  RelativeTimeRangeWithEnd,
  AbsoluteTimeRange,
  RelativeTimeRangeStartOnly,
} from 'views/logic/queries/Query';
import type { QuickAccessTimeRange } from 'components/configurations/QuickAccessTimeRangeForm';
import ToolsStore from 'stores/tools/ToolsStore';

type Props = {
  onToggle?: (open: boolean) => void,
  className?: string,
  displayTitle?: boolean,
  bsSize?: string,
  header: string,
  disabled?: boolean,
  onChange?: (timerange: TimeRange) => void,
  availableOptions: Array<QuickAccessTimeRange>,
};

const getPassedByLimit = async (quickAccessTimeRange: QuickAccessTimeRange, limit: number) => {
  const { timerange } = quickAccessTimeRange;

  const checkIsPassed = async () => {
    switch (timerange.type) {
      case 'relative':
        return ((timerange as RelativeTimeRangeWithEnd).from || (timerange as RelativeTimeRangeStartOnly).range) <= limit;
      case 'absolute':
        return moment().diff((timerange as AbsoluteTimeRange).from, 'seconds') <= limit;
      case 'keyword':
        return ToolsStore.testNaturalDate((timerange as KeywordTimeRange).keyword, (timerange as KeywordTimeRange).timezone)
          .then((response) => moment().diff(response.from, 'seconds') <= limit);
      default:
        throw Error('Timerange type doesn\'t not exist');
    }
  };

  const isPassed = await checkIsPassed();

  return isPassed ? quickAccessTimeRange : null;
};

const ExternalIcon = styled(Icon)`
  margin-left: 6px;
`;

const AdminMenuItem = styled(MenuItem)(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
`);

const RangePresetDropdown = ({ availableOptions, disabled, onChange, onToggle, className, displayTitle, bsSize, header }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { config } = useSearchConfiguration();
  const [filtratedByLimitOptions, setFiltratedByLimitOptions] = useState([]);

  const timeRangeLimit = useMemo(() => moment.duration(config?.query_time_range_limit).asSeconds(), [config?.query_time_range_limit]);
  const title = displayTitle && (availableOptions ? 'Preset Times' : 'Loading Ranges...');

  useEffect(() => {
    const filtrateOptions = async () => {
      const res = timeRangeLimit === 0 ? availableOptions : await Promise
        .all(availableOptions?.map((quickAccessTimeRange) => getPassedByLimit(quickAccessTimeRange, timeRangeLimit)));

      setFiltratedByLimitOptions(res.filter((item) => item !== null));
    };

    filtrateOptions();
  }, [availableOptions, timeRangeLimit]);

  let options;

  if (filtratedByLimitOptions?.length) {
    options = filtratedByLimitOptions.map(({ description, timerange, id }) => {
      const optionLabel = description.replace(/Search\sin(\sthe\slast)?\s/, '');

      const option = (
        <MenuItem eventKey={timerange} key={`timerange-option-${id}`} disabled={disabled}>{optionLabel}</MenuItem>);

      return option;
    });
  } else {
    options = (<MenuItem eventKey="300" disabled>Loading...</MenuItem>);
  }

  const { formatTime } = useUserDateTime();

  const _onChange = (timerange) => {
    if (timerange !== null && timerange !== undefined) {
      sendTelemetry('input_value_change', {
        app_pathname: 'search',
        app_section: 'search-bar',
        app_action_value: 'quick-access-timerange-selector',
        event_details: { timerange },
      });

      onChange(onInitializingTimerange(timerange, formatTime));
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
