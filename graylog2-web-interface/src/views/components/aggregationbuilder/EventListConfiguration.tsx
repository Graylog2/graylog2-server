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

import { Checkbox } from 'components/graylog';

type Props = {
  enabled: boolean,
  onChange: (value: boolean) => void,
};

const EventListConfiguration = ({ enabled, onChange }: Props) => {
  return (
    <Checkbox onChange={(event: React.ChangeEvent<HTMLInputElement>) => onChange(event.target.checked)}
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
