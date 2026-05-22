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
import type { HealthReport } from './HealthReport.types';

const mockHealthReport: HealthReport = {
  overall_status: 'critical',
  generated_at: '2026-04-27T08:45:00Z',
  features: [
    {
      id: 'graylog',
      title: 'Graylog',
      status: 'warning',
      children: [
        {
          id: 'graylog.server',
          title: 'Server',
          status: 'warning',
          children: [
            { id: 'graylog.server.storage', title: 'Storage', status: 'healthy', total_affected: 0 },
            { id: 'graylog.server.cpu', title: 'CPU', status: 'healthy', total_affected: 0 },
            { id: 'graylog.server.memory', title: 'Memory', status: 'warning', total_affected: 1, total: 3 },
            { id: 'graylog.server.certificates', title: 'Certificates', status: 'unknown', total_affected: 0 },
            { id: 'graylog.server.load_balancer', title: 'Load Balancer', status: 'healthy', total_affected: 0 },
            {
              id: 'graylog.server.processing_state',
              title: 'Processing State',
              status: 'warning',
              total_affected: 1,
              total: 3,
            },
          ],
        },
        {
          id: 'graylog.input',
          title: 'Input',
          status: 'healthy',
          children: [
            { id: 'graylog.input.input_buffer', title: 'Input Buffer', status: 'healthy', total_affected: 0 },
            { id: 'graylog.input.input_failures', title: 'Input Failures', status: 'healthy', total_affected: 0 },
          ],
        },
        {
          id: 'graylog.processing',
          title: 'Processing',
          status: 'healthy',
          children: [
            {
              id: 'graylog.processing.processing_buffer',
              title: 'Processing Buffer',
              status: 'healthy',
              total_affected: 0,
            },
            { id: 'graylog.processing.journal_size', title: 'Journal Size', status: 'healthy', total_affected: 0 },
          ],
        },
        {
          id: 'graylog.output',
          title: 'Output',
          status: 'healthy',
          children: [
            { id: 'graylog.output.output_buffer', title: 'Output Buffer', status: 'healthy', total_affected: 0 },
            {
              id: 'graylog.output.report_generation',
              title: 'Report Generation',
              status: 'healthy',
              total_affected: 0,
            },
          ],
        },
        {
          id: 'graylog.archiving',
          title: 'Archiving',
          status: 'healthy',
          children: [
            {
              id: 'graylog.archiving.archive_failures',
              title: 'Archive Failures',
              status: 'healthy',
              total_affected: 0,
            },
          ],
        },
        {
          id: 'graylog.data_lake',
          title: 'Data Lake',
          status: 'healthy',
          children: [
            { id: 'graylog.data_lake.connectivity', title: 'Connectivity', status: 'healthy', total_affected: 0 },
            { id: 'graylog.data_lake.message_drops', title: 'Message Drops', status: 'healthy', total_affected: 0 },
          ],
        },
        {
          id: 'graylog.integrations',
          title: 'Integrations',
          status: 'healthy',
          children: [
            { id: 'graylog.integrations.idp_sync', title: 'IdP Sync', status: 'healthy', total_affected: 0 },
            {
              id: 'graylog.integrations.email_transport',
              title: 'Email Transport',
              status: 'healthy',
              total_affected: 0,
            },
          ],
        },
      ],
    },
    {
      id: 'search_cluster',
      title: 'Search Cluster',
      status: 'healthy',
      children: [
        {
          id: 'search_cluster.server',
          title: 'Server',
          status: 'healthy',
          children: [
            { id: 'search_cluster.server.storage', title: 'Storage', status: 'healthy', total_affected: 0 },
            { id: 'search_cluster.server.cpu', title: 'CPU', status: 'healthy', total_affected: 0 },
            { id: 'search_cluster.server.memory', title: 'Memory', status: 'healthy', total_affected: 0 },
            { id: 'search_cluster.server.certificates', title: 'Certificates', status: 'healthy', total_affected: 0 },
            { id: 'search_cluster.server.state', title: 'State', status: 'healthy', total_affected: 0 },
          ],
        },
        {
          id: 'search_cluster.index_management',
          title: 'Index Management',
          status: 'healthy',
          children: [
            { id: 'search_cluster.index_management.rotation', title: 'Rotation', status: 'healthy', total_affected: 0 },
            {
              id: 'search_cluster.index_management.retention_delete',
              title: 'Retention Delete',
              status: 'healthy',
              total_affected: 0,
            },
            {
              id: 'search_cluster.index_management.warm_tier_move',
              title: 'Warm Tier Move',
              status: 'healthy',
              total_affected: 0,
            },
            {
              id: 'search_cluster.index_management.shard_count',
              title: 'Shard Count',
              status: 'healthy',
              total_affected: 0,
            },
          ],
        },
      ],
    },
    {
      id: 'mongodb',
      title: 'MongoDB',
      status: 'critical',
      children: [
        {
          id: 'mongodb.connectivity',
          title: 'Connectivity',
          status: 'critical',
          total_affected: 1,
          message: [
            'com.mongodb.MongoSocketOpenException: Exception opening socket',
            '    at com.mongodb.internal.connection.SocketStream.open(SocketStream.java:70)',
            '    at com.mongodb.internal.connection.InternalStreamConnection.open(InternalStreamConnection.java:144)',
            '    at com.mongodb.internal.connection.DefaultServerMonitor$ServerMonitorRunnable.run(DefaultServerMonitor.java:117)',
            '    at java.base/java.lang.Thread.run(Thread.java:840)',
            'Caused by: java.net.ConnectException: Connection refused',
            '    at java.base/sun.nio.ch.Net.connect0(Native Method)',
            '    at java.base/sun.nio.ch.Net.connect(Net.java:589)',
            '    at java.base/sun.nio.ch.Net.connect(Net.java:578)',
            '    at java.base/sun.nio.ch.NioSocketImpl.connect(NioSocketImpl.java:583)',
            '    at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:327)',
            '    at java.base/java.net.Socket.connect(Socket.java:633)',
          ].join('\n'),
        },
        {
          id: 'mongodb.primary_state',
          title: 'Primary State',
          status: 'critical',
          total_affected: 1,
          message: 'Replica set has no primary; 2 of 3 voting members are unreachable.',
        },
        { id: 'mongodb.slow_queries', title: 'Slow Queries', status: 'healthy', total_affected: 0 },
        { id: 'mongodb.storage', title: 'Storage', status: 'healthy', total_affected: 0 },
      ],
    },
    {
      id: 'forwarders',
      title: 'Forwarders',
      status: 'healthy',
      children: [],
    },
    {
      id: 'collectors',
      title: 'Collectors',
      status: 'healthy',
      children: [],
    },
  ],
};

export default mockHealthReport;
