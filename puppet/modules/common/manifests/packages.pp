class common::packages {
  package { "autoconf": }
  package { "automake": }
  package { "build-essential": }
  package { "curl": }
  package { "djbdns": }
  package { "emacs23-nox": }
  package { "git": }
  package { "htop": }
  package { "maven": }
  package { "ntp": }
  package { "openjdk-7-jdk": }
  package { "pv": }
  package { "s3cmd": }
  package { "sysstat": }
  package { "tmux": }
  package { "traceroute": }
  package { "wget": }
  package { "zip": }
  package { "zsh": }

  package { "openjdk-6-jre":          ensure => absent }
  package { "openjdk-6-jre-headless": ensure => absent }
  package { "openjdk-6-jre-lib":      ensure => absent }
}
