class daemontools::params {
  $svchome = "/etc/svc"
  $log_user = "nobody"
  $log_group = "nogroup"
  $log_path = undef
  $run_tmpl = "${module_name}/svc/run.erb"
  $run_log_tmpl = "${module_name}/svc/log/run.erb"
}
