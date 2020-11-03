// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Checkbox } from 'components/graylog';

type Props = {
  enabled: boolean,
  onChange: (value: boolean) => void,
};

const EventListConfiguration = ({ enabled, onChange }: Props) => {
  return (
    <Checkbox onChange={(event: SyntheticInputEvent<HTMLInputElement>) => onChange(event.target.checked)}
              checked={enabled}>
      Enable Event Annotation
    </Checkbox>
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
