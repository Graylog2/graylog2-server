GC.copy_on_write_friendly = true if GC.respond_to?(:copy_on_write_friendly=)
GC.enable_stats if GC.respond_to?(:enable_stats)  # for NewRelic RPM
