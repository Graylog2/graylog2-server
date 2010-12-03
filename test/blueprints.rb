require 'machinist/mongo_mapper'

Host.blueprint do
 host { "host-#{sn}" }
 message_count { rand(50000) }
end

Message.blueprint do
 message { "lalalalala" }
 facility { rand(15) }
 level { rand(8) }
 host { "host-#{sn}" }
 created_at { Time.now.to_i }
 deleted { false }
end
