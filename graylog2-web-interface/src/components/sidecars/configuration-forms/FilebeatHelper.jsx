import PropTypes from 'prop-types';
import lodash from 'lodash';
import React from 'react';

import ConfigurationHelperStyle from './ConfigurationHelper.css';

const FilebeatHelper = React.createClass({
  propTypes: {
    section: PropTypes.string,
    paragraph: PropTypes.string,
  },

  statics: {
    toc: {
      prospectors: ['log'],
      outputs: ['logstash'],
      processors: ['fields', 'drop events'],
    },
  },

  prospectorsLog() {
    return (
      <div>
        <h3>Log Prospector</h3>
        Reads every line of the log file.
        {this.example(`filebeat.prospectors:
  - type: log
    paths:
      - /var/log/apache/httpd-*.log`)}
        <b>paths</b><br/>
        Paths that should be crawled and fetched. Glob based paths.
        To fetch all &quot;.log&quot; files from a specific level of subdirectories
        For each file found under this path, a harvester is started.
        Make sure not file is defined twice as this can lead to unexpected behaviour.
        {this.example(`paths:
  - /var/log/*.log
  - c:\\programdata\\elasticsearch\\logs\\*`)}

        <b>encoding</b><br/>
        Configure the file encoding for reading files with international characters.
        {this.example(`Some sample encodings: plain, utf-8, utf-16be-bom, utf-16be, utf-16le, big5, gb18030, gbk...`)}

        <b>exclude_lines</b><br/>
        A list of regular expressions to match. It drops the lines that are
        matching any regular expression from the list. The include_lines is called before
        exclude_lines.
        {this.example(`exclude_lines: ['^DBG']`)}

        <b>include_lines</b><br/>
        A list of regular expressions to match. It exports the lines that are
        matching any regular expression from the list. The include_lines is called before
        exclude_lines.
        {this.example(`include_lines: ['^ERR', '^WARN']`)}

        <b>exclude_files</b><br/>
        Exclude files. A list of regular expressions to match. Filebeat drops the files that
        are matching any regular expression from the list.
        {this.example(`exclude_files: ['.gz$']`)}

        <b>fields</b><br/>
        Optional additional fields. These fields can be freely picked
        to add additional information to the crawled log files for filtering.
        {this.example(`fields:
  level: debug
  review: 1`)}

        <b>fields_under_root</b><br/>
        Set to true to store the additional fields as top level fields instead
        of under the &quot;fields&quot; sub-dictionary.
        {this.example(`fields_under_root: true`)}

        <b>ignore_older</b><br/>
        Ignore files which were modified more then the defined timespan in the past.
        {this.example(`ignore_older: 2h`)}

        <b>scan_frequency</b><br/>
        How often the prospector checks for new files in the paths that are specified
        for harvesting.
        {this.example(`scan_frequency: 10s`)}

        <b>multiline.pattern</b><br/>
        Mutiline can be used for log messages spanning multiple lines. This is common
        for Java Stack Traces or C-Line Continuation. The regexp Pattern that has to be matched.
        {this.example(`multiline.pattern: ^\\[`)}

        <b>multiline.negate</b><br/>
        Defines if the pattern set under pattern should be negated or not.

        <b>multiline.match</b><br/>
        Match can be set to &quot;after&quot; or &quot;before&quot;. It is used to define if lines should be append to a pattern
        that was (not) matched before or after or as long as a pattern is not matched based on negate.
        {this.example(`multiline.match: after`)}

        <b>close_inactive</b><br/>
        Closes the file handler after the predefined period.
        The period starts when the last line of the file was, not the file ModTime.
        {this.example(`close_inactive: 5m`)}

        <b>clean_inactive</b><br/>
        Files for the modification data is older then clean_inactive the state from the registry is removed.
        {this.example(`clean_inactive: 0`)}
      </div>
    );
  },

  outputsLogstash() {
    return (
      <div>
        <h3>Logstash Output</h3>
        This is called the Logstash output but actually it's an output that is using the Beats protocol. Start a Beats
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
  },

  processorsFields() {
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
  },

  processorsDropEvents() {
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
  },

  example(data) {
    return (
      <pre className={`${ConfigurationHelperStyle.marginTab} ${ConfigurationHelperStyle.exampleFunction}`} >{data}</pre>
    );
  },

  lookupName() {
    return lodash.camelCase(`${this.props.section} ${this.props.paragraph}`);
  },

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
  },
});
export default FilebeatHelper;
