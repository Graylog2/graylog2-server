// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled, { type StyledComponent } from 'styled-components';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';

import TimerangeSelector from './TimerangeSelector';
import DateInputWithPicker from './DateInputWithPicker';

type Props = {
  disabled: boolean,
};

const StyledTimerangeSelector: StyledComponent<{}, void, *> = styled(TimerangeSelector)`
  display: flex;
`;

const InputWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  width: 200px;
`;

const Separator: StyledComponent<{}, void, HTMLParagraphElement> = styled.p`
  margin: 0;
  line-height: 34px;
  font-size: 18px;
  padding-left: 15px;
  padding-right: 15px;
`;

const _isValidDateString = (dateString: string) => {
  if (dateString === undefined) {
    return undefined;
  }
  return DateTime.isValidDateString(dateString)
    ? undefined
    : `Invalid date: ${dateString}`;
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
