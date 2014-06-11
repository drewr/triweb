class dropbox (
  $user = "trinity",
  $group = "trinity",
  $home = "/home/trinity",
  $archive = "dropbox-lnx.x86_64-2.8.3.tar.gz",
  $urlpre = "https://s3.amazonaws.com/deploy.trinitynashville.org",
) {
  $url = "${urlpre}/${archive}"
 
  wget::fetch { "dropbox":
    source => $url,
    destination => $home,
  }

  exec { "install-dropbox":
    command => "/bin/sh -c \"cd ${home}; gzip -cd ${archive} | tar xf -\"",
    user => $user,
    path    => "/usr/bin:/usr/sbin:/bin:/usr/local/bin:/opt/local/bin",
    require => Wget::Fetch["dropbox"],
  }

  daemontools::service { "dropbox-${user}":
    run => template("dropbox/run.erb"),
    svchome => "/etc/svc",
    down => false,
    require => Exec["install-dropbox"],
  }
}
