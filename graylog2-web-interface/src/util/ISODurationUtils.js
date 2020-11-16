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
import moment from 'moment';
import {} from 'moment-duration-format';

const ISODurationUtils = {
  isValidDuration(duration, validator) {
    return validator(moment.duration(duration).asMilliseconds(), duration);
  },

  durationStyle(duration, validator, errorClass) {
    let className = errorClass;

    if (!className) {
      className = 'error';
    }

    return this.isValidDuration(duration, validator) ? null : className;
  },

  formatDuration(duration, validator, errorText) {
    let text = errorText;

    if (!text) {
      text = 'error';
    }

    return this.isValidDuration(duration, validator) ? moment.duration(duration).format() : text;
  },

  humanizeDuration(duration, validator, errorText) {
    let text = errorText;

    if (!text) {
      text = 'error';
    }

    return this.isValidDuration(duration, validator) ? moment.duration(duration).humanize() : text;
  },
};

export default ISODurationUtils;
