#! /bin/sh

### BEGIN INIT INFO
# Provides:          renjin-worker
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Renjin Package Build, Test Worker
# Description:       Builds packages, tests more
### END INIT INFO

# Carry out specific functions when asked to by the system
case "$1" in
  start)
    echo "Starting Renjin Worker "

    su --login alex --command "/home/alex/start-renjin-worker"

    ;;
  stop)
    echo "Stopping Renjin Worker"

    killall java
    ;;
  *)
    echo "Usage: /etc/init.d/renjin-worker {start|stop}"
    exit 1
    ;;
esac

exit 0