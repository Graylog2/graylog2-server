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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import RuleBuilderBlock from './RuleBuilderBlock';
import { actionsBlockDict, buildRuleBlock } from './fixtures';
import RuleBuilderProvider from './RuleBuilderProvider';

const block = buildRuleBlock();

describe('RuleBuilderBlock', () => {
  it('renders RuleBlockForm when no block exists', async () => {
    render(<RuleBuilderBlock type="action"
                             blockDict={actionsBlockDict}
                             order={1}
                             addBlock={jest.fn()}
                             updateBlock={jest.fn()}
                             deleteBlock={jest.fn()} />);

    expect(screen.getByText(/Add action/i)).toBeInTheDocument();
  });

  it('renders RuleBlockDisplay when a block exists', async () => {
    render(
      <RuleBuilderProvider>
        <RuleBuilderBlock type="action"
                          blockDict={actionsBlockDict}
                          block={block}
                          order={1}
                          addBlock={jest.fn()}
                          updateBlock={jest.fn()}
                          deleteBlock={jest.fn()} />
      </RuleBuilderProvider>,
    );

    expect(screen.getByText(/to_long "foo"/i)).toBeInTheDocument();
  });
});
