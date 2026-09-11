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
import { useState } from 'react';
import type { InputActionMeta } from 'react-select';

// Unit separator; callers restrict values to characters that exclude it.
export const VALUE_DELIMITER = '\x1F';

type Options = {
  values: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  normalize: (raw: string) => string;
  /** Why this value can't be added, or null when it is acceptable. */
  validate: (candidate: string) => string | null;
  /** Same, for text still being typed. Defaults to `validate`. */
  validateInput?: (candidate: string, raw: string) => string | null;
  max: number;
  describeInvalid: (invalid: ReadonlyArray<string>) => string | null;
  describeDuplicate: (candidate: string) => string;
  /** Withhold the create affordance without producing a message. */
  suppressCreate?: (raw: string, candidate: string) => boolean;
  /** Lets a caller load options from the typed text without ordering them after this hook. */
  onInputChanged?: (value: string) => void;
};

/** Shared behavior for a `Select` used as a chip input, where an invalid value never becomes a chip. */
const useChipInput = ({
  values,
  onChange,
  normalize,
  validate,
  validateInput,
  max,
  describeInvalid,
  describeDuplicate,
  suppressCreate,
  onInputChanged,
}: Options) => {
  const [input, setInput] = useState('');
  // Flagged on commit, not live: typing "auth" toward "authentication" shouldn't read as an error.
  const [duplicateAttempt, setDuplicateAttempt] = useState<string | null>(null);

  const candidate = normalize(input);
  const isAtMax = values.length >= max;
  const inputMessage = candidate ? (validateInput ?? validate)(candidate, input) : null;

  const handleChange = (joined: string) => {
    const raw = joined ? joined.split(VALUE_DELIMITER) : [];
    const normalized = raw.map(normalize).filter((value) => value.length > 0);

    onChange(Array.from(new Set(normalized)).slice(0, max));
    setInput('');
    onInputChanged?.('');
    setDuplicateAttempt(null);
  };

  const handleInputChange = (value: string, actionMeta?: InputActionMeta) => {
    if (actionMeta?.action !== 'input-change') return;

    setInput(value);
    onInputChanged?.(value);
    setDuplicateAttempt(null);
  };

  // react-select rejects a duplicate with no chip and no callback, so it would vanish unexplained.
  const flagDuplicateAttempt = () => {
    if (candidate && values.includes(candidate)) {
      setDuplicateAttempt(candidate);
    }
  };

  const isValidNewOption = (raw: string) => {
    const value = normalize(raw ?? '');

    return (
      value.length > 0 &&
      validate(value) === null &&
      !isAtMax &&
      // Replaces react-select's own duplicate check, which this overrides.
      !values.includes(value) &&
      !suppressCreate?.(raw, value)
    );
  };

  // Only catches values that reached the form another way, such as through the API.
  const invalidValues = values.filter((value) => validate(value) !== null);

  const messages = [
    describeInvalid(invalidValues),
    inputMessage,
    duplicateAttempt ? describeDuplicate(duplicateAttempt) : null,
  ].filter((message): message is string => !!message);

  return {
    input,
    candidate,
    isAtMax,
    messages,
    setDuplicateAttempt,
    flagDuplicateAttempt,
    /** Spread onto `Select`. */
    selectProps: {
      delimiter: VALUE_DELIMITER,
      value: values.join(VALUE_DELIMITER),
      inputValue: input,
      onChange: handleChange,
      onInputChange: handleInputChange,
      onBlur: flagDuplicateAttempt,
    },
    /** Spread onto `Select` too; cast because its Props type doesn't surface these. */
    reactSelectProps: {
      isValidNewOption,
      // An open menu covers the message saying why the value was rejected.
      menuIsOpen: inputMessage ? false : undefined,
    } as object,
  };
};

export default useChipInput;
export type { Options as UseChipInputOptions };
