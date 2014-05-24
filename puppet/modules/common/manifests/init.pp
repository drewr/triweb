class common (
  $admin_email = "dev_ops@elasticsearch.com",
) {
  class { "common::users": }
  class { "common::packages": }

  class { "common::boot":
    region => $::region,
  }

  class { "mail":
    admin_email => $admin_email,
  }

  class { "daemontools": }
}

