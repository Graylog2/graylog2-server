import PropTypes from 'prop-types';
import lodash from 'lodash';
import React from 'react';

import ConfigurationHelperStyle from './ConfigurationHelper.css';

class FilebeatHelper extends React.Component {
  static propTypes = {
    section: PropTypes.string,
    paragraph: PropTypes.string,
  };

  static toc = {
    inputs: ['log', 'syslog', 'docker', 'tcp', 'udp'],
    outputs: ['graylog', 'kafka', 'file'],
    processors: ['fields', 'drop events'],
  };

  inputsLog = () => {
    return (
      <div>
        <h3>File Input</h3>
        Use the log input to read lines from log files.
        {this.example(`filebeat.inputs:
  - type: log
    paths:
      - /var/log/apache/httpd-*.log`)}
        <b>paths</b><br />
        Paths that should be crawled and fetched. Glob based paths.
        To fetch all &quot;.log&quot; files from a specific level of subdirectories
        For each file found under this path, a harvester is started.
        Make sure no file is defined twice as this can lead to unexpected behaviour.
        {this.example(`paths:
  - /var/log/*.log
  - c:\\programdata\\elasticsearch\\logs\\*`)}

        <b>encoding</b><br />
        Configure the file encoding for reading files with international characters.
        {this.example('Some sample encodings: plain, utf-8, utf-16be-bom, utf-16be, utf-16le, big5, gb18030, gbk...')}

        <b>exclude_lines</b><br />
        A list of regular expressions to match. It drops the lines that are
        matching any regular expression from the list. The include_lines is called before
        exclude_lines.
        {this.example(`exclude_lines: ['^DBG']`)}

        <b>include_lines</b><br />
        A list of regular expressions to match. It exports the lines that are
        matching any regular expression from the list. The include_lines is called before
        exclude_lines.
        {this.example(`include_lines: ['^ERR', '^WARN']`)}

        <b>exclude_files</b><br />
        Exclude files. A list of regular expressions to match. Filebeat drops the files that
        are matching any regular expression from the list.
        {this.example(`exclude_files: ['.gz$']`)}

        <b>fields</b><br />
        Optional additional fields. These fields can be freely picked
        to add additional information to the crawled log files for filtering.
        {this.example(`fields:
  level: debug
  review: 1`)}

        <b>fields_under_root</b><br />
        Set to true to store the additional fields as top level fields instead
        of under the &quot;fields&quot; sub-dictionary.
        {this.example('fields_under_root: true')}

        <b>ignore_older</b><br />
        Ignore files which were modified more then the defined timespan in the past.
        {this.example('ignore_older: 2h')}

        <b>scan_frequency</b><br />
        How often the prospector checks for new files in the paths that are specified
        for harvesting.
        {this.example('scan_frequency: 10s')}

        <b>multiline</b><br />
        Options that control how Filebeat deals with log messages that span multiple lines.
        See Manage <a href="https://www.elastic.co/guide/en/beats/filebeat/current/multiline-examples.htm">multiline</a>
        &nbsp;messages for more information about configuring multiline options.
        <br />

        <br /><b>close_inactive</b><br />
        Closes the file handler after the predefined period.
        The period starts when the last line of the file was, not the file ModTime.
        {this.example('close_inactive: 5m')}

        <b>clean_inactive</b><br />
        Files for the modification data is older then clean_inactive the state from the registry is removed.
        {this.example('clean_inactive: 0')}
      </div>
    );
  };

  inputsSyslog = () => {
    return (
      <div>
        <h3>Syslog Input</h3>
        Use the syslog input to read events over TCP or UDP, this input will parse BSD (rfc3164) event and some variant.
        {this.example(`filebeat.inputs:
- type: syslog
  protocol.udp:
    host: "localhost:9000"`)}

        <b>host</b><br />
        The host and UDP port to listen on for event streams.
        <br />

        <br /><i>Protocol tcp:</i><br />

        <br /><b>line_delimiter</b><br />
        Specify the characters used to split the incoming events. The default is \n.
        <br />

        <br /><b>timeout</b><br />
        The number of seconds of inactivity before a remote connection is closed. The default is 300s.
        <br />

        <br /><b>ssl</b><br />
        Configuration options for SSL parameters like the certificate, key and the certificate authorities to use.
        See <a href="https://www.elastic.co/guide/en/beats/filebeat/current/configuration-ssl.html">Specify SSL settings</a>
        &nbsp;for more information.<br />

        <br /><i>Common options:</i><br />
        <br /><b>tags</b><br />
        A list of tags that Filebeat includes in the tags field of each published event.
        {this.example(`filebeat.inputs:
- type: syslog
  . . .
  tags: ["json"]`)}

        <br /><b>fields</b><br />
        Optional fields that you can specify to add additional information to the output.
        For example, you might add fields that you can use for filtering log data.
      </div>
    );
  };

  inputsDocker = () => {
    return (
      <div>
        <h3>Docker Input</h3>
        Use the docker input to read logs from Docker containers.
        {this.example(`filebeat.inputs:
- type: docker
  containers.ids:
    - '8b6fe7dc9e067b58476dc57d6986dd96d7100430c5de3b109a99cd56ac655347'`)}

        <b>containers.ids</b><br />
        The list of Docker container IDs to read logs from. Specify containers.ids: '*' to read from all containers.
        <br />

        <br /><b>containers.path</b><br />
        The base path where Docker logs are located. The default is /var/lib/docker/containers.
        <br />

        <br /><b>containers.streamM</b><br />
        Reads from the specified streams only: all, stdout or stderr. The default is all.
        <br />

        <br /><b>combine_partial</b><br />
        Enable partial messages joining. Docker json-file driver splits log lines larger than 16k bytes,
        end of line (\n) is present for common lines in the resulting file, while it’s not the for the lines
        that have been split. combine_partial joins them back together when enabled. It is enabled by default.
      </div>
    );
  };

  inputsTcp = () => {
    return (
      <div>
        <h3>TCP Input</h3>
        Use the TCP input to read events over TCP.
        {this.example(`filebeat.inputs:
- type: tcp
  max_message_size: 10MiB
  host: "localhost:9000"`)}

        <b>max_message_size</b><br />
        The maximum size of the message received over TCP. The default is 20MiB.
        <br />

        <br /><b>host</b><br />
        The host and TCP port to listen on for event streams.
        <br />

        <br /><b>line_delimiter</b><br />
        Specify the characters used to split the incoming events. The default is \n.
        <br />

        <br /><b>timeout</b><br />
        The number of seconds of inactivity before a remote connection is closed. The default is 300s.
        <br />

        <br /><b>ssl</b><br />
        Configuration options for SSL parameters like the certificate, key and the certificate authorities to use.
        See <a href="https://www.elastic.co/guide/en/beats/filebeat/current/configuration-ssl.html">Specify SSL settings</a>
        &nbsp;for more information.
      </div>
    );
  };

  inputsUdp = () => {
    return (
      <div>
        <h3>UDP Input</h3>
        Use the UDP input to read events over UDP.
        {this.example(`filebeat.inputs:
- type: udp
  max_message_size: 10KiB
  host: "localhost:8080"`)}

        <b>max_message_size</b><br />
        The maximum size of the message received over UDP. The default is 10KiB.
        <br />

        <br /><b>host</b><br />
        The host and UDP port to listen on for event streams.
      </div>
    );
  };

  outputsGraylog = () => {
    return (
      <div>
        <h3>Beats Output</h3>
        This is also called the Logstash output but actually it's an output that is using the Beats protocol. Start a Beats
        input on the Graylog server to make use of this.<br/>
        <b>hosts</b><br/>
        {this.example(`output.logstash:
   hosts: ["localhost:5044"]`)}

        <b>worker</b><br/>
        Number of workers per host.
        {this.example(`worker: 1`)}

        <b>compression_level</b><br/>
        Set gzip compression level.
        {this.example(`compression_level: 3`)}

        <b>ttl</b><br/>
        Optional maximum time to live for a connection, after which the
        connection will be re-established.
        {this.example(`ttl: 30s`)}

        <b>loadbalance</b><br/>
        Optional load balance the events between the hosts.
        {this.example(`loadbalance: false`)}

        <b>ssl.enabled</b><br/>
        Enable SSL support. SSL is automatically enabled, if any SSL setting is set.<br/>

        <b>ssl.verification_mode</b><br/>
        Configure SSL verification mode. If `none` is configured, all server hosts
        and certificates will be accepted. In this mode, SSL based connections are
        susceptible to man-in-the-middle attacks.
        {this.example(`ssl.verification_mode: full`)}

        <b>ssl.certificate_authorities</b><br/>
        List of root certificates for server verifications.
        {this.example(`ssl.certificate_authorities: ["/etc/pki/root/ca.pem"]`)}

        <b>ssl.key</b><br/>
        Client Certificate Key.
        {this.example(`ssl.key: "/etc/pki/client/cert.key"`)}

        <b>ssl.key_passphrase</b><br/>
        Optional passphrase for decrypting the Certificate Key.
        {this.example(`ssl.key_passphrase: 'secure'`)}
      </div>
    );
  };

  outputsKafka = () => {
    return (
      <div>
        <h3>Kafka Output</h3>
        The Kafka output sends the events to Apache Kafka.
        {this.example(`output.kafka:
  # initial brokers for reading cluster metadata
  hosts: ["kafka1:9092", "kafka2:9092", "kafka3:9092"]

  # message topic selection + partitioning
  topic: '%{[fields.log_topic]}'
  partition.round_robin:
    reachable_only: false

  required_acks: 1
  compression: gzip
  max_message_bytes: 1000000`)}

        <b>hosts</b><br />
        The list of Kafka broker addresses from where to fetch the cluster metadata.
        The cluster metadata contain the actual Kafka brokers events are published to.
        <br />

        <br /><b>username</b><br />
        The username for connecting to Kafka. If username is configured, the password must be configured as well.
        Only SASL/PLAIN is supported.
        <br />

        <br /><b>password</b><br />
        The password for connecting to Kafka.
        <br />

        <br /><b>topic</b><br />
        The Kafka topic used for produced events. The setting can be a format string using any event field.
        For example, you can use the fields configuration option to add a custom field called log_topic to the event,
        and then set topic to the value of the custom field:
        {this.example('topic: \'%{[fields.log_topic]}\'')}

        <b>topics</b><br />
        Array of topic selector rules supporting conditionals, format string based field access and name mappings.
        The first rule matching will be used to set the topic for the event to be published. If topics is missing or
        no rule matches, the topic field will be used.

        Rule settings:<br />
        <b>topic:</b> The topic format string to use. If the fields used are missing, the rule fails.<br />
        <b>mapping:</b> Dictionary mapping index names to new names<br />
        <b>default:</b> Default string value if mapping does not find a match.<br />
        <b>when:</b> Condition which must succeed in order to execute the current rule.<br />
        <br />

        <br /><b>partition</b><br />
        Kafka output broker event partitioning strategy. Must be one of random, round_robin, or hash. By default the
        hash partitioner is used.<br />

        <b>random.group_events:</b> Sets the number of events to be published to the same partition, before
        the partitioner selects a new partition by random. The default value is 1 meaning after each event a
        new partition is picked randomly.<br />
        <b>round_robin.group_events:</b> Sets the number of events to be published to the same partition, before the
        partitioner selects the next partition. The default value is 1 meaning after each event the next
        partition will be selected.<br />
        <b>hash.hash:</b> List of fields used to compute the partitioning hash value from. If no field is configured,
        the events key value will be used.<br />
        <b>hash.random:</b> Randomly distribute events if no hash or key value can be computed.<br />

        All partitioners will try to publish events to all partitions by default. If a partition’s leader becomes
        unreachable for the beat, the output might block. All partitioners support setting reachable_only to overwrite
        this behavior. If reachable_only is set to true, events will be published to available partitions only.
      </div>
    );
  };

  processorsFields = () => {
    return (
      <div>
        Processors are used to reduce the number of fields in the exported event or to
        enhance the event with external metadata.<br/>
        <b>fields</b><br/>
        {this.example(`processors:
  - include_fields:
    fields: ["cpu"]
  - drop_fields:
    fields: ["cpu.user", "cpu.system"]`)}
      </div>
    );
  };

  processorsDropEvents = () => {
    return (
      <div>
        The following example drops the events that have the HTTP response code 200:<br/>
        <b>drop_event</b><br/>
        {this.example(`processors:
  - drop_event:
      when:
         equals:
             http.code: 200`)}
      </div>
    );
  };

  example = (data) => {
    return (
      <pre className={`${ConfigurationHelperStyle.marginTab} ${ConfigurationHelperStyle.exampleFunction}`} >{data}</pre>
    );
  };

  lookupName = () => {
    return lodash.camelCase(`${this.props.section} ${this.props.paragraph}`);
  };

  render() {
    if (this.props.section && this.props.paragraph) {
      return (
        this[this.lookupName()]()
      );
    } else {
      return (
        <div>Choose a configuration topic from the drop down to get a quick help.</div>
      );
    }
  }
}

export default FilebeatHelper;
