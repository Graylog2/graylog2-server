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
import { useCallback, useMemo, useRef, useState } from 'react';
import { Position } from 'react-overlays';
import debounce from 'lodash/debounce';
import isEqual from 'lodash/isEqual';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { Button, Input, Popover } from 'components/bootstrap';
import { Icon, Portal, ModalSubmit } from 'components/common';
import type { TimeRange, KeywordTimeRange } from 'views/logic/queries/Query';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useUserDateTime from 'hooks/useUserDateTime';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { QuickAccessTimeRange } from 'components/configurations/QuickAccessTimeRangeForm';
import generateId from 'logic/generateId';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type {
  TimeRangePickerFormValues,
} from 'views/components/searchbar/time-range-filter/time-range-picker/TimeRangePicker';
import {
  normalizeFromPickerForSearchBar,
  normalizeFromSearchBarForBackend,
} from 'views/logic/queries/NormalizeTimeRange';

const StyledModalSubmit = styled(ModalSubmit)`
  margin-top: 15px;
`;

type Props = {
  addTimerange: (title: string) => void,
  toggleModal: () => void,
  target: Button | undefined | null,
  equalTimerange: QuickAccessTimeRange
};

const isTimerangeEqual = (firstTimerange: TimeRange, secondTimerange: TimeRange) => {
  if (firstTimerange.type !== secondTimerange.type) return false;
  if (firstTimerange.type === 'keyword') return (firstTimerange as KeywordTimeRange).keyword === (secondTimerange as KeywordTimeRange).keyword;

  return isEqual(firstTimerange, secondTimerange);
};

const TimeRangeAddToQuickListForm = ({ addTimerange, toggleModal, target, equalTimerange } : Props) => {
  const [description, setDescription] = useState('');
  const debounceHandleOnChangeDescription = debounce((value: string) => setDescription(value), 300);

  const onAddTimerange = useCallback(() => addTimerange(description), [addTimerange, description]);

  return (
    <Portal>
      <Position placement="left"
                target={target}>
        <Popover title="Add to quick access list"
                 id="add-to-quick-list-popover"
                 data-app-section="add-to-quick-list-popover_form"
                 data-event-element="Add to quick list"
                 data-testid="add-to-quick-list-popover">
          <Input type="text"
                 id="add-to-quick-list-description"
                 placeholder="Add description..."
                 title="Time range description"
                 aria-label="Time range description"
                 defaultValue={description}
                 onChange={({ target: { value } }) => debounceHandleOnChangeDescription(value)}
                 formGroupClassName="" />
          {!!equalTimerange && (
            <p>
              <Icon name="exclamation-triangle" />
              You already have similar time range in{' '}
              <Link to={Routes.SYSTEM.CONFIGURATIONS} target="_blank">Range configuration</Link>
              <br />
              <i>f.e. ({equalTimerange.description})</i>
            </p>
          )}
          <StyledModalSubmit disabledSubmit={!description}
                             submitButtonText="Add time range"
                             isAsyncSubmit={false}
                             displayCancel
                             onCancel={toggleModal}
                             onSubmit={onAddTimerange}
                             bsSize="small" />
        </Popover>
      </Position>
    </Portal>
  );
};

const TimeRangeAddToQuickListButton = () => {
  const { userTimezone } = useUserDateTime();
  const { values, errors } = useFormikContext<TimeRangePickerFormValues>();
  const formTarget = useRef();

  const { config, refresh } = useSearchConfiguration();
  const [showForm, setShowForm] = useState(false);
  const sendTelemetry = useSendTelemetry();

  const isValidTimeRange = !errors.nextTimeRange;

  const toggleModal = useCallback(() => {
    setShowForm((cur) => !cur);
  }, []);

  const addTimerange = useCallback((description: string) => {
    const quickAccessTimerangePreset = {
      description,
      timerange: normalizeFromSearchBarForBackend(normalizeFromPickerForSearchBar(values.nextTimeRange) as TimeRange, userTimezone),
      id: generateId(),
    };

    ConfigurationsActions.update(ConfigurationType.SEARCHES_CLUSTER_CONFIG,
      {
        ...config,
        quick_access_timerange_presets: [
          ...config.quick_access_timerange_presets,
          quickAccessTimerangePreset],
      }).then(() => {
      refresh();
      toggleModal();
    });

    if (quickAccessTimerangePreset) {
      sendTelemetry('form_submit', {
        app_pathname: 'search',
        app_section: 'search-bar',
        app_action_value: 'add_to_quick_access_timerange_presets',
        event_details: {
          timerange: quickAccessTimerangePreset.timerange,
          id: quickAccessTimerangePreset.id,
        },
      });
    }
  }, [config, refresh, sendTelemetry, values.nextTimeRange, toggleModal, userTimezone]);

  const equalTimerange = useMemo(() => config
    ?.quick_access_timerange_presets
    ?.find((existingTimerange) => isTimerangeEqual(
      existingTimerange.timerange,
      normalizeFromSearchBarForBackend(values.nextTimeRange as TimeRange, userTimezone),
    )), [config, values.nextTimeRange, userTimezone]);

  return (
    <>
      <Button disabled={!isValidTimeRange}
              title="Add time range to quick access time range list"
              ref={formTarget}
              bsSize="small"
              onClick={toggleModal}>
        Save as preset
      </Button>
      {showForm && (
        <TimeRangeAddToQuickListForm addTimerange={addTimerange}
                                     toggleModal={toggleModal}
                                     target={formTarget.current}
                                     equalTimerange={equalTimerange} />
      )}
    </>
  );
};

export default TimeRangeAddToQuickListButton;
