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
