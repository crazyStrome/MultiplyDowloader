package MultiplyDowloader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class DownUntil {
    /**
     * 下载链接
     */
    private String urlPath;
    /**
     * 文件保存路径
     */
    private String fileToSave;
    /**
     * 文件大小
     */
    private long fileSize;
    /**
     * 下载使用的线程数量，以主机处理器个数为准
     */
    private int threadNum = Runtime.getRuntime().availableProcessors();
    /**
     * 文件类型
     */
    private String contentType;
    /**
     * 线程组，用于下载
     */
    private DowloadThread[] threads = new DowloadThread[threadNum];
    /**
     * 正在运行的下载线程个数
     */
    private int runningThread = 0;

    /**
     *
     * @param urlPath
     * @param fileToSave
     * @param fileSize
     * @param contentType
     */
    public DownUntil(String urlPath, String fileToSave, long fileSize, String contentType) {
        this.urlPath = urlPath;
        /**
         * 如果传入的路径是个文件夹，则自己给它命名
         */
        if (Files.isDirectory(Paths.get(fileToSave))) {
            String name = getName();
            if (name != null) {
                this.fileToSave = Paths.get(fileToSave, name).toString();
            } else {
                this.fileToSave = Paths.get(fileToSave, "dowload.dowload").toString();
            }
        } else {
            this.fileToSave = fileToSave;
        }
        /**
         * 如果传入的文件大小或者文件类型是空的话
         * 再进行一次验证
         */
        if (fileSize == 0 || contentType == null) {
            try {
                Check check = new Check(urlPath);
                FutureTask<Map<String, String>> future = new FutureTask<>(check);
                new Thread(future).start();
                Map<String, String> map = future.get();
                this.fileSize = Long.parseLong(map.getOrDefault("Content-Length", "-1"));
                this.contentType = map.getOrDefault("Content-Type", "type/dowload").split("\\:\\s?")[1];
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.fileSize = fileSize;
            this.contentType = contentType;
        }
    }

    /**
     * 下载方法
     * @return
     */
    public long dowload() {
        try {
            /**
             * 计算每个线程需要下载的块大小
             */
            long currentPartSize = fileSize / threadNum + 1;
            /**
             * 预先划分一个所需文件大小的空间
             */
            RandomAccessFile raf = new RandomAccessFile(fileToSave, "rw");
            raf.setLength(fileSize);
            raf.close();
            /**
             * 开启线程
             */
            for (int i = 0; i < threadNum; i ++) {
                long startPos = i * currentPartSize;
                RandomAccessFile currentPart = new RandomAccessFile(fileToSave,
                        "rw");
                currentPart.seek(startPos);
                threads[i] = new DowloadThread(startPos, currentPartSize,
                        currentPart, i);
                threads[i].start();
                runningThread ++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    /**
     * 由外部调用来获取下载进度
     * @return
     */
    public double getSchedule() {
        long sumSize = 0;
        for (int i = 0; i < threadNum; i ++) {
            if (threads[i] != null) {
                sumSize += threads[i].length;
            }
        }
        return sumSize * 1.0 / fileSize;
    }

    /**
     * 当输入的fileToSave不是文件的话，下列方法可以推断出一个文件名
     * @return
     */
    private String getName () {
        int pos = urlPath.lastIndexOf("/") + 1;
        String name = urlPath.substring(pos);
        if (Pattern.matches("^\\S{3,20}\\.\\S{3,10}", name)) {
            return name;
        } else if (MimeUtils.hasMimeType(contentType)) {
            SimpleDateFormat dateformat=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            return "Dowload" + dateformat.format(new Date()) + "." + MimeUtils.guessExtensionFromMimeType(contentType);
        }
        return null;
    }

    /**
     * 线程子类进行分片下载
     */
    private class DowloadThread extends Thread {
        private int threadId;
        public long length = 0;
        private long startPos;
        private long currentPartSize;
        private RandomAccessFile currentPart;
        public DowloadThread(long start, long dowloadPartSize, RandomAccessFile currentPart, int threadId) {
            this.startPos = start;
            this.currentPartSize = dowloadPartSize;
            this.currentPart = currentPart;
            this.threadId = threadId;
        }
        @Override
        public void run() {
            try {

                CloseableHttpClient client = HttpClients.createDefault();
                HttpGet get = new HttpGet(urlPath);
                get.setHeader("Accept", "*/*");
                get.addHeader("Accept-Language", "zh=CN");
                get.addHeader("Charset", "UTF-8");
                CloseableHttpResponse response = client.execute(get);

                if (response.getStatusLine().getStatusCode() == 200) {
                    File file = new File(fileToSave + threadId + ".tmp");
                    if (file.exists() && file.length() > 0) {
                        /**
                         * 如果之前的缓存文件存在的话，读取之前缓存文件的写入位置
                         */
                        FileInputStream fis = new FileInputStream(file);
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(fis)
                        );
                        String lastPosition = br.readLine();
                        long last = Long.parseLong(lastPosition);
                        length = last - startPos;
                        startPos = last;
                        currentPart.seek(startPos);
                        br.close();
                        fis.close();
                    }
                    InputStream in = response.getEntity().getContent();
                    in.skip(startPos);
                    byte[] buffer = new byte[1024 * 1024];
                    int hasRead;
                    long total = 0;
                    while (length < currentPartSize &&
                            (hasRead = in.read(buffer)) != -1) {
                        currentPart.write(buffer, 0, hasRead);
                        length += hasRead;
                        /**
                         * 每写入文件1M，在缓存文件中记录当前位置
                         */
                        total += hasRead;
                        long currentThreadPos = startPos + total;
                        RandomAccessFile raff = new RandomAccessFile(
                                fileToSave + threadId + ".tmp", "rwd"
                        );
                        raff.write((currentThreadPos + "").getBytes());
                        raff.close();
                    }
                    currentPart.close();
                    in.close();
                    /**
                     * 如果runningThread为0的话，删除所有缓存文件
                     */
                    synchronized (DowloadThread.class) {
                        runningThread --;
                        if (runningThread == 0) {
                            for (int i = 0; i < threadNum; i ++) {
                                Files.delete(Paths.get(
                                        fileToSave + i + ".tmp"
                                ));
                            }
                        }
                    }
                }
                response.close();
                client.close();
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
