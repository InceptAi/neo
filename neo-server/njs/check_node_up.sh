NEO_HOME="/home/vivek/Work/neo/neo-server/njs"
pid=`ps aux | grep node | grep relay | tr -s " " | cut -d " " -f 2`

notify_restart () {
	BODY="Node server was dead. Restarted."
    echo ${BODY}| mail -s "Node server was dead, restarted" arunesh@obiai.tech vivek@obiai.tech
}

if [ -z $pid ]; then
	#email arunesh and vivek
	echo "Node is not running; restart it"
	killall node
	cd $NEO_HOME
	nohup node relay.js &
	notify_restart
else
	echo "Node server up and running"
fi
