// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, FormGroup } from 'components/graylog';

type Props = {
  enabled: boolean,
  onChange: (value: boolean) => void,
};

const EventListConfiguration = ({ enabled, onChange }: Props) => {
  return (
    <form>
      <FormGroup>
        {/* eslint-disable-next-line no-undef */ /* $FlowFixMe: checked is part of target */}
        <Checkbox onChange={(event: SyntheticEvent<HTMLInputElement>) => onChange(event.target.checked)} checked={enabled}>
          Enable Event Annotation
        </Checkbox>
      </FormGroup>
    </form>
  );
};

EventListConfiguration.propTypes = {
  enabled: PropTypes.bool,
  onChange: PropTypes.func,
};

EventListConfiguration.defaultProps = {
  enabled: false,
  onChange: () => {},
};

export default EventListConfiguration;
