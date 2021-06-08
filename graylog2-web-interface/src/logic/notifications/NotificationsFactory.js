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
import React from 'react';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import HideOnCloud from 'util/conditional/HideOnCloud';

class NotificationsFactory {
  static getForNotification(notification) {
    switch (notification.type) {
      case 'check_server_clocks':
        return {
          title: 'Check the system clocks of your Graylog server nodes.',
          description: (
            <span>
              A Graylog server node detected a condition where it was deemed to be inactive immediately after being active.
              This usually indicates either a significant jump in system time, e.g. via NTP, or that a second Graylog server node
              is active on a system that has a different system time. Please make sure that the clocks of graylog2 systems are synchronized.
            </span>
          ),
        };
      case 'deflector_exists_as_index':
        return {
          title: 'Deflector exists as an index and is not an alias.',
          description: (
            <span>
              The deflector is meant to be an alias but exists as an index. Multiple failures of infrastructure can lead
              to this. Your messages are still indexed but searches and all maintenance tasks will fail or produce incorrect
              results. It is strongly recommend that you act as soon as possible.
            </span>
          ),
        };
      case 'email_transport_configuration_invalid':
        return {
          title: 'Email Transport Configuration is missing or invalid!',
          description: (
            <span>
              The configuration for the email transport subsystem has shown to be missing or invalid.
              Please check the related section of your Graylog server configuration file.
              This is the detailed error message: {notification.details.exception}
            </span>
          ),
        };
      case 'email_transport_failed':
        return {
          title: 'An error occurred while trying to send an email!',
          description: (
            <span>
              The Graylog server encountered an error while trying to send an email.
              This is the detailed error message: {notification.details.exception}
            </span>
          ),
        };
      case 'es_cluster_red':
        return {
          title: 'Elasticsearch cluster unhealthy (RED)',
          description: (
            <span>
              The Elasticsearch cluster state is RED which means shards are unassigned.
              This usually indicates a crashed and corrupt cluster and needs to be investigated. Graylog will write
              into the local disk journal. Read how to fix this in {' '}
              <DocumentationLink page={DocsHelper.PAGES.ES_CLUSTER_STATUS_RED} text="the Elasticsearch setup documentation." />
            </span>
          ),
        };
      case 'es_open_files':
        return {
          title: 'Elasticsearch nodes with too low open file limit',
          description: (
            <span>
              There are Elasticsearch nodes in the cluster that have a too low open file limit (current limit:{' '}
              <em>{notification.details.max_file_descriptors}</em> on <em>{notification.details.hostname}</em>;
              should be at least 64000) This will be causing problems
              that can be hard to diagnose. Read how to raise the maximum number of open files in {' '}
              <DocumentationLink page={DocsHelper.PAGES.ES_OPEN_FILE_LIMITS} text="the Elasticsearch setup documentation" />.
            </span>
          ),
        };
      case 'es_unavailable':
        return {
          title: 'Elasticsearch cluster unavailable',
          description: (
            <span>
              Graylog could not successfully connect to the Elasticsearch cluster. If you're using multicast, check that
              it is working in your network and that Elasticsearch is accessible. Also check that the cluster name setting
              is correct. Read how to fix this in {' '}
              <DocumentationLink page={DocsHelper.PAGES.ES_CLUSTER_UNAVAILABLE}
                                 text="the Elasticsearch setup documentation." />
            </span>
          ),
        };
      case 'gc_too_long':
        return {
          title: 'Nodes with too long GC pauses',
          description: (
            <span>
              There are Graylog nodes on which the garbage collector runs too long.
              Garbage collection runs should be as short as possible. Please check whether those nodes are healthy.
              (Node: <em>{notification.node_id}</em>, GC duration: <em>{notification.details.gc_duration_ms} ms</em>,
              GC threshold: <em>{notification.details.gc_threshold_ms} ms</em>)
            </span>
          ),
        };
      case 'generic':
        return {
          title: notification.details.title,
          description: notification.details.description,
        };
      case 'index_ranges_recalculation':
        return {
          title: 'Index ranges recalculation required',
          description: (
            <span>
              The index ranges are out of sync. Please go to System/Indices and trigger a index range recalculation from
              the Maintenance menu of {notification.details.index_sets ? (`the following index sets: ${notification.details.index_sets}`) : 'all index sets'}
            </span>
          ),
        };
      case 'input_failed_to_start':
        return {
          title: 'An input has failed to start',
          description: (
            <span>
              Input {notification.details.input_id} has failed to start on node {notification.node_id} for this reason:
              »{notification.details.reason}«. This means that you are unable to receive any messages from this input.
              This is mostly an indication for a misconfiguration or an error.
              <HideOnCloud>
                You can click <Link to={Routes.SYSTEM.INPUTS}>here</Link> to solve this.
              </HideOnCloud>
            </span>
          ),
        };
      case 'input_failure_shutdown':
        return {
          title: 'An input has shut down due to failures',
          description: (
            <span>
              Input {notification.details.input_title} has shut down on node {notification.node_id} for this reason:
              »{notification.details.reason}«. This means that you are unable to receive any messages from this input.
              This is often an indication of persistent network failures.
              You can click {' '} <Link to={Routes.SYSTEM.INPUTS}>here</Link> to see the input.
            </span>
          ),
        };
      case 'journal_uncommitted_messages_deleted':
        return {
          title: 'Uncommited messages deleted from journal',
          description: (
            <span>
              Some messages were deleted from the Graylog journal before they could be written to Elasticsearch. Please
              verify that your Elasticsearch cluster is healthy and fast enough. You may also want to review your Graylog
              journal settings and set a higher limit. (Node: <em>{notification.node_id}</em>)
            </span>
          ),
        };
      case 'journal_utilization_too_high':
        return {
          title: 'Journal utilization is too high',
          description: (
            <span>
              Journal utilization is too high and may go over the limit soon. Please verify that your Elasticsearch cluster
              is healthy and fast enough. You may also want to review your Graylog journal settings and set a higher limit.
              (Node: <em>{notification.node_id}</em>)
            </span>
          ),
        };
      case 'multi_master':
        return {
          title: 'Multiple Graylog server masters in the cluster',
          description: (
            <span>
              There were multiple Graylog server instances configured as master in your Graylog cluster. The cluster handles
              this automatically by launching new nodes as slaves if there already is a master but you should still fix this.
              Check the graylog.conf of every node and make sure that only one instance has is_master set to true. Close this
              notification if you think you resolved the problem. It will pop back up if you start a second master node again.
            </span>
          ),
        };
      case 'no_input_running':
        return {
          title: 'There is a node without any running inputs.',
          description: (
            <span>
              There is a node without any running inputs. This means that you are not receiving any messages from this
              node at this point in time. This is most probably an indication of an error or misconfiguration.
              <HideOnCloud>
                You can click <Link to={Routes.SYSTEM.INPUTS}>here</Link> to solve this.
              </HideOnCloud>
            </span>
          ),
        };
      case 'no_master':
        return {
          title: 'There was no master Graylog server node detected in the cluster.',
          description: (
            <span>
              Certain operations of Graylog server require the presence of a master node, but no such master was started.
              Please ensure that one of your Graylog server nodes contains the setting <code>is_master = true</code> in its
              configuration and that it is running. Until this is resolved index cycling will not be able to run, which
              means that the index retention mechanism is also not running, leading to increased index sizes. Certain
              maintenance functions as well as a variety of web interface pages (e.g. Dashboards) are unavailable.
            </span>
          ),
        };
      case 'outdated_version':
        return {
          title: 'You are running an outdated Graylog version.',
          description: (
            <span>
              The most recent stable Graylog version is <em>{notification.details.current_version}</em>.
              Get it from <a href="https://www.graylog.org/" target="_blank">https://www.graylog.org/</a>.
            </span>
          ),
        };
      case 'output_disabled':
        return {
          title: 'Output disabled',
          description: (
            <span>
              The output with the id {notification.details.outputId} in stream "{notification.details.streamTitle}"
              (id: {notification.details.streamId}) has been disabled for {notification.details.faultPenaltySeconds}
              seconds because there were {notification.details.faultCount} failures.
              (Node: <em>{notification.node_id}</em>, Fault threshold: <em>{notification.details.faultCountThreshold}</em>)
            </span>
          ),
        };
      case 'output_failing':
        return {
          title: 'Output failing',
          description: (
            <span>
              The output "{notification.details.outputTitle}" (id: {notification.details.outputId})
              in stream "{notification.details.streamTitle}" (id: {notification.details.streamId})
              is unable to send messages to the configured destination.
              <br />
              The error message from the output is: <em>{notification.details.errorMessage}</em>
            </span>
          ),
        };
      case 'stream_processing_disabled':
        return {
          title: 'Processing of a stream has been disabled due to excessive processing time.',
          description: (
            <span>
              The processing of stream <em>{notification.details.stream_title} ({notification.details.stream_id})</em> has taken too long for{' '}
              {notification.details.fault_count} times. To protect the stability of message processing,
              this stream has been disabled. Please correct the stream rules and reenable the stream.
              Check <DocumentationLink page={DocsHelper.PAGES.STREAM_PROCESSING_RUNTIME_LIMITS} text="the documentation" />{' '}
              for more details.
            </span>
          ),
        };
      case 'es_node_disk_watermark_low':
        return {
          title: 'Elasticsearch nodes disk usage above low watermark',
          description: (
            <span>
              There are Elasticsearch nodes in the cluster running out of disk space, their disk usage is above the low watermark.{' '}
              For this reason Elasticsearch will not allocate new shards to the affected nodes.{' '}
              The affected nodes are: [{notification.details.nodes}]{' '}
              Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a>{' '}
              for more details.
            </span>
          ),
        };
      case 'es_node_disk_watermark_high':
        return {
          title: 'Elasticsearch nodes disk usage above high watermark',
          description: (
            <span>
              There are Elasticsearch nodes in the cluster with almost no free disk, their disk usage is above the high watermark.{' '}
              For this reason Elasticsearch will attempt to relocate shards away from the affected nodes.{' '}
              The affected nodes are: [{notification.details.nodes}]{' '}
              Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a>{' '}
              for more details.
            </span>
          ),
        };
      case 'es_node_disk_watermark_flood_stage':
        return {
          title: 'Elasticsearch nodes disk usage above flood stage watermark',
          description: (
            <span>
              There are Elasticsearch nodes in the cluster without free disk, their disk usage is above the flood stage watermark.{' '}
              For this reason Elasticsearch enforces a read-only index block on all indexes having any of their shards in any of the{' '}
              affected nodes. The affected nodes are: [{notification.details.nodes}]{' '}
              Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a>{' '}
              for more details.
            </span>
          ),
        };
      case 'es_version_mismatch':
        const { initial_version: initialVersion, current_version: currentVersion } = notification.details;

        return {
          title: 'Elasticsearch version is incompatible',
          description: (
            <span>
              The Elasticsearch version which is currently running ({currentVersion}) has a different major version than
              the one the Graylog master node was started with ({initialVersion}).{' '}
              This will most probably result in errors during indexing or searching. Graylog requires a full restart after an
              Elasticsearch upgrade from one major version to another.
              <br />
              For details, please see our notes about{' '}
              <DocumentationLink page={DocsHelper.PAGES.ROLLING_ES_UPGRADE}
                                 text="rolling Elasticsearch upgrades." />

            </span>
          ),
        };
      case 'legacy_ldap_config_migration':
        const { auth_service_id: authServiceId } = notification.details;
        const authServiceLink = <Link to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(authServiceId)}>Authentication Service</Link>;

        return {
          title: 'Legacy LDAP/Active Directory configuration has been migrated to an Authentication Service',
          description: (
            <span>
              The legacy LDAP/Active Directory configuration of this system has been upgraded to a new {authServiceLink}.
              Since the new {authServiceLink} requires some information that is not present in the legacy
              configuration, the {authServiceLink} <strong>requires a manual review</strong>!
              <br />
              <br />
              <strong>After reviewing the {authServiceLink} it must be enabled to allow LDAP or Active Directory users
                to log in again!
              </strong>
              <br />
              <br />
              Please check the <DocumentationLink page={DocsHelper.PAGES.UPGRADE_GUIDE} text="upgrade guide" />
              for more details.
            </span>
          ),
        };
      default:
        return { title: `unknown (${notification.type})`, description: 'unknown' };
    }
  }
}

export default NotificationsFactory;
