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
import { render, screen } from 'wrappedTestingLibrary';

import ContentStreamNewsItem from 'components/content-stream/news/ContentStreamNewsItem';

const feedMock = {
  title: 'Getting Started with GROK Patterns',
  link: 'https://graylog.org/post/getting-started-with-grok-patterns/',
  comments: 'https://graylog.org/post/getting-started-with-grok-patterns/?noamp=mobile#respond',
  'dc:creator': 'The Graylog Team',
  pubDate: 'Thu, 27 Jul 2023 18:30:07 +0000',
  category: [
    'Log Management & Analytics',
    'In Product',
  ],
  guid: {
    '#text': 'https://graylog.org/?p=16241',
    attr_isPermaLink: 'false',
  },
  description: '<p>If you’re new to logging, you might be tempted to collect all the data you possibly can. More information means more insights; at least, those NBC “the more you know” public services announcements told you it would help. Unfortunately, you can create new problems if you do too much logging. To streamline your log collection, [&#8230;]</p>\n<p>The post <a rel="nofollow" href="https://graylog.org/post/getting-started-with-grok-patterns/">Getting Started with GROK Patterns</a> appeared first on <a rel="nofollow" href="https://graylog.org">Graylog</a>.</p>\n',
  'content:encoded': '<p>If you’re new to logging, you might be tempted to collect all the data you possibly can. More information means more insights; at least, those NBC “the more you know” public services announcements told you it would help. Unfortunately, you can create new problems if you do too much logging. To <a href="https://www.graylog.org/post/achieve-more-streamlining-your-logs/">streamline your log collection</a>, you can apply some filtering of messages directly from the log source. However, to parse the data, you may need to use a Grok pattern.</p>\n<p>&nbsp;</p>\n<p>If you’re just getting started with Grok patterns, you might want to know what they are, how they work, and how to use them.</p>\n<h2>What is Grok?</h2>\n<p>Used for parsing and analyzing log data, Grok is a tool in the Elasticsearch, Logstash, and Kibana (ELK) stack that helps extract structured data from unstructured log messages. Grok uses regular expressions or pattern matching  to define pattern definitions, enabling users to separate log message fields to more easily analyze the data.</p>\n<p>&nbsp;</p>\n<p>With Grok, users can define patterns to match any type of log message data, including:</p>\n<ul>\n<li>Email addresses</li>\n<li>IP addresses</li>\n<li>Positive and negative integers</li>\n<li>Sets of characters</li>\n</ul>\n<p>&nbsp;</p>\n<p>Grok has a regular expression library and built-in patterns to make getting started easier. However, users can also create pattern files and add more patterns. With the filter plugins, users can apply patterns to log data in a configuration file.</p>\n<p>&nbsp;</p>\n<p>Grok patterns follow the Elastic Common Schema (ECS), enabling users to normalize event data at ingest time to make querying the data sources easier. Grok is particularly effective with log formats written for human rather than computers, like:</p>\n<ul>\n<li>Syslog logs</li>\n<li>Apache and other web server logs</li>\n<li>Mysql log</li>\n</ul>\n<h2>How does it work?</h2>\n<p>Grok patterns use regular expressions to match patterns in log messages. When the Grok filter finds a match, it separates the matched data into fields.</p>\n<h3>Regular expressions (regex)</h3>\n<p>Regex consists of a character sequence that defines a search pattern, enabling complex search and replace operations. The process works similarly to the “Find and Replace With” functions in Word and Google Docs.</p>\n<p>&nbsp;</p>\n<p>You should also keep in mind that regular expressions:</p>\n<ul>\n<li>Use a syntax of specific characters and symbols to define a pattern</li>\n<li>Can match patterns in strings, making them useful for processing and filtering large amounts of data</li>\n<li>Can replace parts of a string that match a pattern for advanced manipulation and editing</li>\n</ul>\n<p>&nbsp;</p>\n<p>Programming languages that use regular expressions, like Grok patterns, provide built-in libraries.</p>\n<h3>Grok basics</h3>\n<p>The fundamentals of parsing log messages with Grok patterns are:</p>\n<ul>\n<li><strong>Defining patterns</strong>: using regex syntax for predefined or custom patterns that include alphanumeric characters, sets of characters, single characters, or utf-8 characters</li>\n<li><strong>Matching patterns</strong>: using filter plugins to match and extract pattern-defined relevant fields from log messages</li>\n<li><strong>Pattern files</strong>: adding predefined or custom patterns to files for sharing across multiple projects or teams</li>\n<li><strong>Composite patterns</strong>: combining multiple predefined or custom patterns into a single pattern for more complex log parsing that simplifies the parsing process and reduces the overall number of partners needed</li>\n</ul>\n<p>&nbsp;</p>\n<h2>Using Grok patterns</h2>\n<p>Grok patterns are essential to processing and analyzing log data because they enable you to extract and categorize data fields within each message. Parsing data is the first step toward normalizing it which is ultimately how you can correlate events across your environment.</p>\n<h3>Normalizing diverse log data formats</h3>\n<p>Log data comes in various formats, including:</p>\n<ul>\n<li>CSV</li>\n<li>JSON</li>\n<li>XML</li>\n</ul>\n<p>&nbsp;</p>\n<p>Further, you need visibility into diverse log types, including:</p>\n<ul>\n<li>Access logs</li>\n<li>System logs</li>\n<li>Application logs</li>\n<li>Security logs</li>\n</ul>\n<p>&nbsp;</p>\n<p>With Grok patterns, you can parse these logs, extracting the defined fields no matter where it’s contained in the technology-generated format. Since you’re focusing on the type of information rather than the message itself, you can now correlate and analyze the data.</p>\n<h3>Debugging Grok expressions</h3>\n<p>Getting the regular expressions for parsing the log files can be challenging. For example, the username could be represented as either:</p>\n<ul>\n<li>USERNAME [a-zA-Z0-9._-]+</li>\n</ul>\n<p>or</p>\n<ul>\n<li>USER %{USERNAME}</li>\n</ul>\n<p>&nbsp;</p>\n<p>Debugging Grok expressions can be a little bit of trial and error, as you compare your expressions to the log files you want to parse.</p>\n<p>&nbsp;</p>\n<p>However, you can find online applications to help you construct a regular expression that matches your given log lines. Some examples of these tools include:</p>\n<ul>\n<li><strong>Incremental construction</strong>: prompting you to select common prefixes or Grok library patterns then running the segment against the log lines</li>\n<li><strong>Matcher</strong>: testing Grok expressions against several log lines simultaneously to determine matches and highlighting unmatched data</li>\n<li><strong>Automatic construction</strong>: running Grok expressions against log lines to generate options</li>\n</ul>\n<p>&nbsp;</p>\n<h3>Managing log data that doesn’t fit a defined pattern</h3>\n<p>Not every log message will have data that fits the defined pattern. Grok manages this in a few different ways:</p>\n<ul>\n<li>Ignoring lines in log data outside the defined pattern to filter out irrelevant or corrupted entries</li>\n<li>Adding custom tags to unmatched entries to identify and track issues with log data or categories entries based on custom criteria</li>\n<li>Using a separate log file or database table for further analyzing or troubleshooting log data</li>\n<li>Creating fallback patterns that apply when the initial pattern fails to match the entry for handling more complex log data</li>\n</ul>\n<p>&nbsp;</p>\n<h2>Graylog: Parsing Made Simple</h2>\n<p>With Graylog, you can use Grok patterns with both extractors and processing pipelines. However, our <a href="https://go2docs.graylog.org/5-0/what_more_can_graylog_do_for_me/security_content_packs.html?tocpath=What%20More%20Can%20Graylog%20Do%20for%20Me%253F%7CGraylog%20Illuminate%7CSecurity%20Content%20Packs%7C_____0">Graylog Illuminate</a> content is included with both <a href="https://www.graylog.org/products/security/">Graylog Security</a> and <a href="https://www.graylog.org/products/operations/">Graylog Operations</a>, enabling you to automate the parsing process without having to build your own Grok patterns. <a href="https://www.graylog.org/features/sidecar/">Graylog Sidecar</a> enables you to gather logs from all your computer systems with any log collection agent while centralized, mass deployment of sidecars can support multiple configurations per collector.</p>\n<p>&nbsp;</p>\n<p>With Graylog Operations and Graylog Security, you can use pre-built content, including parsing rules and pipelines, that help you get immediate value from your log data. You also gain access to search templates, dashboards, correlated alerts, reports, dynamic lookup tables, and streams that give you visibility into your environment. By leveraging our lightning-fast search capabilities, you can get the answers you need as soon as you need them to get to the root cause of incidents as quickly as possible.</p>\n<p>&nbsp;</p>\n<p>To learn more about how Graylog can help you gain the full value of your log data, <a href="https://www.graylog.org/contact-us/">contact us today.</a></p>\n<p>The post <a rel="nofollow" href="https://graylog.org/post/getting-started-with-grok-patterns/">Getting Started with GROK Patterns</a> appeared first on <a rel="nofollow" href="https://graylog.org">Graylog</a>.</p>\n',
  'wfw:commentRss': 'https://graylog.org/post/getting-started-with-grok-patterns/feed/',
  'slash:comments': 0,
  'media:content': {
    'media:title': {
      '#text': '0723_Getting Started with Grok Patterns',
      attr_type: 'plain',
    },
    'media:thumbnail': {
      attr_url: 'https://graylog.org/wp-content/uploads/2023/07/0723_Getting-Started-with-Grok-Patterns-150x150.jpg',
      attr_width: '150',
      attr_height: '150',
    },
    'media:copyright': 'Jeff Darrington',
    attr_url: 'https://graylog.org/wp-content/uploads/2023/07/0723_Getting-Started-with-Grok-Patterns.jpg',
    attr_type: 'image/jpeg',
    attr_medium: 'image',
    attr_width: '1200',
    attr_height: '628',
  },
};

describe('<ContentStreamNewsItem>', () => {
  it('Show ContentStreamNewsItem', () => {
    render(<ContentStreamNewsItem feed={feedMock} />);
    const image = screen.getByRole('img', { name: feedMock.title });
    const time = screen.getByText(/Jul 27, 2023/i);

    expect(image).toBeInTheDocument();
    expect(time).toBeInTheDocument();
  });
});
