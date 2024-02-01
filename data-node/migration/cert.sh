#!/bin/sh
# Root CA
openssl genrsa -out root-ca-key.pem 2048
openssl req -new -x509 -sha256 -key root-ca-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=root" -out root-ca.pem -days 730
# Admin cert
openssl genrsa -out admin-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in admin-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out admin-key.pem
openssl req -new -key admin-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=A" -out admin.csr
openssl x509 -req -in admin.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out admin.pem -days 730
# Node cert 1
openssl genrsa -out node1-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in node1-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out node1-key.pem
openssl req -new -key node1-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=opensearch1" -out node1.csr
echo 'subjectAltName=DNS:opensearch1' > node1.ext
openssl x509 -req -in node1.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out node1.pem -days 730 -extfile node1.ext
# Node cert 2
openssl genrsa -out node2-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in node2-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out node2-key.pem
openssl req -new -key node2-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=opensearch2" -out node2.csr
echo 'subjectAltName=DNS:opensearch2' > node2.ext
openssl x509 -req -in node2.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out node2.pem -days 730 -extfile node2.ext
# Node cert 3
openssl genrsa -out node3-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in node3-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out node3-key.pem
openssl req -new -key node3-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=opensearch3" -out node3.csr
echo 'subjectAltName=DNS:opensearch3' > node3.ext
openssl x509 -req -in node3.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out node3.pem -days 730 -extfile node3.ext
# Client cert
#openssl genrsa -out client-key-temp.pem 2048
#openssl pkcs8 -inform PEM -outform PEM -in client-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out client-key.pem
#openssl req -new -key client-key.pem -subj "/C=CA/ST=ONTARIO/L=TORONTO/O=ORG/OU=UNIT/CN=client.dns.a-record" -out client.csr
#echo 'subjectAltName=DNS:client.dns.a-record' > client.ext
#openssl x509 -req -in client.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out client.pem -days 730 -extfile client.ext
# Cleanup
rm admin-key-temp.pem
rm admin.csr
rm node1-key-temp.pem
rm node1.csr
rm node1.ext
rm node2-key-temp.pem
rm node2.csr
rm node2.ext
rm node3-key-temp.pem
rm node3.csr
rm node3.ext
#rm client-key-temp.pem
#rm client.csr
#rm client.ext


keytool -import -trustcacerts -alias opensearch1 -file node1.pem -keystore keystore.jks
keytool -import -trustcacerts -alias opensearch2 -file node2.pem -keystore keystore.jks
keytool -import -trustcacerts -alias opensearch3 -file node3.pem -keystore keystore.jks
keytool -import -trustcacerts -alias admin -file admin.pem -keystore keystore.jks
keytool -import -trustcacerts -alias root -file root-ca.pem -keystore keystore.jks
 
