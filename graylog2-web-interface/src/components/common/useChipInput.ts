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

// Unit-separator character. Callers restrict values to characters that can't include it, so it is
// safe as the delimiter `Select` uses to (de)serialize a multi value.
export const VALUE_DELIMITER = '\x1F';

type Options = {
  /** The committed values, in order. */
  values: ReadonlyArray<string>;
  onChange: (next: string[]) => void;
  /** Applied to typed text and to committed values before anything else looks at them. */
  normalize: (raw: string) => string;
  /** Why this value can't be added, or null when it is acceptable. */
  validate: (candidate: string) => string | null;
  /**
   * Why the text typed so far can't be added, or null to say nothing yet. Defaults to
   * `validate`. Override where the typed text isn't always a candidate value, such as a field
   * whose input doubles as a search over the options.
   */
  validateInput?: (candidate: string, raw: string) => string | null;
  max: number;
  /** Describes a set of already-committed values that don't pass `validate`. */
  describeInvalid: (invalid: ReadonlyArray<string>) => string | null;
  describeDuplicate: (candidate: string) => string;
  /**
   * Withhold the create affordance for a reason that isn't a validation failure, such as the
   * value already existing as a selectable option. Unlike `validate`, this produces no message.
   */
  suppressCreate?: (raw: string, candidate: string) => boolean;
  /**
   * Notified whenever the typed text changes, including when it is cleared on commit. Lets a
   * caller drive option loading without reading `input` back out of the hook, which would order
   * the options after the hook call that needs them.
   */
  onInputChanged?: (value: string) => void;
};

/**
 * Shared behavior for a `Select` used as a chip/token input, where a value the server would
 * reject must never become a chip.
 *
 * A rejected value stays in the input with a message naming the reason, rather than being
 * committed and flagged afterwards, and the menu is held shut while it is rejected so it can't
 * cover that message.
 */
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
  // Duplicates are only wrong at commit time — typing "auth" on the way to "authentication"
  // shouldn't read as an error — so they're flagged on Enter/Tab/blur rather than live.
  const [duplicateAttempt, setDuplicateAttempt] = useState<string | null>(null);

  const candidate = normalize(input);
  const isAtMax = values.length >= max;
  const inputMessage = candidate ? (validateInput ?? validate)(candidate, input) : null;

  const handleChange = (joined: string) => {
    const raw = joined ? joined.split(VALUE_DELIMITER) : [];
    const normalized = raw.map(normalize).filter((value) => value.length > 0);

    onChange(Array.from(new Set(normalized)).slice(0, max));
    // A value was just committed, so the typed text is spent.
    setInput('');
    onInputChanged?.('');
    setDuplicateAttempt(null);
  };

  const handleInputChange = (value: string, actionMeta?: InputActionMeta) => {
    if (actionMeta?.action !== 'input-change') return;

    setInput(value);
    onInputChanged?.(value);
    // The user is editing, so any duplicate warning is stale. It re-evaluates on the next
    // commit attempt.
    setDuplicateAttempt(null);
  };

  // react-select rejects a duplicate with no chip and no callback, so it has to be flagged here
  // or the value would simply vanish with no explanation.
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

  // Values that fail `validate` can no longer be committed, so this only catches values that
  // reached the form another way, such as an entity created through the API.
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
    /** Spread onto `Select`. Anything here can be overridden by the caller. */
    selectProps: {
      delimiter: VALUE_DELIMITER,
      value: values.join(VALUE_DELIMITER),
      inputValue: input,
      onChange: handleChange,
      onInputChange: handleInputChange,
      onBlur: flagDuplicateAttempt,
    },
    /**
     * Spread onto `Select` through a cast: `Select`'s Props type doesn't surface these, but it
     * passes unknown props through to react-select.
     */
    reactSelectProps: {
      isValidNewOption,
      // An open menu covers the message saying why the value was rejected.
      menuIsOpen: inputMessage ? false : undefined,
    } as object,
  };
};

export default useChipInput;
export type { Options as UseChipInputOptions };
