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
import i18n, { changeLanguage } from 'i18n';

import pseudoLocalize, { PSEUDO_LANGUAGE } from './pseudoLocalize';

describe('pseudoLocalize', () => {
  it('accents letters, keeps other characters and adds length', () => {
    expect(pseudoLocalize('Save 2 items!')).toBe('[Šàṽé 2 îţéɱš!~~~~]');
  });

  it('is applied to translations in the pseudo language only', async () => {
    expect(i18n.t('duration.days', { ns: 'common', count: 2 })).toBe('2 days');

    await changeLanguage(PSEUDO_LANGUAGE);

    expect(i18n.t('duration.days', { ns: 'common', count: 2 })).toBe('[2 ðàýš~~]');

    await changeLanguage('en');
  });
});
