class mail(
  $admin_email = "root@trinitynashville.org",
  $fqdn = $::fqdn,
) {
  package { "qmail": }
  package { "mailutils": }

  file { "mail-admin":
    owner => "root",
    group => "root",
    mode => 0644,
    path => "/var/lib/qmail/alias/.qmail-root",
    content => "&${admin_email}\n",
    require => Package["qmail"]
  }

  file { "/var/lib/qmail/alias/.qmail-default":
    owner => "root",
    group => "root",
    mode => 0644,
    content => "&root\n",
    require => Package["qmail"],
  }

  exec { "restart-qmail":
    command => "/usr/bin/svc -t /etc/service/qmail-send; /usr/bin/svc -t /etc/service/qmail-smtpd",
    user => "root",
    group => "root",
    refreshonly => true,
  }

  file { ["/etc/qmail/locals",
          "/etc/qmail/rcpthosts",
          "/etc/qmail/me"]:
    owner => "root",
    group => "root",
    mode => 0644,
    content => "${fqdn}\n",
    require => Package["qmail"],
    notify => Exec["restart-qmail"],
  }
}
