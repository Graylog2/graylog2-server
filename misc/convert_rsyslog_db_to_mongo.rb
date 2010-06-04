#!/usr/bin/ruby

require 'rubygems'
require 'mysql'
require 'mongo'
require 'time'


begin
  dbh = Mysql.real_connect("localhost", "root", "123", "Syslog")
  res = dbh.query("SELECT Message, ReceivedAt, FromHost, Facility, Priority FROM SystemEvents ORDER BY ReceivedAt ASC");

  while row = res.fetch_row do
    begin
      message = row[0]
      date = row[1]
      created_at = Time.parse(row[1]).to_i
      host = row[2]
      facility =  row[3].to_i
      level = row[4].to_i

      # store in mongo
      db = Mongo::Connection.new.db("graylog2")
      coll = db.collection("messages")
      doc = {"message" => message, "date" => date, "created_at" => created_at, "host" => host, "facility" => facility, "level" => level}
      coll.insert(doc)
    rescue Exception => e
      puts "Skipped. (#{e})"
      next
    end
  end
rescue Exception => e
  puts "Error: #{e}"
ensure
  dbh.close if dbh
end
