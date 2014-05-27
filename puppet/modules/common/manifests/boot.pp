class common::boot(
  $region = undef,
) {
  file { "/usr/local/bin/converge":
    owner => "root",
    group => "root",
    mode => '0755',
    content => template("common/boot/converge.erb"),
  }
}
