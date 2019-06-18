package alluxio.examples;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.Constants;
import alluxio.PropertyKey;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.options.CreateFileOptions;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.client.file.policy.LocalFirstPolicy;
import alluxio.exception.AlluxioException;
import alluxio.exception.status.UnauthenticatedException;
import alluxio.security.LoginUser;
import alluxio.security.authorization.Mode;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static alluxio.client.ReadType.CACHE_PROMOTE;
import static alluxio.client.WriteType.CACHE_THROUGH;

/**
 * @author DPn!ce
 * @date 2019/03/07.
 */
public class Performance {
    public static void main(String[] args) {
        try {
            System.out.println("用户：" + LoginUser.get().toString());
        } catch (UnauthenticatedException e) {
            e.printStackTrace();
        }
        // host:port
        HostAndPort masterAddress = HostAndPort.fromString(args[0]);
        Configuration.set(PropertyKey.SECURITY_LOGIN_USERNAME, "root");
        Configuration.set(PropertyKey.MASTER_HOSTNAME, masterAddress.getHostText());
        Configuration.set(PropertyKey.MASTER_RPC_PORT, Integer.toString(masterAddress.getPort()));
        //线程数
        int thread = Integer.valueOf(args[1]);
        //文件大小
        int fileMB = Integer.parseInt(args[2]);
        //文件总数
        int fileN = Integer.parseInt(args[3]);
        //AlluxioPath  /seaweedfsPath/
        String alluxioPath = args[4];
        //功能 读[r] or 写[w]
        String rw = args[5];
        int threadFileN = fileN / thread;
        if ("r".equals(rw)) {
            List<ReadWorker> readWorkerList = new ArrayList<>();
            int fileStartIndex = 0;
            for (int t = 0; t < thread; t++) {
                readWorkerList.add(new ReadWorker(alluxioPath, fileStartIndex, threadFileN));
                //下一个线程的起始位置
                fileStartIndex += threadFileN;
            }

            final long startTimeMs = System.currentTimeMillis();
            //启动
            readWorkerList.forEach(Thread::start);
            readWorkerList.forEach(readWorker -> {
                try {
                    //Waits for this thread to die.
                    readWorker.join();
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
            });
            final long takenTimeMs = System.currentTimeMillis() - startTimeMs;
            Integer failedNumber = readWorkerList.stream().reduce(0, (x, readWorker) -> x + readWorker.getErrorNumber(), (x, y) -> x + y);
            int totalFileSize = (fileN - failedNumber) * fileMB;
            //报告
            System.out.println("=======Sequential Read=======");
            System.out.println("Concurrency Level:              " + thread);
            System.out.println("Time taken for tests:           " + takenTimeMs / 1000 + " seconds");
            System.out.println("Number of successful uploaded:  " + (fileN - failedNumber));
            System.out.println("Number of failed uploads:       " + failedNumber);
            System.out.println("Total file size:                " + totalFileSize + " MB");
            System.out.println("Transfer rate:                  " + totalFileSize / (takenTimeMs / 1000) + " MB/s");
        } else if ("w".equals(rw)) {
            List<WriteWorker> writeWorkerList = new ArrayList<>();
            byte[] bytes = GenerateFiles.newBytes(fileMB);
            int fileStartIndex = 0;
            for (int t = 0; t < thread; t++) {
                writeWorkerList.add(new WriteWorker(alluxioPath, fileStartIndex, threadFileN, bytes));
                //下一个线程的起始位置
                fileStartIndex += threadFileN;
            }
            final long startTimeMs = System.currentTimeMillis();
            //启动
            writeWorkerList.forEach(Thread::start);
            writeWorkerList.forEach(readWorker -> {
                try {
                    //Waits for this thread to die.
                    readWorker.join();
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
            });
            final long takenTimeMs = System.currentTimeMillis() - startTimeMs;
            Integer failedNumber = writeWorkerList.stream().reduce(0, (x, readWorker) -> x + readWorker.getErrorNumber(), (x, y) -> x + y);
            int totalFileSize = (fileN - failedNumber) * fileMB;
            //报告
            System.out.println("=======Sequential Write=======");
            System.out.println("Concurrency Level:              " + thread);
            System.out.println("Time taken for tests:           " + takenTimeMs / 1000 + " seconds");
            System.out.println("Number of successful uploaded:  " + (fileN - failedNumber));
            System.out.println("Number of failed uploads:       " + failedNumber);
            System.out.println("Total file size:                " + totalFileSize + " MB");
            System.out.println("Transfer rate:                  " + totalFileSize / (takenTimeMs / 1000) + " MB/s");

        } else {
            System.out.println("参数错误,功能参数: r or w");
        }
    }

    private static void create(FileSystem fs, String filePath, byte[] bytes) throws IOException, AlluxioException {
        //  Generate options to set a custom blocksize of 128 MB
        CreateFileOptions options = CreateFileOptions.defaults().setBlockSizeBytes(32 * Constants.MB);
        Mode defaults = Mode.defaults();
        defaults.setOwnerBits(Mode.Bits.ALL);
        options.setMode(defaults);
        // 同步到底层存储 默认是MUST_CACHE
        options.setWriteType(CACHE_THROUGH);
        // Create a file and get its output stream
        FileOutStream fos = fs.createFile(new AlluxioURI(filePath), options);
        // Write data
        fos.write(bytes, 0, bytes.length);
        // Close and complete file
        fos.close();
    }

    /**
     * 读文件
     *
     * @param fs FileSystem
     * @throws IOException
     * @throws AlluxioException
     */
    private static void open(FileSystem fs, String filePath) throws IOException, AlluxioException {
        OpenFileOptions options = OpenFileOptions.defaults()
                .setCacheLocationPolicy(new LocalFirstPolicy())
                .setReadType(CACHE_PROMOTE);

        FileInStream in = fs.openFile(new AlluxioURI(filePath), options);
        //Read data
        int i;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((i = in.read()) != -1) {
            baos.write(i);
        }
        baos.close();
        in.close();

    }

    /**
     * 读文件线程
     */
    public static class ReadWorker extends Thread {
        private FileSystem fs = FileSystem.Factory.get();
        private String filePathPre;
        private int fileStartIndex;
        private int fileStartMax;
        private long completeTime;
        private int errorNumber;

        public long getCompleteTime() {
            return completeTime;
        }

        public int getErrorNumber() {
            return errorNumber;
        }

        public ReadWorker(String filePathPre, int fileStartIndex, int fileN) {
            this.filePathPre = filePathPre;
            this.fileStartIndex = fileStartIndex;
            this.fileStartMax = fileStartIndex + fileN;
        }

        @Override
        public void run() {
            System.out.println("启动");
            long startTime = System.currentTimeMillis();
            while (fileStartIndex < fileStartMax) {
                String filePath = filePathPre + "test" + fileStartIndex;
                try {
                    open(fs, filePath);
                } catch (IOException | AlluxioException e) {
                    errorNumber++;
                }
                fileStartIndex++;
            }
            completeTime = System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 写文件线程
     */
    public static class WriteWorker extends Thread {
        private FileSystem fs = FileSystem.Factory.get();
        private String filePathPre;
        private byte[] bytes;
        private int fileStartIndex;
        private int fileStartMax;
        private long completeTime;
        private int errorNumber;

        public long getCompleteTime() {
            return completeTime;
        }

        public int getErrorNumber() {
            return errorNumber;
        }

        public WriteWorker(String filePathPre, int fileStartIndex, int fileN, byte[] bytes) {
            this.bytes = bytes.clone();
            this.filePathPre = filePathPre;
            this.fileStartIndex = fileStartIndex;
            this.fileStartMax = fileStartIndex + fileN;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            while (fileStartIndex < fileStartMax) {
                String filePath = filePathPre + "test" + fileStartIndex;
                try {
                    create(fs, filePath, bytes);
                } catch (IOException | AlluxioException e) {
                    errorNumber++;
                }
                fileStartIndex++;
            }
            completeTime = System.currentTimeMillis() - startTime;
        }
    }


}
