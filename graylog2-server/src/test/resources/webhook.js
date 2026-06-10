const http = require('http');

process.on('SIGTERM', () => {
  console.log('SIGTERM signal received.');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('SIGINT signal received.');
  process.exit(0);
});

let requests = [];

http.createServer(function (req, res) {

  const { method, url, headers } = req;
  const timestamp = Date.now();

  let bodyChunks = [];
  req.on('data', chunk => {
    bodyChunks.push(chunk);
  }).on('end', () => {
    requests.push(
      { method, url, timestamp, headers, body: Buffer.concat(bodyChunks).toString() },
    );
  });

  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.write('OK');
  res.end();

}).listen(8000);

http.createServer(function (req, res) {
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.write(JSON.stringify(requests));
  res.end();
}).listen(8001);
