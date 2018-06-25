#!/bin/sh

INIT_DIR=$(dirname "$0")
RES_DIR=results
LOG_DIR=log
CONF=${INIT_DIR}/todocorp.conf

mkdir -p $RES_DIR
mkdir -p $LOG_DIR

eval_classifiers() {
    clname=$1
    classifiers=$2
    metaset=$3
    log=$LOG_DIR/${clname}.log
    echo "evaluation set ${clname}, classifiers: ${classifiers}, meta set: ${metaset}, config: $CONF" > $log
    nohup ./bin/todotask evaluate -c $CONF -l INFO \
	  --metaset $metaset --classifiers $classifiers \
	  -o $RES_DIR/${clname}.xls >> $log 2>&1 &
}

case $1 in
    clean)
	rm $RES_DIR/*
	rm $LOG_DIR/*
	;;

    sanity)
	# sanity test
	eval_classifiers test-res zeror set-best
	;;

    best)
	# single best preforming model
	eval_classifiers j48 j48 set-best
	;;

    long)
	# single best preforming model
	eval_classifiers random-forest random-forest set-best

	# long running
	for i in fast lazy meta tree slow really-slow ; do
	    eval_classifiers $i $i set-compare
	done
	;;

    *)
	echo "usage: $0 <clean|sanity|best|long>"
	;;
esac
