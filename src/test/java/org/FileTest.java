package org;

import com.alibaba.fastjson.JSON;
import org.assertj.core.util.Lists;
import org.common.fastdfs.ClientGlobal;
import org.common.fastdfs.StorageClient;
import org.common.fastdfs.TrackerClient;
import org.common.fastdfs.TrackerServer;
import org.common.fastdfs.common.MyException;
import org.common.fastdfs.common.NameValuePair;
import org.junit.Test;
import org.springframework.boot.test.json.JsonbTester;

import java.util.Collections;

import java.io.*;
import java.util.List;

/**
 * @Author onlyone
 * @create 2020/9/25
 */
public class FileTest {

    // 多线程调用，要注意线程安全
    private List<String> fileNames = Collections.synchronizedList(Lists.newArrayList());
    private String groupName = "group1";

    @Test
    public void upload() throws IOException, MyException, InterruptedException {

        ClientGlobal.init("fdfs_client.conf");

        TrackerClient tracker = new TrackerClient();


        for (int i = 1; i <= 15; i++) {
            String f = "/Users/onlyone/open-github/fastdfs-client-example/source/" + i + ".txt";
            String fileName = i + ".txt";
            Runnable t = () -> {

                TrackerServer trackerServer = null;
                try {
                    trackerServer = tracker.getConnection();
                } catch (Exception e) {
                }
                // 打印 Socket 信息，验证是否同一个?
                // 结论：每次上传文件，都创建了一个 Socket
                try {
                    System.out.println("Socket=" + trackerServer.getSocket() + "  tracker_group=" + tracker.getTrackerGroup().tracker_servers.length);
                } catch (Exception e) {
                }

                StorageClient storageClient = new StorageClient(trackerServer, null);

                NameValuePair[] metaList = new NameValuePair[1];
                metaList[0] = new NameValuePair("fileName", fileName);
                File file = new File(f);
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                int length = 0;
                try {
                    length = inputStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] bytes = new byte[length];
                try {
                    inputStream.read(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String[] result = new String[0];
                try {
                    result = storageClient.upload_file(bytes, null, metaList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                fileNames.add(result[1]);
                System.out.println(JSON.toJSONString(result));
            };
            new Thread(t).start();
        }
        Thread.sleep(8000);

        System.out.println("上传的文件个数：" + JSON.toJSONString(fileNames.size()));
        download();

        Thread.sleep(30000);
    }

    public void download() throws IOException, MyException {
        ClientGlobal.init("fdfs_client.conf");


        for (String s : fileNames) {

            Runnable t2 = () -> {

                TrackerClient tracker = new TrackerClient();
                TrackerServer trackerServer = null;
                try {
                    trackerServer = tracker.getConnection();
                } catch (IOException e) {
                }
                StorageClient storageClient = new StorageClient(trackerServer, null);

                byte[] result = new byte[0];
                try {
                    result = storageClient.download_file(groupName, s);
                } catch (Exception e) {
                }
//            String local_filename = "1.txt";
//            writeByteToFile(result, local_filename);
//            File file = new File(local_filename);
                System.out.println(new String(result));
            };
            new Thread(t2).start();
        }


    }
}
