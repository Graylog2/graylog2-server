// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, FormGroup } from 'components/graylog';

type Props = {
  enabled: boolean,
  onChange: () => void,
};

const EventListConfiguration = ({ enabled, onChange }: Props) => {
  return (
    <form>
      <FormGroup>
        <Checkbox onChange={onChange} value={enabled}>Enable Event Annotation</Checkbox>
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
