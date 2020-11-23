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
import * as Immutable from 'immutable';

import View, { ViewJson } from 'views/logic/views/View';
import Search from 'views/logic/search/Search';

const simpleView = (): View => View.builder()
  .id('foo')
  .type(View.Type.Dashboard)
  .title('Foo')
  .summary('summary')
  .description('Foo')
  .search(Search.create().toBuilder().id('foosearch').build())
  .properties(Immutable.List())
  .state(Immutable.Map())
  .createdAt(new Date())
  .owner('admin')
  .requires({})
  .build();

const simpleViewJson = () => ({ ...simpleView().toJSON(), requires: {} }) as unknown as ViewJson;

export {
  simpleView,
  simpleViewJson,
};
