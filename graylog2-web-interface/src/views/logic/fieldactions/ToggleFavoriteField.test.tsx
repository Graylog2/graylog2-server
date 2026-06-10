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
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';

import ToggleFavoriteField from './ToggleFavoriteField';

jest.mock('util/AppConfig', () => ({
  isFeatureEnabled: () => true,
}));
type ActionContexts = Partial<ActionHandlerArguments>['contexts'];

describe('ToggleFavoriteField', () => {
  describe('isHidden', () => {
    it('returns true when favorite fields context is missing', () => {
      const contexts = {} as ActionContexts;
      expect(ToggleFavoriteField.isHidden(true, { contexts })).toBeTruthy();
    });
    it('returns false when favorite fields context is preset', () => {
      const contexts = { favoriteFields: [] } as ActionContexts;
      expect(ToggleFavoriteField.isHidden(true, { contexts })).toBeFalsy();
    });
  });
});
