# Graylog NetFlow Plugin

NetFlow related Graylog plugins.


## Testing

Install the pmacct package to collect and send NetFlow data to Graylog.

Example configuration:

```
daemonize: false
debug: true
interface: eth0
plugins: nfprobe
nfprobe_receiver: 10.0.2.2:5555 # ip and port of the Graylog NetFlow input
nfprobe_version: 5              # NetFlow version
```

The following files provide examples and a config reference.

* /usr/share/doc/pmacct/EXAMPLES.gz
* /usr/share/doc/pmacct/CONFIG-KEYS.gz

Start pmacctd with the config:

```
$ pmacctd -f pmacctd.conf
INFO ( default/nfprobe ): 131070 bytes are available to address shared memory segment; buffer size is 244 bytes.
INFO ( default/nfprobe ): Trying to allocate a shared memory segment of 3997452 bytes.
OK ( default/core ): link type is: 1
INFO ( default/nfprobe ): NetFlow probe plugin is originally based on softflowd 0.9.7 software, Copyright 2002 Damien Miller <djm@mindrot.org> All rights reserved.
INFO ( default/nfprobe ):           TCP timeout: 3600s
INFO ( default/nfprobe ):  TCP post-RST timeout: 120s
INFO ( default/nfprobe ):  TCP post-FIN timeout: 300s
INFO ( default/nfprobe ):           UDP timeout: 300s
INFO ( default/nfprobe ):          ICMP timeout: 300s
INFO ( default/nfprobe ):       General timeout: 3600s
INFO ( default/nfprobe ):      Maximum lifetime: 604800s
INFO ( default/nfprobe ):       Expiry interval: 60s
INFO ( default/nfprobe ): Exporting flows to [10.0.2.2]:5555
DEBUG ( default/nfprobe ): ADD FLOW seq:1 [10.0.2.2]:58442 <> [10.0.2.15]:22 proto:6
```

Running pmacctd without config and print out flows:

```
$ pmacctd -P print -r 1 -i eth0 -c src_host,dst_host
```
