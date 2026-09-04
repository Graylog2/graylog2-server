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
import React, { useState, useEffect } from 'react';

import { Input } from 'components/bootstrap';
import * as ISODurationUtils from 'util/ISODurationUtils';

/**
 * Displays an `Input` component for ISO8601 durations.
 */
type Props = {
  id: string;
  duration: string;
  update: (newDuration: string) => void;
  label?: string;
  help?: string;
  validator?: (newDuration: number) => boolean;
  errorText?: string;
  autoFocus?: boolean;
  required?: boolean;
  disabled?: boolean;
};

const ISODurationInput = ({
  id,
  duration: propDuration,
  update,
  label = 'Duration',
  help = 'as ISO8601 Duration',
  validator = () => true,
  errorText = 'invalid',
  autoFocus = false,
  required = false,
  disabled = false,
}: Props) => {
  const [duration, setDuration] = useState(propDuration);

  useEffect(() => {
    setDuration(propDuration);
  }, [propDuration]);

  const handleChange = (event: React.ChangeEvent) => {
    let validDuration = (event.target as any).value;

    if (!validDuration.startsWith('P')) {
      validDuration = `P${validDuration}`;
    }

    setDuration(validDuration);

    if (ISODurationUtils.isValidDuration(validDuration, validator)) {
      // Only propagate state if the config is valid.
      update(validDuration);
    }
  };

  return (
    <Input
      id={id}
      type="text"
      label={label}
      onChange={handleChange}
      value={duration}
      help={help}
      addonAfter={ISODurationUtils.humanizeDuration(duration, validator, errorText)}
      bsStyle={ISODurationUtils.durationStyle(duration, validator)}
      autoFocus={autoFocus}
      required={required}
      disabled={disabled}
    />
  );
};

export default ISODurationInput;
