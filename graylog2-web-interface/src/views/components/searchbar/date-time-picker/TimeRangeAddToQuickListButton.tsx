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
import styled from 'styled-components';
import debounce from 'lodash/debounce';
import isEqual from 'lodash/isEqual';

import { ButtonToolbar, Button, Input, Popover } from 'components/bootstrap';
import { Icon, Portal } from 'components/common';
import type { TimeRange, KeywordTimeRange } from 'views/logic/queries/Query';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import { onSubmittingTimerange } from 'views/components/TimerangeForForm';
import useUserDateTime from 'hooks/useUserDateTime';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { QuickAccessTimeRange } from 'components/configurations/QuickAccessTimeRangeForm';

type Props = {
  addTimerange: (title: string) => void,
  toggleModal: () => void,
  target: Button | undefined | null,
  equalTimerange: QuickAccessTimeRange
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  margin-top: 5px;
`;

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
                 data-event-element="Add to quick list">
          <Input type="text"
                 placeholder="Add description..."
                 defaultValue={description}
                 onChange={({ target: { value } }) => debounceHandleOnChangeDescription(value)}
                 formGroupClassName="" />
          {!!equalTimerange && (
          <p>
            <span><Icon name="exclamation-triangle" />
              You already have similar time range in
              {' '}
              <Link to={Routes.SYSTEM.CONFIGURATIONS} target="_blank">Range configuration</Link>
              <p>
                <i>f.e. ({equalTimerange.description})</i>
              </p>
            </span>
          </p>
          )}
          <StyledButtonToolbar>
            <Button disabled={!description}
                    bsStyle="info"
                    type="submit"
                    bsSize="sm"
                    onClick={onAddTimerange}>
              Add
            </Button>
            <Button onClick={toggleModal}
                    bsSize="sm">
              Cancel
            </Button>
          </StyledButtonToolbar>
        </Popover>
      </Position>
    </Portal>
  );
};

const TimeRangeAddToQuickListButton = ({ timerange, isTimerangeValid }: {timerange: TimeRange, isTimerangeValid: boolean }) => {
  const { userTimezone } = useUserDateTime();
  const formTarget = useRef();
  const { config, refresh } = useSearchConfiguration();
  const [showForm, setShowForm] = useState(false);

  const toggleModal = useCallback(() => {
    setShowForm((cur) => !cur);
  }, []);
  const addTimerange = useCallback((description) => {
    ConfigurationsActions.update(ConfigurationType.SEARCHES_CLUSTER_CONFIG,
      {
        ...config,
        quick_access_timerange_presets: [
          ...config.quick_access_timerange_presets,
          { description, timerange: onSubmittingTimerange(timerange, userTimezone) }],
      }).then(() => {
      refresh();
      toggleModal();
    });
  }, [config, refresh, timerange, toggleModal, userTimezone]);

  const equalTimerange = useMemo(() => config
    .quick_access_timerange_presets
    .find((existingTimerange) => isTimerangeEqual(existingTimerange.timerange, onSubmittingTimerange(timerange, userTimezone))), [config.quick_access_timerange_presets, timerange, userTimezone]);

  return (
    <>
      <Button disabled={!isTimerangeValid} title="Add timerange to qiuck access timerange list" ref={formTarget} onClick={toggleModal}>
        <Icon name="thumbtack" />
      </Button>
      {showForm && (
        <TimeRangeAddToQuickListForm addTimerange={addTimerange} toggleModal={toggleModal} target={formTarget.current} equalTimerange={equalTimerange} />
      )}
    </>
  );
};

export default TimeRangeAddToQuickListButton;
