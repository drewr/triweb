class web (
  $jetty_version = "jetty-distribution-9.2.1.v20140609",
  $archive = "jetty-distribution-9.2.1.v20140609.tar.gz",
  $urlpre = "https://s3.amazonaws.com/deploy.trinitynashville.org",
  $user = "deploy",
  $group = "deploy",
  $home = "/home/deploy",
){
  $url = "${urlpre}/${archive}"

  file { $home:
    ensure => directory,
    owner => $user,
    group => $group,
  }

  user { $user:
    managehome => true,
    ensure => present,
  }

ssh_authorized_key { $user:
    type => 'ssh-rsa',
    key  => 'AAAAB3NzaC1yc2EAAAADAQABAAABAQCqR1DVZtLibe37zsSz7H4iqbHllTFUYfN0reDd/gCRHvrt7+RBpOxrUthSac+OxY+n/ORll62VGqcVmHOeAY6X4YQVzrSMdwDuljw7U0yg21kZGhVfiyfsWKaPWDC0d/B+9kpwB/67Q3KNACIBWgV1jiI6za5AiEn+1LCQJbSxS2u4E8cC3XBcF6Ifmxiw40GM44AFpTMwqYn3zsK2eM02m7nn49UpbLa417+YkqJZ94La1f3Ag0CpeZaA1qFIM6eEq/olxgOcby7tJHJ+/AQ9oMnn7WCHo51+63gCR6bj7z7TgGGwRiSUnNBbMfQ6E2QT1OiXASFuVILWOs5dbhGj',
  }  

  wget::fetch { "jetty":
    source => $url,
    destination => "${home}/${archive}",
    require => [User[$user],
                File[$home]],
  }

  exec { "install-jetty":
    command => "/bin/tar zxpf ${archive}",
    cwd => $home,
    user => $user,
    onlyif => "test -f ${home}/${archive}",
    path    => "/usr/bin:/usr/sbin:/bin:/usr/local/bin:/opt/local/bin",
    require => Wget::Fetch["jetty"],
  }

  file { "${home}/jetty":
    ensure => symlink,
    target => $jetty_version,
    owner => $user,
    group => $group,
  }

  daemontools::service { "jetty":
    run => template("web/run-jetty.erb"),
    svchome => "/etc/svc",
    down => false,
    require => Exec["install-jetty"],
  }
}
