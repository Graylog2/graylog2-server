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
import type { Dispatch } from 'react';
import React, { useCallback, useMemo } from 'react';
import moment from 'moment/moment';
import styled from 'styled-components';
import Immutable from 'immutable';
import debounce from 'lodash/debounce';

import { Button, Input } from 'components/bootstrap';
import { Icon, SortableList } from 'components/common';
import type { TimeRange } from 'views/logic/queries/Query';
import { useStore } from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import TimeRangeFilter from 'views/components/searchbar/time-range-filter';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';
import generateId from 'logic/generateId';

export type TimeRangePreset = {
  timerange: TimeRange,
  description: string,
  id: string,
}

const ItemWrapper = styled.div`
  display: flex;
  align-items: stretch;
  gap: 5px;
  flex-grow: 1;
`;

const StyledInput = styled(Input)`
  margin-bottom: 0;
  width: 200px;
`;

const IconWrapper = styled.div`
  width: 40px;
`;

const Description = styled.div`
  display: flex;
`;

type ItemProps = {
  idx: number,
  id: string,
  timerange: TimeRange,
  description: string,
  onChange: (timerange: TimeRangePreset, idx: number) => void,
  onRemove: (idx: number) => void,
  limitDuration: number,
}

const contextSettings = {
  showDropdownButton: false,
  showPresetsButton: false,
  showAddToQuickListButton: false,
  ignoreLimitDurationInTimeRangeDropdown: true,
};

const TimeRangePresetFormItem = ({ idx, id, timerange, description, onChange, onRemove, limitDuration }: ItemProps) => {
  const handleOnChangeRange = useCallback((newTimerange: TimeRange) => {
    onChange({ timerange: newTimerange, description, id }, idx);
  }, [description, id, idx, onChange]);

  const handleOnChangeDescription = useCallback((newDescription: string) => {
    onChange({ timerange, description: newDescription, id }, idx);
  }, [id, idx, onChange, timerange]);

  const handleOnRemove = useCallback(() => {
    onRemove(idx);
  }, [idx, onRemove]);

  const debounceHandleOnChangeDescription = debounce((value: string) => handleOnChangeDescription(value), 300);

  return (
    <ItemWrapper data-testid={`time-range-preset-${id}`}>
      <TimeRangeFilter onChange={handleOnChangeRange} limitDuration={limitDuration} value={timerange} withinPortal={false} />
      <Description>
        <StyledInput type="text"
                     id={`quick-access-time-range-description-${id}`}
                     placeholder="Add description..."
                     title="Time range preset description"
                     aria-label="Time range preset description"
                     required
                     defaultValue={description}
                     onChange={({ target: { value } }) => debounceHandleOnChangeDescription(value)}
                     formGroupClassName="" />
        <IconWrapper className="input-group-addon" onClick={handleOnRemove} title="Remove preset">
          <Icon name="trash-alt" style={{ cursor: 'pointer' }} />
        </IconWrapper>
      </Description>
    </ItemWrapper>
  );
};

const TimeRangePresetForm = ({ options, onUpdate }: {
  options: Immutable.List<TimeRangePreset>,
  onUpdate: Dispatch<Immutable.List<TimeRangePreset>>
}) => {
  const onChange = useCallback((newPreset: TimeRangePreset, idx: number) => {
    const newState = options.set(idx, newPreset);
    onUpdate(newState);
  }, [onUpdate, options]);

  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const limitDuration = useMemo(() => moment.duration(config?.query_time_range_limit).asSeconds() ?? 0, [config?.query_time_range_limit]);

  const onRemove = useCallback((idx: number) => {
    const newState = options.delete(idx);
    onUpdate(newState);
  }, [onUpdate, options]);

  const onMoveItem = useCallback((items: Array<TimeRangePreset>) => {
    onUpdate(Immutable.List(items));
  }, [onUpdate]);

  const addTimeRange = useCallback(() => {
    onUpdate(options.push({
      id: generateId(),
      description: '',
      timerange: { type: 'relative', from: 300 },
    }));
  }, [onUpdate, options]);

  const customContentRender = useCallback(({ item: { id, description, timerange }, index }) => (
    <TimeRangePresetFormItem id={id}
                             onRemove={onRemove}
                             idx={index}
                             onChange={onChange}
                             timerange={timerange}
                             description={description}
                             limitDuration={limitDuration} />
  ), [limitDuration, onChange, onRemove]);

  return (
    <div className="form-group">
      <strong>Search Time Range Presets</strong>
      <span className="help-block">
        <span>Configure the available search time range presets.</span>
      </span>
      <div className="wrapper">
        <TimeRangeInputSettingsContext.Provider value={contextSettings}>
          <SortableList items={options.toArray()}
                        onMoveItem={onMoveItem}
                        displayOverlayInPortal
                        alignItemContent="center"
                        customContentRender={customContentRender} />
        </TimeRangeInputSettingsContext.Provider>
      </div>
      <Button bsSize="xs" onClick={addTimeRange} title="Add new search time range preset" aria-label="Add new search time range preset">
        Add option
      </Button>
    </div>
  );
};

export default TimeRangePresetForm;
