class common (
  $admin_email = "root@trinitynashville.org",
  $region = $::region,
) {
  class { "common::users": }
  class { "common::packages": }

  class { "common::boot":
    region => $region,
  }

  class { "mail":
    admin_email => $admin_email,
  }

  class { "daemontools": }
}

