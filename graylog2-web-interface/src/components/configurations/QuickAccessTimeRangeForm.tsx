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
import { Formik, Form, Field } from 'formik';
import moment from 'moment/moment';
import styled from 'styled-components';

import { Button, Input, Row } from 'components/bootstrap';
import { Icon, Select } from 'components/common';
import type { TimeRange } from 'views/logic/queries/Query';
import type { TimeRangeType } from 'views/components/searchbar/date-time-picker/TimeRangeDropdown';
import TabRelativeTimeRange from 'views/components/searchbar/date-time-picker/TabRelativeTimeRange';
import RelativeRangeSelect from 'views/components/searchbar/date-time-picker/RelativeRangeSelect';
import {
  classifyFromRange,
  RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
} from 'views/components/searchbar/date-time-picker/RelativeTimeRangeClassifiedHelper';
import { DEFAULT_RELATIVE_FROM } from 'views/Constants';
import { useStore } from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';

export type QuickAccessTimeRange = {
  timerange: TimeRange,
  description: string,
}

const ItemWrapper = styled.div`
  display: flex;
  gap: 30px
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

const List = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px
`;

type ItemProps = {
  idx: number,
  timerange: TimeRange,
  description: string,
  onChange: (timerange: QuickAccessTimeRange, idx: number) => void,
  onRemove: (idx: number) => void,
}

const QuickAccessTimeRangeFormItem = ({ idx, timerange, description, onChange, onRemove }: ItemProps) => {
  const handleOnChangeRange = useCallback((newTimerange) => {
    onChange({ timerange: newTimerange, description }, idx);
  }, [description, idx, onChange]);

  const handleOnChangeDescription = useCallback(({ target: { value: newDescription } }) => {
    onChange({ timerange, description: newDescription }, idx);
  }, [onChange, timerange]);

  const handleOnRemove = useCallback(() => {
    onRemove(idx);
  }, [idx, onRemove]);

  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  return (
    <ItemWrapper>
      <TimeRangeInput onChange={handleOnChangeRange} limitDuration={limitDuration} value={timerange} />
      <Description>
        <StyledInput type="text"
                     placeholder="Add description..."
                     value={description}
                     onChange={handleOnChangeDescription}
                     formGroupClassName="" />
        <IconWrapper className="input-group-addon">
          <Icon name="trash-alt" style={{ cursor: 'pointer' }} onClick={handleOnRemove} />
        </IconWrapper>
      </Description>
    </ItemWrapper>
  );
};

const QuickAccessTimeRangeForm = ({ quickAccessTimeRangePresets, setQuickAccessTimeRangePresets }: {
  quickAccessTimeRangePresets: Array<QuickAccessTimeRange>,
  setQuickAccessTimeRangePresets: Dispatch<Array<QuickAccessTimeRange>>
}) => {
  const onChange = useCallback((newPreset, idx) => {
    const newState = [...quickAccessTimeRangePresets];
    newState[idx] = newPreset;
    setQuickAccessTimeRangePresets(newState);
  }, [quickAccessTimeRangePresets, setQuickAccessTimeRangePresets]);

  const onRemove = useCallback((idx) => {
    const newState = [...quickAccessTimeRangePresets].splice(idx, 1);
    setQuickAccessTimeRangePresets(newState);
  }, [quickAccessTimeRangePresets, setQuickAccessTimeRangePresets]);

  return (
    <div className="form-group">
      <strong>Quick Access Timerange Options</strong>
      <span className="help-block">
        <span>Configure the available options for the <strong>quick access</strong> time range selector</span>
      </span>
      <div className="wrapper">
        <TimeRangeInputSettingsContext.Provider value={{
          showDropdownButton: false,
          showRelativePresetsButton: false,
        }}>
          <Formik initialValues={{}} onSubmit={() => {}}>
            <Form>
              <List>
                {
                  quickAccessTimeRangePresets.map(({ timerange, description }, idx) => {
                    return (
                      <QuickAccessTimeRangeFormItem timerange={timerange}
                                                    description={description}
                                                    onChange={onChange}
                                                    idx={idx}
                                                    onRemove={onRemove} />
                    );
                  })
                }
              </List>
            </Form>
          </Formik>
        </TimeRangeInputSettingsContext.Provider>
      </div>
      <Button bsSize="xs" onClick={() => {}}>Add</Button>
    </div>
  );
};

export default QuickAccessTimeRangeForm;
