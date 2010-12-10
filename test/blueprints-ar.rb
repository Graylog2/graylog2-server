require 'machinist/active_record'
require 'sham'

Sham.sn { rand(500000) }

Hostgroup.blueprint do
 name { "group-#{sn}" }
end

FavoritedStream.blueprint do
 stream_id { sn }
 user_id { sn }
end

Stream.blueprint do
  title { "stream-#{sn}" }
end
