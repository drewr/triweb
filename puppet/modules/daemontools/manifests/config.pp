class daemontools::config (
  $svchome = undef,
) {
  package { "daemontools": }
  package { "daemontools-run": }

  service { "svscan":
    ensure => running,
    provider => "upstart",
    require => Package["daemontools"],
  }

  file { $svchome:
    ensure  => directory,
    owner   => "root",
    group   => "root",
    mode    => 0755,
  }
}
