## Seaweedfs Under Storage

Alluxio支持seaweedfs作为底层存储的扩展

### Build

```bash
mvn package
```

### Run Integration Tests

```bash
mvn test -Dseaweedfs.client.pool.size=10  -Dseaweedfs.replication=000
```
### Install

```bash
bin/alluxio extensions install alluxio-underfs-seaweedfs-1.8.1.jar
```

### List

```bash
bin/alluxio extensions ls
```

### Mount

```bash
bin/alluxio fs mount /seaweedfs-storage seaweedfs://<host-name>:8888/ --option seaweedfs.replication=000 --option seaweedfs.client.pool.size=10
```
> The host-name is the name of the filer server,8888 is the default port.

### RunTests

```bash
bin/alluxio runTests --directory /seaweedfs-storage
```

### Unmount

```bash
bin/alluxio fs unmount /seaweedfs-storage
```

### Uninstall

```bash
bin/alluxio extensions uninstall alluxio-underfs-seaweedfs-1.8.1.jar
```
### Benchmark:

##### Cluster Configuration Alluxio & Seaweedfs:
node | alluxio worker| master | volume | filer | Cpu | Memory
---|---|---|---|---|---|---
data1| √| √ | √ | √ | 4 Intel(R) Xeon(R) CPU E5-2620 v4 @ 2.10GHz |10.34GB
data2| √| √ | √ | - | 4 Intel(R) Xeon(R) CPU E5-2620 v4 @ 2.10GHz |10.34GB
data4| √| √ | √ | - | 4 Intel(R) Xeon(R) CPU E5-2620 v4 @ 2.10GHz |10.34GB

##### Machine to execute test jar:

free | cpu
---|---
5763 MB |  2 Intel(R) Xeon(R) CPU E5-2620 v4 @ 2.10GHz

Write:

    java -cp alluxio-performance-1.0.jar alluxio.examples.Performance mgr1:19998 10 1 1000 /seaweedfs-storage/ w

    =======Sequential Write=======
    Concurrency Level:              10
    Time taken for tests:           138 seconds
    Number of successful uploaded:  1000
    Number of failed uploads:       0
    Total file size:                1000 MB
    Transfer rate:                  7 MB/s

Read:

    java -cp alluxio-performance-1.0.jar alluxio.examples.Performance mgr1:19998 10 1 1000 /seaweedfs-storage/ r

    =======Sequential Read=======
    Concurrency Level:              10
    Time taken for tests:           32 seconds
    Number of successful uploaded:  1000
    Number of failed uploads:       0
    Total file size:                1000 MB
    Transfer rate:                  31 MB/s