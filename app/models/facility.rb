class Facility < ActiveRecord::Base

  validates_presence_of :number
  validates_presence_of :title
  validates_numericality_of :number

  def self.standards
    { -1 => "GELF (-1)",
      0 => "Kernel (0)",
      1 => "User-Level (1)",
      2 =>  "Mail (2)",
      3 => "System Daemon (3)",
      4 => "Security/Authorization (4)",
      5 => "Syslogd (5)",
      6 => "Line Printer (6)",
      7 => "News (7)",
      8 => "UUCP (8)",
      9 => "Clock (9)",
      10 => "Security/Authorization (10)",
      11 => "FTP (11)",
      12 => "NTP (12)",
      13 => "Log Audit (13)",
      14 => "Log Alert (14)",
      15 => "Clock (15)",
      16 => "local0 (16)",
      17 => "local1 (17)",
      18 => "local2 (18)",
      19 => "local3 (19)",
      20 => "local4 (20)",
      21 => "local5 (21)",
      22 => "local6 (22)",
      23 => "local7 (23)" }
  end

  def self.user_defined
    ret = Hash.new
    self.all.each do |f|
      ret[f.number] = f.title + " (#{f.number})"
    end

    return ret
  end

  def self.get_all
    self.standards.merge(self.user_defined)
  end

  def self.ordered_for_select
    self.get_all.map { |n,f| [f,n] }
  end

  def self.to_human(facility_id)
    self.get_all[facility_id.to_i].blank? ? "Unknown (#{facility_id})" : self.get_all[facility_id.to_i]
  end

end
