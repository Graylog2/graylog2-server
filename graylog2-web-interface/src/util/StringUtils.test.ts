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

import StringUtils from 'util/StringUtils';

describe('capitalizeFirstLetter', () => {
  it('capitalizes the first letter of a string supplied', () => {
    const testString = 'capitalizeMe';
    const expectedOutcome = 'CapitalizeMe';

    const newString = StringUtils.capitalizeFirstLetter(testString);

    expect(newString).toBe(expectedOutcome);
  });
});

describe('HTML escape unescape', () => {
  const unescapedString = 'Hello<enter-name> & enjoy';
  const escapedString = 'Hello&lt;enter-name&gt; &amp; enjoy';

  it('escapes a string for HTML use', () => {
    const result = StringUtils.escapeHTML(unescapedString);

    expect(result).toBe(escapedString);
  });

  it('unescapes a string used in HTML', () => {
    const result = StringUtils.unescapeHTML(escapedString);

    expect(result).toBe(unescapedString);
  });
});

describe('pluralize', () => {
  it('pluralizes a string', () => {
    const singular = 'racecar';
    const plural = 'racecars';
    const resultSingular = StringUtils.pluralize(1, singular, plural);

    expect(resultSingular).toBe(singular);

    const resultPlural = StringUtils.pluralize(2, singular, plural);

    expect(resultPlural).toBe(plural);
  });
});

describe('stringify', () => {
  const testObj = {
    fruitName: 'Apple',
    brixRating: 7,
    color: 'red',
  };
  const stringifiedObj = '{"fruitName":"Apple","brixRating":7,"color":"red"}';

  it('stringifies an object', () => {
    const result = StringUtils.stringify(testObj);

    expect(result).toBe(stringifiedObj);
  });

  it('returns anything else as a string', () => {
    const testNum = 7;
    const testArray = [1, 2, 3];

    const numResult = StringUtils.stringify(testNum);

    expect(numResult).toBe('7');

    const arrayResult = StringUtils.stringify(testArray);

    expect(arrayResult).toBe('[1,2,3]');
  });

  it('returns an empty string if String() would return an error', () => {
    const canNotStringMe = undefined;
    const result = StringUtils.stringify(canNotStringMe);

    expect(result).toBe('undefined');
  });
});

describe('replaceSpaces', () => {
  it('replaces spaces in a string with "-"', () => {
    const makeHyphenized = 'Rubber baby bunky bumpers';
    const hyphenized = 'Rubber-baby-bunky-bumpers';
    const result = StringUtils.replaceSpaces(makeHyphenized);

    expect(result).toBe(hyphenized);
  });

  it('replaces spaces in a string with a supplied character', () => {
    const makePipified = 'Rubber baby bunky bumpers';
    const pipified = 'Rubber|baby|bunky|bumpers';
    const result = StringUtils.replaceSpaces(makePipified, '|');

    expect(result).toBe(pipified);
  });
});

describe('toTitleCase', () => {
  it('title cases a string and defaults the split to " "', () => {
    const makeTitleCase = 'Rubber baby bunky bumpers.';
    const titleCased = 'Rubber Baby Bunky Bumpers.';
    const result = StringUtils.toTitleCase(makeTitleCase);

    expect(result).toBe(titleCased);
  });

  it('titles cases a string based on supplied split character', () => {
    const makeTitleCase = 'Rubber-baby-bunky-bumpers.';
    const titleCased = 'Rubber Baby Bunky Bumpers.';
    const result = StringUtils.toTitleCase(makeTitleCase, '-');

    expect(result).toBe(titleCased);
  });
});

describe('truncateWithEllipses', () => {
  it('truncates a string with a default length of 10 and ending string of "..."', () => {
    const makeShorter = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent nec.';
    const truncated = 'Lorem ipsu...';
    const result = StringUtils.truncateWithEllipses(makeShorter);

    expect(result).toBe(truncated);
  });

  it('truncates a string with supplied length and default ending string of "..."', () => {
    const makeShorter = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent nec.';
    const truncated = 'Lorem ipsum dol...';
    const result = StringUtils.truncateWithEllipses(makeShorter, 15);

    expect(result).toBe(truncated);
  });

  it('truncates a string with supplied length and supplied ending string', () => {
    const makeShorter = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent nec.';
    const truncated = 'Lorem ipsum dol|||';
    const result = StringUtils.truncateWithEllipses(makeShorter, 15, '|||');

    expect(result).toBe(truncated);
  });
});
