Host.blueprint do
 host { "host-#{sn}" }
 message_count { rand(50000) }
end

Message.blueprint do
 message { Faker::Lorem.words(15) }
 facility { rand(15) }
 level { rand(8) }
 host { "host-#{sn}" }
 created_at { Time.now.to_i }
 deleted { false }
end
