package seaweedfs.client;

import java.util.List;

public class FilerGrpcClientTest {
    public static void main(String[] args) {

        String host = "cdh2";
        int grpcPort = 10000 + 8888;

        FilerGrpcClient filerGrpcClient = new FilerGrpcClient(host, grpcPort);
        FilerClient filerClient = new FilerClient(filerGrpcClient);

        //查看目录下文件
        List<FilerProto.Entry> entries = filerClient.listEntries("/");
        entries.forEach(System.out::println);
        System.out.println("=================================================");

        //查看文件描述
        FilerProto.Entry log = filerClient.lookupEntry("/path/to/sources", "LOG");
        System.out.println(log);
        System.out.println("=================================================");

        // 创建文件夹
        boolean mkdirs = filerClient.mkdirs("/newPath1/test", 777);
        System.out.println(mkdirs);

        // 创建文件
        FilerProto.Entry builder = filerClient.newFileEntry("newPath2", 777, 0, 0, "dp", new String[]{}).build();
        System.out.println(builder.getName());
        boolean entry = filerClient.createEntry("/", builder);
        System.out.println(entry);

        // 删除文件
        boolean b = filerClient.deleteEntry("/", builder.getName(), false, true);
        System.out.println(b);

        // 创建文件夹
        FilerProto.Entry builder1 = filerClient.newDirectoryEntry("Directory", 777, 0, 0, "dp", new String[]{}).build();
        boolean createDirectory = filerClient.createEntry("/p", builder1);
        System.out.println(createDirectory);

        // 递归删除
        boolean rm = filerClient.rm("/newPath1", true);
        System.out.println(rm);

        // 创建文件夹
        boolean touch = filerClient.touch("/b.txt", 777);
        System.out.println(touch);

        // 更新
        boolean b1 = filerClient.updateEntry("/", builder1);
        System.out.println(b1);

    }
}
