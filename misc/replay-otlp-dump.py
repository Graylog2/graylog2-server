#!/usr/bin/env python3
"""
Replay OTLP collector dump files into a Graylog OTLP HTTP input.

Reads gzipped ndjson dump files (as produced by Graylog's OtlpTrafficDumpService),
reconstructs ExportLogsServiceRequest JSON payloads, and POSTs them to the OTLP
HTTP input endpoint.

Usage:
    # Auto-discover mTLS certs from collector data directory
    python3 replay-otlp-dump.py \
        --collector-dir /var/lib/graylog-collector \
        --endpoint https://localhost:14401/v1/logs \
        --receiver-type windowseventlog \
        data/otlp/*/_data/collector-otlp-dump/*.ndjson.gz

    # Explicit cert paths
    python3 replay-otlp-dump.py \
        --endpoint https://localhost:14401/v1/logs \
        --tls-cert signing.crt --tls-key signing.key --tls-ca ca.pem \
        --receiver-type windowseventlog *.ndjson.gz

    # Plain HTTP (standard OTLP input, no mTLS)
    python3 replay-otlp-dump.py --receiver-type windowseventlog *.ndjson.gz

    # Dry run (parse and count, don't send)
    python3 replay-otlp-dump.py --dry-run *.ndjson.gz
"""
import argparse
import gzip
import json
import os
import ssl
import sys
import tempfile
import time
import urllib.request
import urllib.error


def read_dump_records(files, receiver_type=None):
    """Yield (resource, scope, logRecord) tuples from dump files."""
    for filepath in files:
        opener = gzip.open if filepath.endswith('.gz') else open
        try:
            with opener(filepath, 'rt') as f:
                for line_num, line in enumerate(f, 1):
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        record = json.loads(line)
                    except json.JSONDecodeError as e:
                        print(f"  WARN: {filepath}:{line_num}: invalid JSON: {e}", file=sys.stderr)
                        continue

                    if receiver_type:
                        rt = record.get('collectorReceiverType', '')
                        if rt != receiver_type:
                            continue

                    otel = record.get('otelRecord', {})
                    log = otel.get('log', {})
                    resource = log.get('resource', {})
                    scope = log.get('scope', {})
                    log_record = log.get('logRecord', {})

                    if not log_record:
                        continue

                    yield resource, scope, log_record
        except Exception as e:
            print(f"  ERROR: {filepath}: {e}", file=sys.stderr)


def build_request(batch):
    """
    Build an ExportLogsServiceRequest JSON from a batch of (resource, scope, logRecord) tuples.

    Groups records by (resource, scope) to produce a compact request structure, matching
    how the OTLP spec organizes ResourceLogs -> ScopeLogs -> LogRecords.
    """
    resource_groups = {}
    for resource, scope, log_record in batch:
        resource_key = json.dumps(resource, sort_keys=True)
        scope_key = json.dumps(scope, sort_keys=True)

        if resource_key not in resource_groups:
            resource_groups[resource_key] = {'resource': resource, 'scopes': {}}

        scopes = resource_groups[resource_key]['scopes']
        if scope_key not in scopes:
            scopes[scope_key] = {'scope': scope, 'logRecords': []}

        scopes[scope_key]['logRecords'].append(log_record)

    resource_logs = []
    for rg in resource_groups.values():
        scope_logs = []
        for sg in rg['scopes'].values():
            scope_logs.append({
                'scope': sg['scope'],
                'logRecords': sg['logRecords']
            })
        resource_logs.append({
            'resource': rg['resource'],
            'scopeLogs': scope_logs
        })

    return {'resourceLogs': resource_logs}


def resolve_collector_tls(collector_dir):
    """
    Auto-discover mTLS material from a collector data directory.

    The collector v2 stores:
      - keys/signing.crt  — client certificate (X.509, PEM)
      - keys/signing.key  — client private key (Ed25519 PKCS#8, PEM)
      - supervisor/own-logs.yaml — persisted settings with ca_cert_pem (CA cert, PEM bytes)

    Returns (cert_path, key_path, ca_path_or_None).
    The CA cert is extracted from the YAML and written to a temp file if present.
    """
    keys_dir = os.path.join(collector_dir, 'keys')
    cert_path = os.path.join(keys_dir, 'signing.crt')
    key_path = os.path.join(keys_dir, 'signing.key')

    if not os.path.isfile(cert_path):
        sys.exit(f"ERROR: Client certificate not found: {cert_path}")
    if not os.path.isfile(key_path):
        sys.exit(f"ERROR: Client key not found: {key_path}")

    print(f"  Client cert: {cert_path}")
    print(f"  Client key:  {key_path}")

    # Try to extract CA cert from persisted own-logs.yaml
    ca_path = None
    own_logs_path = os.path.join(collector_dir, 'supervisor', 'own-logs.yaml')
    if os.path.isfile(own_logs_path):
        ca_pem = _extract_ca_from_own_logs(own_logs_path)
        if ca_pem:
            # Write to a temp file so ssl.load_verify_locations can read it
            tmp = tempfile.NamedTemporaryFile(mode='wb', suffix='.pem', prefix='collector-ca-', delete=False)
            tmp.write(ca_pem)
            tmp.close()
            ca_path = tmp.name
            print(f"  CA cert:     {ca_path} (extracted from {own_logs_path})")
        else:
            print(f"  CA cert:     not found in {own_logs_path}")
    else:
        print(f"  CA cert:     {own_logs_path} not found, will use system CA store")

    return cert_path, key_path, ca_path


def _extract_ca_from_own_logs(yaml_path):
    """
    Extract the CA certificate PEM from the collector's own-logs.yaml.

    The YAML stores the CA cert as a block scalar under 'tls_ca_pem_contents'
    or 'ca_cert_pem'. Requires PyYAML (pip install pyyaml).
    """
    try:
        import yaml
    except ImportError:
        print("  WARN: PyYAML not installed — cannot parse own-logs.yaml for CA cert.", file=sys.stderr)
        print("        Install with: pip install pyyaml", file=sys.stderr)
        print("        Or provide --tls-ca explicitly, or use --tls-no-verify.", file=sys.stderr)
        return None

    try:
        with open(yaml_path, 'r') as f:
            data = yaml.safe_load(f)

        for key in ('tls_ca_pem_contents', 'ca_cert_pem'):
            value = data.get(key)
            if value and '-----BEGIN CERTIFICATE-----' in value:
                return value.encode('utf-8')

    except Exception as e:
        print(f"  WARN: Could not parse {yaml_path}: {e}", file=sys.stderr)

    return None


def create_ssl_context(tls_cert=None, tls_key=None, tls_ca=None, tls_no_verify=False):
    """Create an SSL context for mTLS or simple TLS connections."""
    if not tls_cert and not tls_no_verify and not tls_ca:
        return None

    ctx = ssl.create_default_context()

    if tls_no_verify:
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

    if tls_ca:
        ctx.load_verify_locations(tls_ca)

    if tls_cert:
        ctx.load_cert_chain(certfile=tls_cert, keyfile=tls_key)

    return ctx


def send_request(endpoint, request_body, ssl_context=None, timeout=30):
    """POST an ExportLogsServiceRequest JSON to the OTLP endpoint."""
    data = json.dumps(request_body).encode('utf-8')
    req = urllib.request.Request(
        endpoint,
        data=data,
        headers={
            'Content-Type': 'application/json',
        },
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=ssl_context) as resp:
            return resp.status, resp.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        body = e.read().decode('utf-8', errors='replace') if e.fp else ''
        return e.code, body
    except urllib.error.URLError as e:
        return None, str(e.reason)


def main():
    parser = argparse.ArgumentParser(
        description='Replay OTLP dump files into Graylog',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Auto-discover certs from collector data dir
  %(prog)s --collector-dir /var/lib/graylog-collector \\
    --endpoint https://localhost:14401/v1/logs \\
    --receiver-type windowseventlog dump/*.ndjson.gz

  # Quick smoke test
  %(prog)s --collector-dir /var/lib/graylog-collector \\
    --endpoint https://localhost:14401/v1/logs \\
    --max-records 100 dump/*.ndjson.gz
""")
    parser.add_argument('files', nargs='+', help='Dump files (ndjson or ndjson.gz)')
    parser.add_argument('--endpoint', default='http://localhost:4318/v1/logs',
                        help='OTLP HTTP endpoint (default: http://localhost:4318/v1/logs)')
    parser.add_argument('--receiver-type', default=None,
                        help='Filter by collectorReceiverType (e.g., windowseventlog)')
    parser.add_argument('--batch-size', type=int, default=50,
                        help='Records per HTTP request (default: 50)')
    parser.add_argument('--delay', type=float, default=0.1,
                        help='Seconds between batches (default: 0.1)')
    parser.add_argument('--dry-run', action='store_true',
                        help='Parse and count records without sending')
    parser.add_argument('--max-records', type=int, default=None,
                        help='Stop after N records (default: all)')
    parser.add_argument('--quiet', action='store_true',
                        help='Only print summary')

    tls_group = parser.add_argument_group('TLS/mTLS options')
    tls_group.add_argument('--collector-dir', default=None,
                           help='Collector data directory — auto-discovers keys/signing.crt, '
                                'keys/signing.key, and CA cert from supervisor/own-logs.yaml')
    tls_group.add_argument('--tls-cert', default=None,
                           help='Client certificate for mTLS (PEM)')
    tls_group.add_argument('--tls-key', default=None,
                           help='Client private key for mTLS (PEM)')
    tls_group.add_argument('--tls-ca', default=None,
                           help='CA certificate to verify server (PEM)')
    tls_group.add_argument('--tls-no-verify', action='store_true',
                           help='Disable TLS certificate verification')
    args = parser.parse_args()

    # Resolve TLS material
    tls_cert = args.tls_cert
    tls_key = args.tls_key
    tls_ca = args.tls_ca
    ca_tempfile = None

    if args.collector_dir:
        if args.tls_cert or args.tls_key:
            parser.error("--collector-dir cannot be combined with --tls-cert/--tls-key")
        print(f"Resolving TLS from collector dir: {args.collector_dir}")
        tls_cert, tls_key, ca_tempfile = resolve_collector_tls(args.collector_dir)
        if ca_tempfile and not tls_ca:
            tls_ca = ca_tempfile

    ssl_context = create_ssl_context(
        tls_cert=tls_cert,
        tls_key=tls_key,
        tls_ca=tls_ca,
        tls_no_verify=args.tls_no_verify,
    )

    try:
        _replay(args, ssl_context)
    finally:
        # Clean up temp CA file
        if ca_tempfile and os.path.exists(ca_tempfile):
            os.unlink(ca_tempfile)


def _replay(args, ssl_context):
    batch = []
    total_sent = 0
    total_records = 0
    total_errors = 0
    start_time = time.time()

    receiver_filter = f" (filter: {args.receiver_type})" if args.receiver_type else ""
    print(f"Replaying to {args.endpoint}{receiver_filter}")
    print(f"Batch size: {args.batch_size}, delay: {args.delay}s")
    if args.dry_run:
        print("DRY RUN - not sending any data")
    print()

    for resource, scope, log_record in read_dump_records(args.files, args.receiver_type):
        batch.append((resource, scope, log_record))
        total_records += 1

        if args.max_records and total_records >= args.max_records:
            if not args.quiet:
                print(f"  Reached --max-records={args.max_records}, stopping")
            break

        if len(batch) >= args.batch_size:
            if args.dry_run:
                if not args.quiet:
                    print(f"  [dry-run] Would send batch of {len(batch)} records (total: {total_records})")
            else:
                request_body = build_request(batch)
                status, body = send_request(args.endpoint, request_body, ssl_context)
                total_sent += len(batch)
                if status == 200:
                    if not args.quiet:
                        print(f"  Sent {len(batch)} records (total: {total_sent})")
                else:
                    total_errors += 1
                    print(f"  ERROR: status={status} body={body[:200]}", file=sys.stderr)
                time.sleep(args.delay)
            batch = []

    # Send remaining records
    if batch:
        if args.dry_run:
            if not args.quiet:
                print(f"  [dry-run] Would send final batch of {len(batch)} records")
        else:
            request_body = build_request(batch)
            status, body = send_request(args.endpoint, request_body, ssl_context)
            total_sent += len(batch)
            if status == 200:
                if not args.quiet:
                    print(f"  Sent {len(batch)} records (total: {total_sent})")
            else:
                total_errors += 1
                print(f"  ERROR: status={status} body={body[:200]}", file=sys.stderr)

    elapsed = time.time() - start_time
    print()
    print(f"Done in {elapsed:.1f}s")
    print(f"  Records processed: {total_records}")
    if not args.dry_run:
        print(f"  Records sent: {total_sent}")
        print(f"  Errors: {total_errors}")


if __name__ == '__main__':
    main()
