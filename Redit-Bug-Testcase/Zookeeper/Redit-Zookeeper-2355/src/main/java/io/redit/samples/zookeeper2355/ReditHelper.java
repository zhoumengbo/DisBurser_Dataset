package io.redit.samples.zookeeper2355;

import io.redit.ReditRunner;
import io.redit.dsl.entities.Deployment;
import io.redit.dsl.entities.PathAttr;
import io.redit.dsl.entities.ServiceType;
import io.redit.exceptions.RuntimeEngineException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ReditHelper {
    public static int numOfServers = 3;
    public static final int HTTP_PORT = 2181;
    public static final String dir = "zookeeper-3.5.3-beta";
    public static String getZookeeperHomeDir(){
        return "/zookeeper/" + dir;
    }

    public static Deployment getDeployment() {
        String workDir = System.getProperty("user.dir");
        String compressedPath = workDir + "/../../../Benchmark/Zookeeper/v3.5.3/" + dir + ".tar.gz";

        Deployment.Builder builder = Deployment.builder("sample-zookeeper")
                .withService("zookeeper")
                .applicationPath(compressedPath, "/zookeeper",  PathAttr.COMPRESSED)
                .applicationPath("conf/zoo.cfg", getZookeeperHomeDir() + "/conf/zoo.cfg")
                .dockerImageName("mengpo1106/redit").dockerFileAddress("docker/Dockerfile", true)
                .libraryPath(getZookeeperHomeDir() + "/lib/*.jar")
                .logDirectory(getZookeeperHomeDir() + "/logs")
                .environmentVariable("ZOO_LOG_DIR", "/zookeeper/" + dir + "/logs")
                .serviceType(ServiceType.JAVA).and();

        builder.withService("server", "zookeeper").tcpPort(HTTP_PORT).and()
                .nodeInstances(numOfServers, "server", "server", true)
                .node("server1").applicationPath("conf/server1/myid", getZookeeperHomeDir() + "/zkdata/myid").and()
                .node("server2").applicationPath("conf/server2/myid", getZookeeperHomeDir() + "/zkdata/myid").and()
                .node("server3").applicationPath("conf/server3/myid", getZookeeperHomeDir() + "/zkdata/myid").and();

        builder.node("server1").and().testCaseEvents("E1", "E2", "E3", "E4", "E5", "X1", "X2").runSequence("E1 * E2 * X1 * E3 * X2 * E4 * E5");
        return builder.build();
    }

    public static void startNodes(ReditRunner runner) throws RuntimeEngineException, InterruptedException {
        for (int index = 1; index <= numOfServers; index++) {
            runner.runtime().startNode("server" + index);
            Thread.sleep(1000);
        }
    }

    public static ArrayList<Object> getFileRW() throws IOException {
        ArrayList<Object> RWs = new ArrayList<>();
        RWs.add(new FileReader("conf/zoo.cfg"));
        RWs.add(new FileWriter("conf/zoo.cfg", true));
        return RWs;
    }
}
