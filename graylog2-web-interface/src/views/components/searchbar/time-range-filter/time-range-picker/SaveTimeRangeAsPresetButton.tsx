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
import isEqual from 'lodash/isEqual';
import styled from 'styled-components';
import { useFormikContext, Formik, Form } from 'formik';

import { Button, Popover } from 'components/bootstrap';
import { Icon, Portal, ModalSubmit, FormikInput } from 'components/common';
import type { TimeRange, KeywordTimeRange } from 'views/logic/queries/Query';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useUserDateTime from 'hooks/useUserDateTime';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import generateId from 'logic/generateId';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type {
  TimeRangePickerFormValues,
} from 'views/components/searchbar/time-range-filter/time-range-picker/TimeRangePicker';
import {
  normalizeFromPickerForSearchBar,
  normalizeFromSearchBarForBackend,
} from 'views/logic/queries/NormalizeTimeRange';
import { NO_TIMERANGE_OVERRIDE } from 'views/Constants';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type FormValues = {
  description: string
}

const StyledModalSubmit = styled(ModalSubmit)`
  margin-top: 15px;
`;

const isTimerangeEqual = (firstTimerange: TimeRange, secondTimerange: TimeRange) => {
  if (firstTimerange.type !== secondTimerange.type) return false;
  if (firstTimerange.type === 'keyword') return (firstTimerange as KeywordTimeRange).keyword === (secondTimerange as KeywordTimeRange).keyword;

  return isEqual(firstTimerange, secondTimerange);
};

const validate = ({ description }: FormValues) => {
  if (!description) {
    return { description: 'Description is required' };
  }

  return ({});
};

type Props = {
  addTimerange: (title: string) => void,
  toggleModal: () => void,
  target: typeof Button | undefined | null,
};

const TimeRangeAddToQuickListForm = ({ addTimerange, toggleModal, target }: Props) => {
  const { userTimezone } = useUserDateTime();
  const { config } = useSearchConfiguration();
  const { values: { timeRangeTabs, activeTab } } = useFormikContext<TimeRangePickerFormValues>();

  const activeTabTimeRange = timeRangeTabs[activeTab];

  const equalTimerange = useMemo(() => (config
    ?.quick_access_timerange_presets
    ?.find((existingPreset) => isTimerangeEqual(
      existingPreset.timerange,
      normalizeFromSearchBarForBackend((
        normalizeFromPickerForSearchBar(activeTabTimeRange)
      ) as TimeRange, userTimezone),
    ))), [config, activeTabTimeRange, userTimezone]);

  const onSubmit = ({ description }: { description: string }) => addTimerange(description);

  return (
    <Portal>
      <Position placement="left"
                target={target}>
        <Popover title="Save as preset"
                 id="time-range-preset-popover"
                 data-testid="time-range-preset-popover">
          <Formik<FormValues> onSubmit={onSubmit} initialValues={{ description: '' }} validate={validate}>
            {({ isValid }) => (
              <Form>
                <FormikInput type="text"
                             name="description"
                             id="time-range-preset-description"
                             placeholder="Add description..."
                             aria-label="Time range description"
                             formGroupClassName="" />
                {!!equalTimerange && (
                  <p>
                    <Icon name="exclamation-triangle" />
                    You already have similar time range in{' '}
                    <Link to={Routes.SYSTEM.CONFIGURATIONS} target="_blank">Range configuration</Link>
                    <br />
                    <i>({equalTimerange.description})</i>
                  </p>
                )}
                <StyledModalSubmit disabledSubmit={!isValid}
                                   submitButtonText="Save preset"
                                   isAsyncSubmit={false}
                                   displayCancel
                                   onCancel={toggleModal}
                                   bsSize="small" />
              </Form>
            )}
          </Formik>
        </Popover>
      </Position>
    </Portal>
  );
};

const SaveTimeRangeAsPresetButton = () => {
  const { userTimezone } = useUserDateTime();
  const { values: { timeRangeTabs, activeTab }, errors } = useFormikContext<TimeRangePickerFormValues>();
  const formTarget = useRef();
  const activeTabTimeRange = timeRangeTabs[activeTab];

  const { config, refresh } = useSearchConfiguration();
  const [showForm, setShowForm] = useState(false);
  const sendTelemetry = useSendTelemetry();

  const isValidTimeRange = !errors.timeRangeTabs?.[activeTab];

  const toggleModal = useCallback(() => {
    setShowForm((cur) => !cur);
  }, []);

  const addTimerange = useCallback((description: string) => {
    const timeRangePreset = {
      description,
      timerange: activeTabTimeRange ? normalizeFromSearchBarForBackend(normalizeFromPickerForSearchBar(activeTabTimeRange) as TimeRange, userTimezone) : NO_TIMERANGE_OVERRIDE,
      id: generateId(),
    };

    ConfigurationsActions.update(ConfigurationType.SEARCHES_CLUSTER_CONFIG,
      {
        ...config,
        quick_access_timerange_presets: [
          ...config.quick_access_timerange_presets,
          timeRangePreset],
      }).then(() => {
      refresh();
      toggleModal();
    });

    if (timeRangePreset) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PRESET_ADD_QUICK_ACCESS, {
        app_pathname: 'search',
        app_section: 'search-bar',
        event_details: {
          timerange: timeRangePreset.timerange,
          id: timeRangePreset.id,
        },
      });
    }
  }, [config, refresh, sendTelemetry, activeTabTimeRange, toggleModal, userTimezone]);

  return (
    <>
      <Button disabled={!isValidTimeRange}
              title="Save current time range as preset"
              ref={formTarget}
              bsSize="small"
              onClick={toggleModal}>
        Save as preset
      </Button>
      {showForm && (
        <TimeRangeAddToQuickListForm addTimerange={addTimerange}
                                     toggleModal={toggleModal}
                                     target={formTarget.current} />
      )}
    </>
  );
};

export default SaveTimeRangeAsPresetButton;
