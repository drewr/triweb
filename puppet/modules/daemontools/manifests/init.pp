class daemontools (
  $svchome = $daemontools::params::svchome,
) inherits daemontools::params {
  class { "daemontools::config":
    svchome => $svchome,
  }
}
