if defined?(Unicorn::HttpServer)
  Unicorn::HttpServer.class_eval do
    NewRelic::Agent.logger.debug "Installing Unicorn worker hook."
    old_worker_loop = instance_method(:worker_loop)
    define_method(:worker_loop) do | worker |
      NewRelic::Agent.after_fork(:force_reconnect => true)
      old_worker_loop.bind(self).call(worker)
    end
  end
end
