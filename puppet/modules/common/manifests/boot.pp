class common::boot(
) {
  file { "/usr/local/bin/converge":
    owner => "root",
    group => "root",
    mode => '0755',
    content => template("common/boot/converge.erb"),
  }
}
