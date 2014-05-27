define daemontools::service (
  $svc          = $title,
  $svchome      = $daemontools::params::svchome,
  $run          = undef,
  $run_tmpl     = $daemontools::params::run_tmpl,
  $run_log      = undef,
  $run_log_tmpl = $daemontools::params::run_log_tmpl,
  $log_user     = $daemontools::params::log_user,
  $log_group    = $daemontools::params::log_group,
  $log_path     = $daemontools::params::log_path,
  $down         = absent,
) {
  $svc_dir = "${svchome}/${svc}"
  $svc_log_dir = "${svc_dir}/log"
  $active_dir = "/etc/service/${$svc}"
  $run_bin = "${svc_dir}/run"
  $run_log_bin = "${svc_log_dir}/run"
  $downfile = "${svc_dir}/down"

  if ($log_path == undef) {
    $log_path_real = "${svc_log_dir}/main"
  }
  else {
    $log_path_real = $log_path
  }

  if ($run == undef) {
    $run_content = template($run_tmpl)
  }
  else {
    $run_content = $run
  }

  if ($run_log == undef) {
    $run_log_content = template($run_log_tmpl)
  }
  else {
    $run_log_content = $run_log
  }

  file { [$svc_dir,
          $svc_log_dir,
          ]:
    ensure  => directory,
    owner   => "root",
    group   => "root",
    mode    => 0755,
  }

  file { $log_path_real:
    ensure  => directory,
    owner   => $log_user,
    group   => $log_group,
    mode    => 0755,
  }

  file { $run_bin:
    owner   => "root",
    group   => "root",
    mode    => 0755,
    content => $run_content,
    require => File[$svc_dir, $run_log_bin],
  }

  # Presence of a `down` file will cause supervise(1) not to run `run`
  # automatically
  file { $downfile:
    ensure  => $down,
    owner   => "root",
    group   => "root",
    mode    => 0644,
    content => "",
    require => File[$svc_dir],
  }
    
  file { $run_log_bin:
    owner   => "root",
    group   => "root",
    mode    => 0755,
    content => $run_log_content,
    require => File["${svc_log_dir}"],
  }

  file { $active_dir:
    ensure => "link",
    target => "${svc_dir}",
    require => File[$run_bin, $run_log_bin]
  }

  exec { "/usr/bin/svc -t ${svc_dir}":
    subscribe => File[$run_bin],
    refreshonly => true
  }

  exec { "/usr/bin/svc -t ${svc_log_dir}":
    subscribe => File[$run_log_bin],
    refreshonly => true
  }
}
