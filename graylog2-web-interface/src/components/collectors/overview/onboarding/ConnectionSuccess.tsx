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

import {Label} from 'components/bootstrap';
import {Icon, Link, RelativeTime, Section, SimpleGrid, Stack} from 'components/common';
import Routes from 'routing/Routes';
import {defaultCompare} from 'logic/DefaultCompare';
import type {CollectorInstanceView} from 'components/collectors/types';
import {useSources} from 'components/collectors/hooks/useSourceQueries';

import useCollectorLogPreview from './useCollectorLogPreview';
import LogPreviewSection from './LogPreviewSection';

import {DetailLabel, DetailRow} from '../../common/DetailRow';
import {IconRow, IconRowList} from '../../common/IconRowList';
import InstanceStatusLabel from '../../common/InstanceStatusLabel';
import collectorOsName from '../../common/collectorOsName';
import collectorReceivedMessagesUrl from '../../common/collectorReceivedMessagesUrl';
import collectorSystemLogsUrl from '../../common/collectorSystemLogsUrl';
import {COLLECTOR_INSTANCE_UID_FIELD} from '../../common/fields';
import {SOURCE_TYPE_LABELS} from '../../sources/Constants';

type Props = {
  instance: CollectorInstanceView;
  fleetName: string | undefined;
};

const ConnectionSuccess = ({instance, fleetName}: Props) => {
  const {selfLogs, sourceLogs, selfLogsError, sourceLogsError, isLoading} = useCollectorLogPreview(
    instance.instance_uid,
  );
  const {data: sources} = useSources(instance.fleet_id);

  const attributes = [
    ...Object.entries(instance.identifying_attributes ?? {}),
    ...Object.entries(instance.non_identifying_attributes ?? {}),
  ].sort((attr1, attr2) => defaultCompare(attr1[0], attr2[0]));

  return (
    <Stack gap="lg">
      <Section title={'Collector'}>
        <SimpleGrid cols={{base: 1, md: 3}} spacing="md">
          <Section title={`Host ${instance.hostname ?? instance.instance_uid}`} titleAs="h3">
            <DetailRow>
              <DetailLabel>Status:</DetailLabel>
              <InstanceStatusLabel status={instance.status} />
            </DetailRow>
            <DetailRow>
              <DetailLabel>OS:</DetailLabel>
              <span>{collectorOsName(instance)}</span>
            </DetailRow>
            <DetailRow>
              <DetailLabel>Version:</DetailLabel>
              <span>{instance.version || 'Unknown'}</span>
            </DetailRow>
            <DetailRow>
              <DetailLabel>Last seen:</DetailLabel>
              <RelativeTime dateTime={instance.last_seen} />
            </DetailRow>
            <DetailRow>
              <DetailLabel>Enrolled:</DetailLabel>
              <RelativeTime dateTime={instance.enrolled_at} />
            </DetailRow>

            {attributes.map(([key, value]) => (
              <DetailRow key={key}>
                <DetailLabel>{key}</DetailLabel>
                <span>{String(value)}</span>
              </DetailRow>
            ))}
          </Section>

          <Section title="Fleet" titleAs="h3">
            <DetailRow>
              <DetailLabel>Name:</DetailLabel>
              <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>{fleetName ?? 'Unknown'}</Link>
            </DetailRow>
            <DetailRow>
              <DetailLabel>Sources:</DetailLabel>
              <span>{sources?.length ?? 0} configured</span>
            </DetailRow>

            <IconRowList>
              {sources?.map((source) => (
                <IconRow key={source.id}>
                  <Label bsStyle="info">{SOURCE_TYPE_LABELS[source.type] ?? source.type}</Label>
                  <span>{source.name}</span>
                </IconRow>
              ))}
            </IconRowList>
          </Section>

          <Section title="What's next?" titleAs="h3">
            <IconRowList>
              <IconRow>
                <Icon name="hub" />
                <Link to={Routes.SYSTEM.COLLECTORS.FLEETS}>Manage Fleets</Link>
              </IconRow>
              <IconRow>
                <Icon name="settings" />
                <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>Configure Sources</Link>
              </IconRow>
              <IconRow>
                <Icon name="dns" />
                <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>View Instances</Link>
              </IconRow>
            </IconRowList>
          </Section>
        </SimpleGrid>
      </Section>
      {/* Zero gap: each `Section` already carries its own bottom margin. */}
      <Stack gap={0}>
        <LogPreviewSection
          title="Your log sources"
          searchUrl={collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid)}
          preview={sourceLogs}
          isLoading={isLoading}
          error={sourceLogsError}
        />

        <LogPreviewSection
          title="Collector logs"
          searchUrl={collectorSystemLogsUrl(instance.instance_uid)}
          preview={selfLogs}
          isLoading={isLoading}
          error={selfLogsError}
          collapsible
        />
      </Stack>
    </Stack>
  );
};

export default ConnectionSuccess;
