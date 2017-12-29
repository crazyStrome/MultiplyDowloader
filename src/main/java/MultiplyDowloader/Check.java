package MultiplyDowloader;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static MultiplyDowloader.Main.println;

/**
 * 返回值是文件类型
 */
public class Check implements Callable<Map<String, String>> {

    /**
     * 作为线程执行体的返回值，包含Content-Length和Content-Type
     */
    private Map<String, String> map;
    private String checkPath;
    public Check(String checkPath) {
        this.checkPath = checkPath;
        map = new HashMap<>();
    }

    @Override
    public Map<String, String> call() {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(checkPath);
            CloseableHttpResponse response = client.execute(get);
            println(response.getStatusLine());
            if (response.getStatusLine().getStatusCode() == 200) {
                map.put("Content-Type", response.getEntity().getContentType().toString());
                map.put("Content-Length", response.getEntity().getContentLength()+"");
                Arrays.stream(response.getAllHeaders()).forEach(Main::println);
            }
            client.close();
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return map;
    }

    public Map<String, String> getMap() {
        return map;
    }

    /**
    public static void main(String[] args) throws Exception{
        //FutureTask<Map<String, String>> fu = new FutureTask<>(new Check("http://dl.mqego.com/soft1/navicatmysqlfront.zip"));
        //new Thread(fu).start();
        //println(fu.get());
    }
     */
}
