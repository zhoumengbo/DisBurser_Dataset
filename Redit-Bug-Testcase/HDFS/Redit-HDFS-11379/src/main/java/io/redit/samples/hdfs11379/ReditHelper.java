package io.redit.samples.hdfs11379;

import io.redit.ReditRunner;
import io.redit.dsl.entities.Deployment;
import io.redit.dsl.entities.PathAttr;
import io.redit.dsl.entities.ServiceType;
import io.redit.exceptions.RuntimeEngineException;

import java.io.File;

public class ReditHelper {
    private static final int NN_HTTP_PORT = 50070;
    private static final int NN_RPC_PORT = 8020;
    public static int numOfNNs = 2;
    private static int numOfDNs = 3;
    private static int numOfJNs = 3;
    public static final String dir = "hadoop-2.7.0";
    public static String getHadoopHomeDir() { return "/hadoop/" + dir; }

    public static Deployment getDeployment(){
        String workDir = System.getProperty("user.dir");
        String compressedPath = workDir + "/../../../Benchmark/Hadoop/v2.7.0/" + dir + ".tar.gz";

        Deployment.Builder builder = Deployment.builder("sample-hdfs")
                .withService("hadoop-base")
                .applicationPath(compressedPath, "/hadoop",  PathAttr.COMPRESSED)
                .applicationPath("conf/hdfs-site.xml", getHadoopHomeDir() + "/etc/hadoop/hdfs-site.xml")
                .applicationPath("conf/core-site.xml", getHadoopHomeDir() + "/etc/hadoop/core-site.xml")
                .dockerImageName("mengpo1106/redit").dockerFileAddress("docker/Dockerfile", true)
                .environmentVariable("HADOOP_HOME", getHadoopHomeDir()).environmentVariable("HADOOP_HEAPSIZE_MAX", "1g")
                .libraryPath(getHadoopHomeDir() + "/share/hadoop/**/*.jar")
                .libraryPath(getHadoopHomeDir() + "/share/hadoop/**/*.java")
                .logDirectory(getHadoopHomeDir() + "/logs")
                .serviceType(ServiceType.JAVA).and();

        addRuntimeLibsToDeployment(builder, getHadoopHomeDir());

        builder.withService("nn", "hadoop-base").tcpPort(NN_HTTP_PORT, NN_RPC_PORT)
                .initCommand(getHadoopHomeDir() + "/bin/hdfs namenode -bootstrapStandby")
                .startCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh start namenode")
                .stopCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh stop namenode").and()
                .nodeInstances(numOfNNs, "nn", "nn", true)
                .withService("dn", "hadoop-base")
                .startCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh start datanode")
                .stopCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh stop datanode").and()
                .nodeInstances(numOfDNs, "dn", "dn", true)
                .withService("jn", "hadoop-base")
                .startCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh start journalnode")
                .stopCommand(getHadoopHomeDir() + "/sbin/hadoop-daemon.sh stop journalnode").and()
                .nodeInstances(numOfJNs, "jn", "jn", false);

        builder.node("nn1").initCommand(getHadoopHomeDir() + "/bin/hdfs namenode -format").and()
                .testCaseEvents("E1", "E2", "E3", "X1").runSequence("E1 * X1 * E2 * E3");;

        addInstrumentablePath(builder, "/share/hadoop/hdfs/hadoop-hdfs-2.8.0.jar");

        return builder.build();
    }

    //Add the runtime library to the deployment
    private static void addRuntimeLibsToDeployment(Deployment.Builder builder, String hadoopHome) {
        for (String cpItem: System.getProperty("java.class.path").split(":")) {
            if (cpItem.contains("aspectjrt") || cpItem.contains("reditrt")) {
                String fileName = new File(cpItem).getName();
                builder.service("hadoop-base")
                        .applicationPath(cpItem, hadoopHome + "/share/hadoop/common/" + fileName, PathAttr.LIBRARY).and();
            }
        }
    }

    public static void addInstrumentablePath(Deployment.Builder builder, String path) {
        String[] services = {"hadoop-base", "nn", "dn", "jn"};
        for (String service: services) {
            builder.service(service).instrumentablePath(getHadoopHomeDir() + path).and();
        }
    }

    public static void startNodes(ReditRunner runner) throws RuntimeEngineException, InterruptedException {
        if (numOfNNs > 1) {
            // wait for journal nodes to come up
            Thread.sleep(5000);
        }
        runner.runtime().startNode("nn1");
        Thread.sleep(5000);
        if (numOfNNs > 1) {
            for (int nnIndex = 2; nnIndex <= numOfNNs; nnIndex++) {
                runner.runtime().startNode("nn" + nnIndex);
            }
        }
        for (String node : runner.runtime().nodeNames())
            if (node.startsWith("dn")) runner.runtime().startNode(node);
    }
}
