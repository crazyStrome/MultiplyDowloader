package MultiplyDowloader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class DownUntil {
    private String urlPath;
    private String fileToSave;
    private long fileSize;
    private boolean startDowload = false;
    private boolean dowloadComplete = false;
    private int threadNum = Runtime.getRuntime().availableProcessors();
    private String contentType;
    private DowloadThread[] threads = new DowloadThread[threadNum];
    public DownUntil(String urlPath, String fileToSave, long fileSize, String contentType) {
        this.urlPath = urlPath;
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

    public long dowload() {
        try {
            long currentPartSize = fileSize / threadNum + 1;
            RandomAccessFile raf = new RandomAccessFile(fileToSave, "rw");
            raf.setLength(fileSize);
            raf.close();
            for (int i = 0; i < threadNum; i ++) {
                long startPos = i * currentPartSize;
                RandomAccessFile currentPart = new RandomAccessFile(fileToSave,
                        "rw");
                currentPart.seek(startPos);
                threads[i] = new DowloadThread(startPos, currentPartSize,
                        currentPart);
                threads[i].start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public boolean isDowloadComplete() {
        return dowloadComplete;
    }

    public boolean isStartDowload() {
        return startDowload;
    }

    private void setDowloadComplete(boolean dowloadComplete) {
        this.dowloadComplete = dowloadComplete;
    }

    private void setStartDowload(boolean startDowload) {
        this.startDowload = startDowload;
    }

    public double getSchedule() {
        long sumSize = 0;
        for (int i = 0; i < threadNum; i ++) {
            if (threads[i] != null) {
                sumSize += threads[i].length;
            }
        }
        return sumSize * 1.0 / fileSize;
    }
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
        public long length = 0;
        private long startPos;
        private long currentPartSize;
        private RandomAccessFile currentPart;
        public DowloadThread(long start, long dowloadPartSize, RandomAccessFile currentPart) {
            this.startPos = start;
            this.currentPartSize = dowloadPartSize;
            this.currentPart = currentPart;
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
                InputStream in = response.getEntity().getContent();
                in.skip(startPos);
                byte[] buffer = new byte[1024];
                int hasRead = 0;
                while (length < currentPartSize &&
                        (hasRead = in.read(buffer)) != -1) {
                    currentPart.write(buffer, 0, hasRead);
                    length += hasRead;
                }
                currentPart.close();
                in.close();
                response.close();
                client.close();
            }  catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
