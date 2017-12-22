package MultiplyDowloader;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class Main {
    /**
     * UI 的绘制
     */
    private JFrame jFrame;
    private JButton searchBn;
    private JButton fileChooseBn;
    private JButton dowloadBn;

    private JTextField urlField;
    private JTextField filePathField;
    private JTextField statusField;

    private JProgressBar dowloadProgress;

    private Font font;

    /**
     * 该下载器的尺寸
     */
    private int width = 500;
    private int heigh = 300;
    /**
     * 屏幕尺寸
     */
    private int screenWidth;
    private int screenHeigh;

    /**
     * 下载路径
     */
    private String urlPath;

    /**
     * 文件保存路径
     */
    private String fileToSave;

    private String TAG = "状态: ";

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 可返回结果的线程执行体
     */
    private Check check;

    private void init() {
        /**
         * 使整个UI和平台相关，可以美化Windows环境下的JFileChooser
         */
        if (UIManager.getLookAndFeel().isSupportedLookAndFeel()) {
            final String platform = UIManager.getSystemLookAndFeelClassName();
            if (!UIManager.getLookAndFeel().getName().equals(platform)) {
                try {
                    UIManager.setLookAndFeel(platform);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        jFrame = new JFrame("多线程下载器");
        /**
         * 设置界面在屏幕正中间
         */
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = (int) screenSize.getWidth();
        screenHeigh = (int) screenSize.getHeight();
        jFrame.setBounds((screenWidth - width) / 2,
                (screenHeigh - heigh) / 2,
                width, heigh);
        jFrame.setResizable(false);

        /**
         * 该字体将被所有控件使用
         */
        font = new Font("微软雅黑", 0, 13);

        /**
         * 设置为4*1的网格布局管理器
         */
        jFrame.setLayout(new GridLayout(4, 1));

        /**
         * 第一个JPanel
         * 进行链接的验证
         */
        JPanel dowloadPathPanel = new JPanel();
        dowloadPathPanel.setBackground(Color.WHITE);
        JLabel label1 = new JLabel("下载链接:");
        label1.setFont(font);
        urlField = new JTextField(20);
        urlField.setFont(font);
        searchBn = new JButton("验证");
        searchBn.setBackground(Color.WHITE);
        searchBn.setFont(font);
        searchBn.setFocusPainted(false);
        searchBn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(urlField.getText().trim().equals("") || urlField.getText() == null)) {
                    /**
                     * FutureTask的get方法是个阻塞方法，新开一个线程对其处理比较好
                     */
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                urlPath = urlField.getText().trim();
                                check = new Check(urlPath);
                                FutureTask<Map<String, String>> futureTask = new FutureTask<>(check);
                                new Thread(futureTask).start();
                                Map<String, String> map = futureTask.get();

                                statusField.setText(TAG + "已验证链接");
                                Thread.sleep(1000);
                                fileSize = Long.parseLong(map.getOrDefault("Content-Length", "-1"));
                                contentType = map.getOrDefault("Content-Type", null).split("\\:\\s?")[1];
                                if (fileSize <= 0) {
                                    statusField.setText(TAG + "链接无效");
                                } else {
                                    statusField.setText(TAG + "文件类型:" + contentType +
                                            ", 文件大小:" + getFileSize());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    statusField.setText(TAG + "请输入下载链接");
                }
            }
        });
        dowloadPathPanel.add(label1);
        dowloadPathPanel.add(urlField);
        dowloadPathPanel.add(searchBn);
        jFrame.add(dowloadPathPanel);

        /**
         * 第二个面板
         * 进行保存路径的设置
         */
        JPanel savePathPanel = new JPanel();
        savePathPanel.setBackground(Color.WHITE);
        JLabel label2 = new JLabel("保存路径:");
        label2.setFont(font);
        filePathField = new JTextField(20);
        filePathField.setFont(font);
        fileChooseBn = new JButton("路径");
        fileChooseBn.setBackground(Color.WHITE);
        fileChooseBn.setFont(font);
        fileChooseBn.setFocusPainted(false);
        fileChooseBn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFont(font);
                chooser.setDialogTitle("选择保存文件夹");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = chooser.showOpenDialog(jFrame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    if (getFileName() == null) {
                        fileToSave = chooser.getSelectedFile().getPath();
                    } else {
                        fileToSave = Paths.get(chooser.getSelectedFile().getPath(), getFileName()).toString();
                        statusField.setText(TAG + "已选择" + fileToSave);
                    }
                    println(fileToSave);
                    filePathField.setText(fileToSave);

                }
            }
        });
        savePathPanel.add(label2);
        savePathPanel.add(filePathField);
        savePathPanel.add(fileChooseBn);
        jFrame.add(savePathPanel);

        /**
         * 第三个面板
         * 进度条
         */
        dowloadProgress = new JProgressBar();
        dowloadProgress.setMinimum(0);
        dowloadProgress.setMaximum(100);
        dowloadProgress.setString("未开始下载...");
        dowloadProgress.setFont(font);
        dowloadProgress.setValue(0);
        dowloadProgress.setStringPainted(true);
        dowloadProgress.setPreferredSize(new Dimension(305, 25));
        dowloadProgress.setBorderPainted(true);
        dowloadProgress.setBackground(Color.WHITE);
        dowloadProgress.setForeground(new Color(0x6959CD));
        JPanel downPanel = new JPanel();
        downPanel.setBackground(Color.WHITE);
        dowloadBn = new JButton("下载");
        dowloadBn.setBackground(Color.WHITE);
        dowloadBn.setFocusPainted(false);
        dowloadBn.setFont(font);
        dowloadBn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                urlPath = urlPath.equals(urlField.getText().trim()) ? urlPath : urlField.getText().trim();
                fileToSave = fileToSave.equals(filePathField.getText().trim()) ? fileToSave : filePathField.getText().trim();
                if (urlPath == null || fileToSave == null) {
                    statusField.setText(TAG + "下载失败，请填写链接和路径");
                    return;
                }
                DownUntil downUntil = new DownUntil(urlPath, fileToSave, fileSize, contentType);
                /**
                 * 新开一个线程监控下载情况
                 * 并在进度条上反映出来
                 */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        double schedule = 0;
                        long start = System.currentTimeMillis();
                        statusField.setText(TAG + "正在下载");
                        while ((schedule = downUntil.getSchedule()) < 1d) {
                            dowloadProgress.setString(String.format("%.2f", schedule*100) + "%");
                            dowloadProgress.setValue((int) (schedule * 100));
                            dowloadProgress.validate();
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        long end = System.currentTimeMillis();
                        dowloadProgress.setValue(100);
                        dowloadProgress.setString("下载完成");
                        statusField.setText(TAG + "下载用时: " + excuteTime(end-start));
                    }
                }).start();


                statusField.setText(TAG + "正在准备下载");
                long time = downUntil.dowload();
                if (time == -1) {
                    statusField.setText(TAG + "下载失败");
                }
            }
        });
        downPanel.add(dowloadProgress);
        downPanel.add(dowloadBn);
        jFrame.add(downPanel);

        /**
         * 第四个面板
         * 状态信息
         */
        JPanel statusPanel = new JPanel();
        statusPanel.setBackground(Color.WHITE);
        statusField = new JTextField(31);
        statusField.setText("状态: ");
        statusField.setBorder(null);
        statusField.setEditable(false);
        statusField.setFont(font);
        statusField.setBackground(Color.WHITE);
        statusPanel.add(statusField);
        jFrame.add(statusPanel);

        jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

        /**
         * 在启动时查看系统剪贴板中的内容
         * 如果有链接，就直接复制到urlField上去
         */
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipTf = sysClip.getContents(null);
        if (clipTf != null) {
            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
                    String check = "((http|ftp|https)://)(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?";
                    if (Pattern.matches(check, ret)) {
                        statusField.setText(TAG + "检测到剪贴板的链接");
                        urlField.setText(ret);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Main() {
        init();
    }
    public static void main(String[] args) {
        new Main();
    }

    public static <T> void print(T t) {
        System.out.print(t);
    }

    public static <T> void println(T t) {
        print(t + "\n");
    }

    /**
     * 获取文件大小，格式化数字
     * @return
     */
    private String getFileSize () {
        if (fileSize < 1024) {
            return fileSize + "B";
        } else if (fileSize < 1024*1024){
            return String.format("%.2f", fileSize * 1.0 / 1024) + "KB";
        } else if (fileSize < 1024*1024*1024){
            return String.format("%.2f", fileSize * 1.0 / (1024*1024)) + "MB";
        } else {
            return String.format("%.2f", fileSize * 1.0 / (1024*1024*1024)) + "GB";
        }
    }

    /**
     * 通过URL解析或者content-type转换获取文件名，用户也可以自定义文件名
     * @return
     */
    private String getFileName() {
        println(contentType);
        if ((urlPath = urlField.getText()).equals("") || urlPath == null) {
            statusField.setText(TAG + "请输入下载链接");
            return null;
        } else {
            int pos = urlPath.lastIndexOf("/") + 1;
            String name = urlPath.substring(pos);
            if (Pattern.matches("^\\S{3,20}\\.\\S{3,10}", name)) {
                return name;
            } else if (MimeUtils.hasMimeType(contentType)) {
                SimpleDateFormat dateformat=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                return "Dowload" + dateformat.format(new Date()) + "." + MimeUtils.guessExtensionFromMimeType(contentType);
            }
        }
        return null;
    }

    /**
     * 对下载所用的时间格式进行处理
     * @param time
     * @return
     */
    private String excuteTime(long time) {
        if (time < 1000) {
            return time + "ms";
        } else if (time < 1000*60) {
            return time/1000 + "s";
        } else if (time < 1000*60*60) {
            return time/(1000*60) + "min";
        } else {
            return time/(1000*60*60) + "h";
        }
    }
}
