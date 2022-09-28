import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) throws InterruptedException {

        // set db with host and such
        final String serverURL = "http://127.0.0.1:8086", username = "root", password = "root";
        final InfluxDB influxDB = InfluxDBFactory.connect(serverURL, username, password);

        // this is how you can create and set databases ( i think this produces an error in jenkins).
        String databaseName = "javaInfluxDB";
        //influxDB.query(new Query("CREATE DATABASE " + databaseName));
        influxDB.setDatabase(databaseName);

        // Create "Retention policy". No idea how this affects us or what it even does
        String retentionPolicyName = "one_day_only";
        influxDB.query(new Query("CREATE RETENTION POLICY " + retentionPolicyName
                + " ON " + databaseName + " DURATION 1d REPLICATION 1 DEFAULT"));
        influxDB.setRetentionPolicy(retentionPolicyName);

        // enable batch writers. best practice apparently, also supposedly improves performance
        influxDB.enableBatch(
                BatchOptions.DEFAULTS
                        .threadFactory(runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setDaemon(true);
                            return thread;
                        })
        );

        // signal to terminate when no longer in use
        Runtime.getRuntime().addShutdownHook(new Thread(influxDB::close));

        // Write points n influx
        addPoint(influxDB, "CPU_A", 0, 12);
        addPoint(influxDB, "CPU_B", 6, 48);
        addPoint(influxDB, "CPU_C", 576, 120);

        // Wwait in case of delay
        Thread.sleep(5_000L);

         //can query results directly from here. NOTE: CANT INSERT VIA THIS METHOD
        QueryResult queryResult = influxDB.query(new Query("SELECT * FROM Tests"));

        System.out.println(queryResult);

    }

    static void addPoint(InfluxDB idb, String cpuName, int mtest, int atest){
        idb.write(Point.measurement("Tests")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .tag("Machine", cpuName)
                .addField("MTests", mtest)
                .addField("ATests", atest)
                .build());
    }



}
