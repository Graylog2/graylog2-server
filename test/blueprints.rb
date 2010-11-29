require 'machinist/mongo_mapper'

Host.blueprint do
 host { "host-#{sn}" }
 message_count { rand(50000) }
end
