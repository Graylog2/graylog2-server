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
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';
import generateId from 'logic/generateId';

export type QuickAccessTimeRange = {
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
  onChange: (timerange: QuickAccessTimeRange, idx: number) => void,
  onRemove: (idx: number) => void,
  limitDuration: number,
}

const contextSettings = {
  showDropdownButton: false,
  showRelativePresetsButton: false,
  showAbsolutePresetsButton: false,
  showKeywordPresetsButton: false,
  showAddToQuickListButton: false,
  ignoreLimitDurationInTimeRangeDropdown: true,
};

const QuickAccessTimeRangeFormItem = ({ idx, id, timerange, description, onChange, onRemove, limitDuration }: ItemProps) => {
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
      <TimeRangeInput onChange={handleOnChangeRange} limitDuration={limitDuration} value={timerange} />
      <Description>
        <StyledInput type="text"
                     id={`quick-access-time-range-description-${id}`}
                     placeholder="Add description..."
                     title="Time range preset description"
                     aria-label="Time range preset description"
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

const QuickAccessTimeRangeForm = ({ options, onUpdate }: {
  options: Immutable.List<QuickAccessTimeRange>,
  onUpdate: Dispatch<Immutable.List<QuickAccessTimeRange>>
}) => {
  const onChange = useCallback((newPreset: QuickAccessTimeRange, idx: number) => {
    const newState = options.set(idx, newPreset);
    onUpdate(newState);
  }, [onUpdate, options]);

  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const limitDuration = useMemo(() => moment.duration(config?.query_time_range_limit).asSeconds() ?? 0, [config?.query_time_range_limit]);

  const onRemove = useCallback((idx: number) => {
    const newState = options.delete(idx);
    onUpdate(newState);
  }, [onUpdate, options]);

  const onMoveItem = useCallback((items: Array<QuickAccessTimeRange>) => {
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
    <QuickAccessTimeRangeFormItem id={id}
                                  onRemove={onRemove}
                                  idx={index}
                                  onChange={onChange}
                                  timerange={timerange}
                                  description={description}
                                  limitDuration={limitDuration} />
  ), [limitDuration, onChange, onRemove]);

  return (
    <div className="form-group">
      <strong>Quick Access Time Range Options</strong>
      <span className="help-block">
        <span>Configure the available options for the <strong>quick access</strong> time range selector</span>
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
      <Button bsSize="xs" onClick={addTimeRange} title="Add quick access timerange" aria-label="Add quick access timerange">
        Add option
      </Button>
    </div>
  );
};

export default QuickAccessTimeRangeForm;
