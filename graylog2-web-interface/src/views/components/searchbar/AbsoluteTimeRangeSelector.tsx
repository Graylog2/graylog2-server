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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
import type { ThemeInterface } from 'theme';

import TimerangeSelector from './TimerangeSelector';
import DateInputWithPicker from './DateInputWithPicker';

type Props = {
  disabled: boolean,
};

const StyledTimerangeSelector: StyledComponent<{}, void, any> = styled(TimerangeSelector)`
  display: flex;
`;

const InputWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  width: 200px;
`;

const Separator: StyledComponent<{}, ThemeInterface, HTMLParagraphElement> = styled.p(({ theme }) => css`
  margin: 0;
  line-height: 34px;
  font-size: ${theme.fonts.size.large};
  padding-left: 15px;
  padding-right: 15px;
`);

const _isValidDateString = (dateString: string) => {
  if (dateString === undefined) {
    return undefined;
  }

  return DateTime.isValidDateString(dateString)
    ? undefined
    : 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]';
};

const AbsoluteTimeRangeSelector = ({ disabled }: Props) => {
  return (
    <StyledTimerangeSelector className="absolute">
      <Field name="timerange.from" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <InputWrap>
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search start date"
                                 error={error} />
          </InputWrap>
        )}
      </Field>

      <Separator className="text-center">
        <Icon name="long-arrow-alt-right" />
      </Separator>

      <Field name="timerange.to" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <InputWrap>
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search end date"
                                 error={error} />
          </InputWrap>
        )}
      </Field>
    </StyledTimerangeSelector>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
