Graylog NetFlow Plugin
======================

This plugin provides a NetFlow UDP input to act as a Flow collector that receives data from Flow exporters. Each received Flow will be converted to a Graylog message.

Example fields:

![NetFlow example fields screenshot](netflow-example.png)

## Supported NetFlow Versions

The plugin only supports NetFlow V5 at the moment.

## Credits

The NetFlow parsing code is based on the https://github.com/wasted/netflow project and has been ported from Scala to Java.

## Development

### Testing

To generate some NetFlow data for debugging and testing you can use softflowd.

Example command and output:

```
# softflowd -D -i eth0 -v 5 -t maxlife=1 -n 10.0.2.2:2055

Using eth0 (idx: 0)
softflowd v0.9.9 starting data collection
Exporting flows to [10.0.2.2]:2055
ADD FLOW seq:1 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6
ADD FLOW seq:2 [10.0.2.2]:51428 <> [10.0.2.15]:22 proto:6
Starting expiry scan: mode 0
Queuing flow seq:1 (0x7fef0318bc70) for expiry reason 6
Finished scan 1 flow(s) to be evicted
Sending v5 flow packet len = 120
sent 1 netflow packets
EXPIRED: seq:1 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6 octets>:322 packets>:7 octets<:596 packets<:7 start:2015-07-21T13:18:01.236 finish:2015-07-21T13:18:27.718 tcp>:10 tcp<:18 flowlabel>:00000000 flo
wlabel<:00000000  (0x7fef0318bc70)
ADD FLOW seq:3 [10.0.2.2]:2055 <> [10.0.2.15]:48363 proto:17
ADD FLOW seq:4 [10.0.2.2]:48164 <> [10.0.2.15]:22 proto:6
```
