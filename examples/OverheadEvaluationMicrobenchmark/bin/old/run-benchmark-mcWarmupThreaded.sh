#!/bin/bash

BINDIR=$(dirname $0)/
BASEDIR=${BINDIR}../

SLEEPTIME=30
NUM_LOOPS=1
THREADS=16
TOTALCALLS=1000000
RECORDEDCALLS=100000
METHODTIME=500000

TIME=`expr ${METHODTIME} \* ${TOTALCALLS} / 1000000000 \* 4 \* ${NUM_LOOPS} + ${SLEEPTIME} \* 4 \* ${NUM_LOOPS}`
echo "Experiment will take circa ${TIME} seconds."

# determine correct classpath separator
CPSEPCHAR=":" # default :, ; for windows
if [ ! -z "$(uname | grep -i WIN)" ]; then CPSEPCHAR=";"; fi
# echo "Classpath separator: '${CPSEPCHAR}'"

RESULTSDIR="${BASEDIR}tmp/results-mcWarmupThreaded/"
echo "Removing and recreating '$RESULTSDIR'"
rm -rf ${RESULTSDIR} && mkdir ${RESULTSDIR}
mkdir -p ${BASEDIR}build/META-INF/

# Clear kieker.log and initialize logging
rm -f ${BASEDIR}kieker.log

RESULTSFN="${RESULTSDIR}results.csv"
# Write csv file headers:
CSV_HEADER="configuration;iteration;order_index;threadid;duration_nsec"
echo ${CSV_HEADER} > ${RESULTSFN}

AOPXML_PATH="${BASEDIR}build/META-INF/aop.xml"
AOPXML_INSTR_EMPTYPROBE="${BASEDIR}configuration/MonitoredApplication/aop-emptyProbe.xml"
AOPXML_INSTR_PROBE="${BASEDIR}configuration/MonitoredApplication/aop-probe.xml"

KIEKER_MONITORING_CONF_NOLOGGING="${BASEDIR}configuration/MonitoredApplication/kieker.monitoring-nologging.properties"
KIEKER_MONITORING_CONF_LOGGING="${BASEDIR}configuration/MonitoredApplication/kieker.monitoring-logging.properties"

JAVAARGS="-server"
#JAVAARGS="${JAVAARGS} -d64"
#JAVAARGS="${JAVAARGS} -XX:+PrintCompilation -XX:+PrintInlining"
#JAVAARGS="${JAVAARGS} -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation"
#JAVAARGS="${JAVAARGS} -Djava.compiler=NONE"
MAINCLASSNAME=kieker.evaluation.benchmark.WarmupThreaded
CLASSPATH=$(ls lib/*.jar | tr "\n" "${CPSEPCHAR}")build/
#echo "Classpath: ${CLASSPATH}"

JAVAARGS_NOINSTR="${JAVAARGS}"
JAVAARGS_LTW="${JAVAARGS} -javaagent:${BASEDIR}lib/aspectjweaver.jar -Dorg.aspectj.weaver.showWeaveInfo=false -Daj.weaving.verbose=false"
JAVAARGS_KIEKER="-Djava.util.logging.config.file=${BASEDIR}configuration/logging.properties -Dkieker.monitoring.storeInJavaIoTmpdir=false -Dkieker.monitoring.customStoragePath=${BASEDIR}tmp/"
JAVAARGS_INSTR_EMPTYPROBE="${JAVAARGS_LTW} ${JAVAARGS_KIEKER} -Dkieker.monitoring.configuration=${KIEKER_MONITORING_CONF_NOLOGGING}"
JAVAARGS_INSTR_NOLOGGING="${JAVAARGS_LTW} ${JAVAARGS_KIEKER} -Dkieker.monitoring.configuration=${KIEKER_MONITORING_CONF_NOLOGGING}"
JAVAARGS_INSTR_LOGGING="${JAVAARGS_LTW} ${JAVAARGS_KIEKER} -Dkieker.monitoring.configuration=${KIEKER_MONITORING_CONF_LOGGING}"

## Write configuration
uname -a >${RESULTSDIR}configuration.txt
java ${JAVAARGS} -version 2>>${RESULTSDIR}configuration.txt
echo "JAVAARGS: ${JAVAARGS}" >>${RESULTSDIR}configuration.txt
echo "" >>${RESULTSDIR}configuration.txt
echo "Runtime: circa ${TIME} seconds" >>${RESULTSDIR}configuration.txt
echo "" >>${RESULTSDIR}configuration.txt
echo "SLEEPTIME=${SLEEPTIME}" >>${RESULTSDIR}configuration.txt
echo "NUM_LOOPS=${NUM_LOOPS}" >>${RESULTSDIR}configuration.txt
echo "TOTALCALLS=${TOTALCALLS}" >>${RESULTSDIR}configuration.txt
echo "RECORDEDCALLS=${RECORDEDCALLS}" >>${RESULTSDIR}configuration.txt
echo "METHODTIME=${METHODTIME}" >>${RESULTSDIR}configuration.txt
echo "THREADS=${THREADS}" >>${RESULTSDIR}configuration.txt
sync

## Execute Benchmark

for ((i=1;i<=${NUM_LOOPS};i+=1)); do
    echo "## Starting iteration ${i}/${NUM_LOOPS}"

    # 1 No instrumentation
    echo " # ${i}.1 No instrumentation"
    java  ${JAVAARGS_NOINSTR} -cp "${CLASSPATH}" ${MAINCLASSNAME} \
        --output-filename ${RESULTSFN} \
        --configuration-id "noinstr;${i};0" \
        --totalcalls ${TOTALCALLS} \
        --recordedcalls ${RECORDEDCALLS} \
        --methodtime ${METHODTIME} \
        --totalthreads ${THREADS}
    [ -f ${BASEDIR}hotspot.log ] && mv ${BASEDIR}hotspot.log ${RESULTSDIR}hotspot_${i}_1.log
    sync
    sleep ${SLEEPTIME}

    # 2 Empty probe
    echo " # ${i}.2 Empty probe"
    cp ${AOPXML_INSTR_EMPTYPROBE} ${AOPXML_PATH}
    java  ${JAVAARGS_INSTR_EMPTYPROBE} -cp "${CLASSPATH}" ${MAINCLASSNAME} \
        --output-filename ${RESULTSFN} \
        --configuration-id "empty_probe;${i};1" \
        --totalcalls ${TOTALCALLS} \
        --recordedcalls ${RECORDEDCALLS} \
        --methodtime ${METHODTIME} \
        --totalthreads ${THREADS}
    rm -f ${AOPXML_PATH}
    [ -f ${BASEDIR}hotspot.log ] && mv ${BASEDIR}hotspot.log ${RESULTSDIR}hotspot_${i}_2.log
    sync
    sleep ${SLEEPTIME}

    # 3 No logging
    echo " # ${i}.3 No logging (null writer)"
    cp ${AOPXML_INSTR_PROBE} ${AOPXML_PATH}
    java  ${JAVAARGS_INSTR_NOLOGGING} -cp "${CLASSPATH}" ${MAINCLASSNAME} \
        --output-filename ${RESULTSFN} \
        --configuration-id "no_logging;${i};2" \
        --totalcalls ${TOTALCALLS} \
        --recordedcalls ${RECORDEDCALLS} \
        --methodtime ${METHODTIME} \
        --totalthreads ${THREADS}
    rm -f ${AOPXML_PATH}
    [ -f ${BASEDIR}hotspot.log ] && mv ${BASEDIR}hotspot.log ${RESULTSDIR}hotspot_${i}_3.log
    sync
    sleep ${SLEEPTIME}

    # 4 Logging
    echo " # ${i}.4 Logging"
    cp ${AOPXML_INSTR_PROBE} ${AOPXML_PATH}
    java  ${JAVAARGS_INSTR_LOGGING} -cp "${CLASSPATH}" ${MAINCLASSNAME} \
        --output-filename ${RESULTSFN} \
        --configuration-id "logging;${i};3" \
        --totalcalls ${TOTALCALLS} \
        --recordedcalls ${RECORDEDCALLS} \
        --methodtime ${METHODTIME} \
        --totalthreads ${THREADS}
    rm -f ${AOPXML_PATH}
    rm -rf ${BASEDIR}tmp/tpmon-*
    [ -f ${BASEDIR}hotspot.log ] && mv ${BASEDIR}hotspot.log ${RESULTSDIR}hotspot_${i}_4.log
    sync
    sleep ${SLEEPTIME}

done

mv ${BASEDIR}kieker.log ${RESULTSDIR}kieker.log
${BINDIR}run-r-mcWarmupThreaded.sh
[ -f ${RESULTSDIR}hotspot_1_1.log ] && grep "<task " ${RESULTSDIR}hotspot_*.log >${RESULTSDIR}log.log
[ -f ${BASEDIR}nohup.out ] && mv ${BASEDIR}nohup.out ${RESULTSDIR}