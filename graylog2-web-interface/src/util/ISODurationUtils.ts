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
import 'moment-duration-format';

const ISODurationUtils = {
  isValidDuration<T>(duration: string, validator: (milliseconds: number, duration: string) => T) {
    return validator(moment.duration(duration).asMilliseconds(), duration);
  },

  durationStyle(duration: string, validator: (milliseconds: number, duration: string) => boolean, errorClass: string = 'error') {
    return this.isValidDuration(duration, validator) ? null : errorClass;
  },

  formatDuration(duration: string, validator: (milliseconds: number, duration: string) => boolean, errorText: string = 'error') {
    return this.isValidDuration(duration, validator) ? moment.duration(duration).format() : errorText;
  },

  humanizeDuration(duration: string, validator: (milliseconds: number, duration: string) => boolean, errorText: string = 'error') {
    return this.isValidDuration(duration, validator) ? moment.duration(duration).humanize() : errorText;
  },
};

export default ISODurationUtils;
