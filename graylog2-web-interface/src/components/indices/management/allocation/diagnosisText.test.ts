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
import { avoidText, causeText, commandWarning, headline, optionText } from './diagnosisText';
import { dataOn } from './format';

import type { AllocationDiagnosis, DiagnosisAction, ShardExplanation, Situation } from '../types';

const diagnosis = (overrides: Partial<AllocationDiagnosis> = {}): AllocationDiagnosis => ({
  situation: 'UNKNOWN',
  needs_action: true,
  copies: [],
  failed_on_node: null,
  left_node: null,
  damaged_file: null,
  cause: null,
  blocking: [],
  remaining_delay_ms: null,
  options: [],
  avoid: [],
  ...overrides,
});

const primary = { index: 'graylog_17', shard: 0, primary: true, totalShards: 4 };

// As the server answers for graylog_17 on dev (translog.ckp torn, five failed attempts).
const translogDamaged = diagnosis({
  situation: 'TRANSLOG_DAMAGED',
  copies: [{ node: 'os-dev-node-0', state: 'IN_SYNC', problem: null }],
  failed_on_node: 'os-dev-node-0',
  damaged_file: 'translog.ckp',
  cause: 'TranslogCorruptedException',
  options: [
    {
      action: 'SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY',
      data_loss: 'UNFLUSHED_OPERATIONS',
      where: 'HOST_ACCESS',
      node: 'os-dev-node-0',
      command: 'bin/opensearch-shard remove-corrupted-data --index graylog_17 --shard-id 0',
    },
    { action: 'RESTORE_SNAPSHOT', data_loss: 'WRITES_SINCE_SNAPSHOT', where: 'OPENSEARCH_API', node: null, command: null },
    { action: 'ALLOCATE_EMPTY_PRIMARY', data_loss: 'WHOLE_SHARD', where: 'OPENSEARCH_API', node: 'os-dev-node-0', command: '{}' },
    { action: 'DELETE_INDEX', data_loss: 'WHOLE_INDEX', where: 'GRAYLOG', node: null, command: null },
  ],
  avoid: ['RETRY_FAILED', 'ALLOCATE_STALE_PRIMARY'],
});

describe('headline', () => {
  it('says what happened, where, and that saved documents are intact', () => {
    expect(headline(translogDamaged, primary)).toBe(
      'Shard 0 of 4 of graylog_17 on os-dev-node-0 has a damaged transaction log (translog.ckp). The documents already saved are intact; only the most recent operations, not yet saved, are at risk.',
    );
  });

  it('keeps the "must be restored or deleted" message for the one case without a repair', () => {
    const text = headline(
      diagnosis({
        situation: 'COMMIT_UNREADABLE',
        copies: [{ node: 'os-dev-node-0', state: 'DAMAGED', problem: 'corrupt_index_exception: ...' }],
        damaged_file: 'segments_4',
      }),
      { index: 'graylog_19', shard: 0, primary: true, totalShards: 1 },
    );

    expect(text).toBe(
      "Shard 0 of graylog_19 on os-dev-node-0 is damaged and can't be repaired: its list of data files (segments_4) can't be read, and no other copy exists.",
    );
  });

  it('names replicas and counts down a delay', () => {
    expect(
      headline(diagnosis({ situation: 'DELAYED_NODE_LEFT', needs_action: false, remaining_delay_ms: 59622 }), {
        index: 'logs',
        shard: 2,
        primary: false,
      }),
    ).toBe(
      "A replica of shard 2 of logs is waiting 60 seconds for its node to come back. If it doesn't, OpenSearch places the copy on another node.",
    );
  });

  it('explains environmental failures in words', () => {
    expect(
      headline(diagnosis({ situation: 'TRANSIENT_FAILURE', failed_on_node: 'node-1', cause: 'No space left on device' }), {
        index: 'logs',
        shard: 0,
        primary: true,
      }),
    ).toBe("Shard 0 of logs couldn't start on node-1 because its disk is full. Its data isn't damaged.");
    expect(causeText('SomethingNewException')).toBe('SomethingNewException');
    expect(causeText(null)).toBe('an unknown problem');
  });

  it('has its own sentence for every known situation', () => {
    const fallback = headline(diagnosis(), primary);
    const situations: Array<Situation> = [
      'INITIALIZING', 'DELAYED_NODE_LEFT', 'FETCHING_SHARD_DATA', 'THROTTLED', 'REPLICA_REBUILDS_FROM_PRIMARY',
      'PRIMARY_NOT_ACTIVE', 'TRANSIENT_FAILURE', 'RESTORE_FAILED', 'TRANSLOG_DAMAGED', 'RETENTION_LEASES_DAMAGED',
      'SEGMENT_DATA_DAMAGED', 'COMMIT_UNREADABLE', 'DAMAGED_OTHER', 'STALE_COPY_ONLY', 'NO_COPY_FOUND',
      'TOO_FEW_NODES', 'ALLOCATION_FILTER', 'SHARDS_PER_NODE_LIMIT', 'ALLOCATION_DISABLED', 'DISK_WATERMARK',
      'AWARENESS', 'NODE_VERSION', 'RETRY_LIMIT',
    ];

    situations.forEach((situation) => {
      expect(headline(diagnosis({ situation }), primary)).not.toBe(fallback);
    });
    expect(fallback).toContain("OpenSearch doesn't say clearly why");
  });
});

describe('optionText', () => {
  it('describes each option with what it loses and who can do it', () => {
    expect(translogDamaged.options.map((option) => optionText(option, primary))).toEqual([
      {
        title:
          'Repair the copy on os-dev-node-0: stop OpenSearch on that node, run the shard repair tool, start the node again, then start the repaired copy.',
        dataLoss: 'Loses only operations not yet saved to disk, usually the last moments before the failure.',
        where: 'Needs access to the OpenSearch server',
      },
      {
        title: 'Restore the index from a snapshot, if you have one.',
        dataLoss: 'Loses what was written after the snapshot.',
        where: 'OpenSearch API request',
      },
      {
        title: 'Start shard 0 empty and keep the rest of the index.',
        dataLoss: 'Loses every document in this shard.',
        where: 'OpenSearch API request',
      },
      { title: 'Delete the index (on this page).', dataLoss: 'Loses the whole index.', where: 'In Graylog' },
    ]);
  });

  it('has text for every action and no data-loss note for waiting', () => {
    const actions: Array<DiagnosisAction> = [
      'WAIT', 'RETRY_FAILED', 'FIX_ENVIRONMENT_THEN_RETRY', 'FIX_PRIMARY', 'RESTORE_AGAIN',
      'SHARD_TOOL_THEN_ALLOCATE_STALE_PRIMARY', 'ALLOCATE_STALE_PRIMARY', 'RESTORE_SNAPSHOT', 'ALLOCATE_EMPTY_PRIMARY',
      'DELETE_INDEX', 'BRING_NODE_BACK', 'ADD_NODES', 'LOWER_REPLICAS', 'CHANGE_ALLOCATION_FILTER', 'RAISE_SHARD_LIMIT',
      'ENABLE_ALLOCATION', 'FREE_DISK_SPACE', 'FIX_AWARENESS', 'FINISH_UPGRADE',
    ];

    actions.forEach((action) => {
      const text = optionText({ action, data_loss: 'NONE', where: 'AUTOMATIC', node: null, command: null }, primary);
      expect(text.title).not.toBe(action);
      expect(text.where).toBeNull();
    });
    expect(
      optionText({ action: 'WAIT', data_loss: 'NONE', where: 'AUTOMATIC', node: null, command: null }, primary).dataLoss,
    ).toBeNull();
  });
});

describe('commandWarning', () => {
  it('warns above every ready-made command that loses data, and says how much', () => {
    const withCommand = translogDamaged.options.filter((option) => option.command);

    expect(withCommand.length).toBeGreaterThan(0);
    withCommand.forEach((option) => expect(commandWarning(option)).toMatch(/^Running this can't be undone\. Loses /));
  });

  it('does not warn when the command keeps all documents or there is no command', () => {
    const [first] = translogDamaged.options;

    expect(commandWarning({ ...first, data_loss: 'NONE' })).toBeNull();
    expect(commandWarning({ ...first, command: null })).toBeNull();
  });
});

describe('avoidText', () => {
  it('warns against starting a damaged copy before repairing it', () => {
    expect(avoidText('ALLOCATE_STALE_PRIMARY')).toContain("Don't start this copy before it's repaired");
    expect(avoidText('DELETE_INDEX')).toBeNull();
  });
});

describe('dataOn', () => {
  const shard = (overrides: Partial<ShardExplanation>): ShardExplanation => ({
    index: 'logs',
    shard: 0,
    primary: true,
    current_state: 'unassigned',
    unassigned_reason: 'ALLOCATION_FAILED',
    unassigned_since: null,
    failed_attempts: null,
    root_cause: null,
    details: null,
    can_allocate: 'no',
    explanation: null,
    max_retries_exceeded: false,
    nodes: [],
    diagnosis: null,
    error: null,
    ...overrides,
  });

  it('lists the copies a primary still has, else where it failed', () => {
    expect(
      dataOn(
        shard({
          diagnosis: diagnosis({
            copies: [
              { node: 'node-1', state: 'STALE', problem: null },
              { node: 'node-2', state: 'DAMAGED', problem: 'x' },
            ],
          }),
        }),
      ),
    ).toBe('node-1 (older copy), node-2 (damaged copy)');
    // OpenSearch reports graylog_17's copy as in sync; the damage only showed while starting it.
    expect(dataOn(shard({ diagnosis: translogDamaged }))).toBe('os-dev-node-0 (damaged copy)');
    expect(dataOn(shard({ diagnosis: diagnosis({ failed_on_node: 'node-3' }) }))).toBe('node-3 (failed there)');
    expect(dataOn(shard({ can_allocate: 'no_valid_shard_copy', diagnosis: diagnosis() }))).toBe('no copy on any node');
    expect(dataOn(shard({}))).toBe('');
  });
});
