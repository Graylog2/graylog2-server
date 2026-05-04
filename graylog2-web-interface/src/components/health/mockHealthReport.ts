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
      status: 'critical',
      children: [
        {
          id: 'graylog.server',
          title: 'Server',
          status: 'critical',
          children: [
            { id: 'graylog.server.storage', title: 'Storage', status: 'warning', total_affected: 1, total: 3 },
            { id: 'graylog.server.cpu', title: 'CPU', status: 'warning', total_affected: 1, total: 3 },
            { id: 'graylog.server.memory', title: 'Memory', status: 'critical', total_affected: 1, total: 3 },
            { id: 'graylog.server.certificates', title: 'Certificates', status: 'unknown', total_affected: 0 },
            { id: 'graylog.server.load_balancer', title: 'Load Balancer', status: 'warning', total_affected: 1, total: 3 },
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
          status: 'critical',
          children: [
            { id: 'graylog.input.input_buffer', title: 'Input Buffer', status: 'warning', total_affected: 1, total: 3 },
            { id: 'graylog.input.input_failures', title: 'Input Failures', status: 'critical', total_affected: 2, total: 8 },
          ],
        },
        {
          id: 'graylog.processing',
          title: 'Processing',
          status: 'warning',
          children: [
            {
              id: 'graylog.processing.processing_buffer',
              title: 'Processing Buffer',
              status: 'warning',
              total_affected: 1,
              total: 3,
            },
            { id: 'graylog.processing.journal_size', title: 'Journal Size', status: 'warning', total_affected: 2, total: 3 },
          ],
        },
        {
          id: 'graylog.output',
          title: 'Output',
          status: 'critical',
          children: [
            { id: 'graylog.output.output_buffer', title: 'Output Buffer', status: 'warning', total_affected: 1, total: 3 },
            {
              id: 'graylog.output.report_generation',
              title: 'Report Generation',
              status: 'critical',
              total_affected: 2,
              total: 5,
            },
          ],
        },
        {
          id: 'graylog.archiving',
          title: 'Archiving',
          status: 'warning',
          children: [
            { id: 'graylog.archiving.archive_failures', title: 'Archive Failures', status: 'warning', total_affected: 3 },
          ],
        },
        {
          id: 'graylog.data_lake',
          title: 'Data Lake',
          status: 'critical',
          children: [
            { id: 'graylog.data_lake.connectivity', title: 'Connectivity', status: 'critical', total_affected: 1 },
            { id: 'graylog.data_lake.message_drops', title: 'Message Drops', status: 'warning', total_affected: 142 },
          ],
        },
        {
          id: 'graylog.integrations',
          title: 'Integrations',
          status: 'critical',
          children: [
            { id: 'graylog.integrations.idp_sync', title: 'IdP Sync', status: 'warning', total_affected: 1, total: 2 },
            {
              id: 'graylog.integrations.email_transport',
              title: 'Email Transport',
              status: 'critical',
              total_affected: 1,
            },
          ],
        },
      ],
    },
    {
      id: 'search_cluster',
      title: 'Search Cluster',
      status: 'critical',
      children: [
        {
          id: 'search_cluster.server',
          title: 'Server',
          status: 'critical',
          children: [
            { id: 'search_cluster.server.storage', title: 'Storage', status: 'warning', total_affected: 1, total: 3 },
            { id: 'search_cluster.server.cpu', title: 'CPU', status: 'warning', total_affected: 1, total: 3 },
            { id: 'search_cluster.server.memory', title: 'Memory', status: 'critical', total_affected: 1, total: 3 },
            { id: 'search_cluster.server.certificates', title: 'Certificates', status: 'unknown', total_affected: 0 },
            { id: 'search_cluster.server.state', title: 'State', status: 'warning', total_affected: 1, total: 3 },
          ],
        },
        {
          id: 'search_cluster.index_management',
          title: 'Index Management',
          status: 'warning',
          children: [
            {
              id: 'search_cluster.index_management.rotation',
              title: 'Rotation',
              status: 'warning',
              total_affected: 1,
            },
            {
              id: 'search_cluster.index_management.retention_delete',
              title: 'Retention Delete',
              status: 'warning',
              total_affected: 1,
            },
            {
              id: 'search_cluster.index_management.warm_tier_move',
              title: 'Warm Tier Move',
              status: 'warning',
              total_affected: 2,
            },
            {
              id: 'search_cluster.index_management.shard_count',
              title: 'Shard Count',
              status: 'warning',
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
        { id: 'mongodb.connectivity', title: 'Connectivity', status: 'critical', total_affected: 1 },
        { id: 'mongodb.primary_state', title: 'Primary State', status: 'critical', total_affected: 1 },
        { id: 'mongodb.slow_queries', title: 'Slow Queries', status: 'warning', total_affected: 12 },
        { id: 'mongodb.storage', title: 'Storage', status: 'warning', total_affected: 1, total: 3 },
      ],
    },
    {
      id: 'forwarders',
      title: 'Forwarders',
      status: 'warning',
      children: [],
    },
    {
      id: 'collectors',
      title: 'Collectors',
      status: 'warning',
      children: [],
    },
  ],
};

export default mockHealthReport;
