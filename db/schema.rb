# This file is auto-generated from the current state of the database. Instead of editing this file, 
# please use the migrations feature of Active Record to incrementally modify your database, and
# then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your database schema. If you need
# to create the application database on another system, you should be using db:schema:load, not running
# all the migrations from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended to check this file into your version control system.

ActiveRecord::Schema.define(:version => 20100524020921) do

  create_table "blacklisted_terms", :force => true do |t|
    t.string   "term"
    t.integer  "blacklist_id"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "blacklists", :force => true do |t|
    t.string   "title"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

  create_table "streams", :force => true do |t|
    t.string   "title"
    t.string   "filter_host"
    t.string   "filter_message"
    t.integer  "filter_severity"
    t.integer  "filter_level"
    t.datetime "created_at"
    t.datetime "updated_at"
  end

end
