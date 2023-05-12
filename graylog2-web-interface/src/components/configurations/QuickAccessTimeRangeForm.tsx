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
import React, { useCallback } from 'react';
import { Formik, Form } from 'formik';
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
}

const contextSettings = {
  showDropdownButton: false,
  showRelativePresetsButton: false,
};

const QuickAccessTimeRangeFormItem = ({ idx, id, timerange, description, onChange, onRemove }: ItemProps) => {
  const handleOnChangeRange = useCallback((newTimerange) => {
    onChange({ timerange: newTimerange, description, id }, idx);
  }, [description, id, idx, onChange]);

  const handleOnChangeDescription = useCallback((newDescription) => {
    onChange({ timerange, description: newDescription, id }, idx);
  }, [id, idx, onChange, timerange]);

  const handleOnRemove = useCallback(() => {
    onRemove(idx);
  }, [idx, onRemove]);

  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  const debounceHandleOnChangeDescription = debounce((value: string) => handleOnChangeDescription(value), 1000);

  return (
    <ItemWrapper>
      <TimeRangeInput onChange={handleOnChangeRange} limitDuration={limitDuration} value={timerange} />
      <Description>
        <StyledInput type="text"
                     placeholder="Add description..."
                     defaultValue={description}
                     onChange={({ target: { value } }) => debounceHandleOnChangeDescription(value)}
                     formGroupClassName="" />
        <IconWrapper className="input-group-addon">
          <Icon name="trash-alt" style={{ cursor: 'pointer' }} onClick={handleOnRemove} />
        </IconWrapper>
      </Description>
    </ItemWrapper>
  );
};

const QuickAccessTimeRangeForm = ({ options, onUpdate }: {
  options: Immutable.List<QuickAccessTimeRange>,
  onUpdate: Dispatch<Immutable.List<QuickAccessTimeRange>>
}) => {
  const onChange = useCallback((newPreset, idx) => {
    const newState = options.set(idx, newPreset);
    onUpdate(newState);
  }, [onUpdate, options]);

  const onRemove = useCallback((idx) => {
    const newState = options.delete(idx);
    onUpdate(newState);
  }, [onUpdate, options]);

  const onMoveItem = useCallback((items) => {
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
    <QuickAccessTimeRangeFormItem id={id} onRemove={onRemove} idx={index} onChange={onChange} timerange={timerange} description={description} />
  ), [onChange, onRemove]);

  return (
    <div className="form-group">
      <strong>Quick Access Timerange Options</strong>
      <span className="help-block">
        <span>Configure the available options for the <strong>quick access</strong> time range selector</span>
      </span>
      <div className="wrapper">
        <TimeRangeInputSettingsContext.Provider value={contextSettings}>
          <Formik initialValues={{}} onSubmit={() => {}}>
            <Form>
              <SortableList items={options.toArray()}
                            onMoveItem={onMoveItem}
                            displayOverlayInPortal
                            alignItemContent="center"
                            customContentRender={customContentRender} />
            </Form>
          </Formik>
        </TimeRangeInputSettingsContext.Provider>
      </div>
      <Button bsSize="xs" onClick={addTimeRange}>Add</Button>
    </div>
  );
};

export default QuickAccessTimeRangeForm;
