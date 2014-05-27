File {
  owner => 0,
  group => 0,
  mode => 0644,
}

Exec {
  path => '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
}

class { "common":
  admin_email => "root@trinitynashville.org",
  region => $::region,
}

node /^web01\./ {
  # awsg tri.drewr ec2 \
  #  run-instances \
  #   --image-id ami-7fe7fe16 \
  #   --placement AvailabilityZone=us-east-1d \
  #   --count 1 \
  #   --instance-type m3.medium \
  #   --key-name triweb \
  #   --security-groups web
  #
  # awsg tri.drewr ec2 \
  #   create-tags \
  #     --resources i-598a610b \
  #     --tags Key=Name,Value=web01
  #
  # awsg tri.drewr ec2 \
  #    describe-instances \
  #     --filter "Name=tag:Name,Values=web01"
  class { "web": }
}

