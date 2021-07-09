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
import FieldType from 'views/logic/fieldtypes/FieldType';

import RemoveFromTableActionHandler from './RemoveFromTableActionHandler';

import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';

const actionArgs = {
  field: 'foo',
  queryId: 'deadbeef',
  type: FieldType.create('string'),
} as const;

describe('RemoveFromTableActionHandler.condition', () => {
  it('enables action if field is presented in message table', () => {
    const widget = MessagesWidget.builder()
      .config(MessagesWidgetConfig.builder().fields(['foo']).build())
      .build();
    const contexts = { widget };

    const result = RemoveFromTableActionHandler.isEnabled({ ...actionArgs, contexts });

    expect(result).toEqual(true);
  });

  it('enables action if field is presented in message table', () => {
    const widget = MessagesWidget.builder()
      .config(MessagesWidgetConfig.builder().build())
      .build();
    const contexts = { widget };

    const result = RemoveFromTableActionHandler.isEnabled({ ...actionArgs, contexts });

    expect(result).toEqual(false);
  });

  it('checks properly for non message tables', () => {
    const widget = AggregationWidget.builder().build();
    const contexts = { widget };

    const result = RemoveFromTableActionHandler.isEnabled({ ...actionArgs, contexts });

    expect(result).toEqual(false);
  });
});
