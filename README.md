# 多线程下载器

## 功能：
***

* 断点续传
* 启动时自动检查剪切板
* 用户界面友好

## 实现
***

首先上成果图,如下

![UI界面](https://raw.githubusercontent.com/crazyStrome/myPhote/master/UI.PNG)

打开该软件，会检查剪贴板里面是否保存着链接，如果链接的格式匹配成功的话，直接显示出来。

![检查链接](https://cdn-std.dprcdn.net/files/acc_615569/7fIc0?download)

单击**验证**按钮，会进行验证，以下是验证成功的界面，显示了文件的大小和类型。

![连接验证成功](https://cdn-std.dprcdn.net/files/acc_615569/e7vmwB?download)

单击**路径**按钮，会弹出如下界面进行存放路径选择。

![路径选择](https://cdn-std.dprcdn.net/files/acc_615569/2Uy02?download)

选择路径之后，软件会根据下载链接和之前验证的文件类型来推断出来文件名字以及后缀并添加到文本框中去。

![自动完成文本](https://cdn-std.dprcdn.net/files/acc_615569/GHnmPM?download)

单击**下载**按钮，会进行下载，如果之前被中断过的话，可以断点续传。

![下载1](https://cdn-std.dprcdn.net/files/acc_615569/BUgf3z?download)

![下载1](https://cdn-std.dprcdn.net/files/acc_615569/VwWD3y?download)

会在文件保存路径生成缓存文件，下载完成后自动删除。

![](https://cdn-std.dprcdn.net/files/acc_615569/9PhTcq?download)

## 代码
***

[Github](https://github.com/crazyStrome/MultiplyDowloader)

主要有四个文件：

<span id="Main">**Main.class**</span>

<span id="Check">**Check.class**</span>

<span id="MimeUtils">**MimeUtils.class**</span>

<span id="DownUtil">**DownUtil**</span>
***

### [Main](https://github.com/crazyStrome/MultiplyDowloader/blob/master/src/main/java/MultiplyDowloader/Main.java)

由于swing的FileChooser界面太丑了，所以我在初始化开头放了一段代码，使整体的代码具有Windows的风格。

```@Java
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
```

还有一个是在初始化时对剪贴板进行检测，如果剪贴板中的内容是链接的话，直接放到URL的文本框中去。

```@Java
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
```

其他的都是基本的交互逻辑，具体看代码。

### [Check](https://github.com/crazyStrome/MultiplyDowloader/blob/master/src/main/java/MultiplyDowloader/Check.java)

该类主要是继承了Callable接口，是多线程编程的另一种实现方法。
多线程编程主要三个方式：

* 继承自Thread类并重写run方法
* 实现Runnable接口的run方法，并传入Thread的构造方法
* 继承自Callable接口，并用FutureTask实现

Callable和Runnable接口类似，他们两个都是可编程接口。Runnable定义了run方法，Callable实现了
call方法并具有一个返回值，至就意味着，在Thread结束后可以获取方法执行体的结果，这使得多线程编程更加方便。
具体实现方法如下：

```@Java
	FutureTask<T> future = new FutureTask<T>(? implements Callable);
	new Thread(future).start();
	T result = future.get();
```

在该类中我定义的返回值是Map：

```@Java
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
```

### [MimeUtils](https://github.com/crazyStrome/MultiplyDowloader/blob/master/src/main/java/MultiplyDowloader/MimeUtils.java)

为了推断文件的后缀名，我使用获取的Mime类型进行查找，该类是小米公司开源的内容，开源代码如下：

```@Java
/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */
package MultiplyDowloader;

import java.util.HashMap;
import java.util.Map;


public final class MimeUtils {
    private static final Map<String, String> mimeTypeToExtensionMap = new HashMap<String, String>();

    //private static final Map<String, String> extensionToMimeTypeMap = new HashMap<String, String>();

    static {
        // The following table is based on /etc/mime.types data minus
        // chemical/* MIME types and MIME types that don't map to any
        // file extensions. We also exclude top-level domain names to
        // deal with cases like:
        //
        // mail.google.com/a/google.com
        //
        // and "active" MIME types (due to potential security issues).

        add("application/andrew-inset", "ez");
        add("application/dsptype", "tsp");
        add("application/futuresplash", "spl");
        add("application/hta", "hta");
        add("application/mac-binhex40", "hqx");
        add("application/mac-compactpro", "cpt");
        add("application/mathematica", "nb");
        add("application/msaccess", "mdb");
        add("application/oda", "oda");
        add("application/ogg", "ogg");
        add("application/pdf", "pdf");
        add("application/pgp-keys", "key");
        add("application/pgp-signature", "pgp");
        add("application/pics-rules", "prf");
        add("application/rar", "rar");
        add("application/rdf+xml", "rdf");
        add("application/rss+xml", "rss");
        add("application/zip", "zip");
        add("application/vnd.android.package-archive", "apk");
        add("application/vnd.cinderella", "cdy");
        add("application/vnd.ms-pki.stl", "stl");
        add("application/vnd.oasis.opendocument.database", "odb");
        add("application/vnd.oasis.opendocument.formula", "odf");
        add("application/vnd.oasis.opendocument.graphics", "odg");
        add("application/vnd.oasis.opendocument.graphics-template", "otg");
        add("application/vnd.oasis.opendocument.image", "odi");
        add("application/vnd.oasis.opendocument.spreadsheet", "ods");
        add("application/vnd.oasis.opendocument.spreadsheet-template", "ots");
        add("application/vnd.oasis.opendocument.text", "odt");
        add("application/vnd.oasis.opendocument.text-master", "odm");
        add("application/vnd.oasis.opendocument.text-template", "ott");
        add("application/vnd.oasis.opendocument.text-web", "oth");
        add("application/vnd.google-earth.kml+xml", "kml");
        add("application/vnd.google-earth.kmz", "kmz");
        add("application/msword", "doc");
        add("application/msword", "dot");
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        add("application/vnd.openxmlformats-officedocument.wordprocessingml.template", "dotx");
        add("application/vnd.ms-excel", "xls");
        add("application/vnd.ms-excel", "xlt");
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        add("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "xltx");
        add("application/vnd.ms-powerpoint", "ppt");
        add("application/vnd.ms-powerpoint", "pot");
        add("application/vnd.ms-powerpoint", "pps");
        add("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        add("application/vnd.openxmlformats-officedocument.presentationml.template", "potx");
        add("application/vnd.openxmlformats-officedocument.presentationml.slideshow", "ppsx");
        add("application/vnd.rim.cod", "cod");
        add("application/vnd.smaf", "mmf");
        add("application/vnd.stardivision.calc", "sdc");
        add("application/vnd.stardivision.draw", "sda");
        add("application/vnd.stardivision.impress", "sdd");
        add("application/vnd.stardivision.impress", "sdp");
        add("application/vnd.stardivision.math", "smf");
        add("application/vnd.stardivision.writer", "sdw");
        add("application/vnd.stardivision.writer", "vor");
        add("application/vnd.stardivision.writer-global", "sgl");
        add("application/vnd.sun.xml.calc", "sxc");
        add("application/vnd.sun.xml.calc.template", "stc");
        add("application/vnd.sun.xml.draw", "sxd");
        add("application/vnd.sun.xml.draw.template", "std");
        add("application/vnd.sun.xml.impress", "sxi");
        add("application/vnd.sun.xml.impress.template", "sti");
        add("application/vnd.sun.xml.math", "sxm");
        add("application/vnd.sun.xml.writer", "sxw");
        add("application/vnd.sun.xml.writer.global", "sxg");
        add("application/vnd.sun.xml.writer.template", "stw");
        add("application/vnd.visio", "vsd");
        add("application/x-abiword", "abw");
        add("application/x-apple-diskimage", "dmg");
        add("application/x-bcpio", "bcpio");
        add("application/x-bittorrent", "torrent");
        add("application/x-cdf", "cdf");
        add("application/x-cdlink", "vcd");
        add("application/x-chess-pgn", "pgn");
        add("application/x-cpio", "cpio");
        add("application/x-debian-package", "deb");
        add("application/x-debian-package", "udeb");
        add("application/x-director", "dcr");
        add("application/x-director", "dir");
        add("application/x-director", "dxr");
        add("application/x-dms", "dms");
        add("application/x-doom", "wad");
        add("application/x-dvi", "dvi");
        add("application/x-flac", "flac");
        add("application/x-font", "pfa");
        add("application/x-font", "pfb");
        add("application/x-font", "gsf");
        add("application/x-font", "pcf");
        add("application/x-font", "pcf.Z");
        add("application/x-freemind", "mm");
        add("application/x-futuresplash", "spl");
        add("application/x-gnumeric", "gnumeric");
        add("application/x-go-sgf", "sgf");
        add("application/x-graphing-calculator", "gcf");
        add("application/x-gtar", "gtar");
        add("application/x-gtar", "tgz");
        add("application/x-gtar", "taz");
        add("application/x-hdf", "hdf");
        add("application/x-ica", "ica");
        add("application/x-internet-signup", "ins");
        add("application/x-internet-signup", "isp");
        add("application/x-iphone", "iii");
        add("application/x-iso9660-image", "iso");
        add("application/x-jmol", "jmz");
        add("application/x-kchart", "chrt");
        add("application/x-killustrator", "kil");
        add("application/x-koan", "skp");
        add("application/x-koan", "skd");
        add("application/x-koan", "skt");
        add("application/x-koan", "skm");
        add("application/x-kpresenter", "kpr");
        add("application/x-kpresenter", "kpt");
        add("application/x-kspread", "ksp");
        add("application/x-kword", "kwd");
        add("application/x-kword", "kwt");
        add("application/x-latex", "latex");
        add("application/x-lha", "lha");
        add("application/x-lzh", "lzh");
        add("application/x-lzx", "lzx");
        add("application/x-maker", "frm");
        add("application/x-maker", "maker");
        add("application/x-maker", "frame");
        add("application/x-maker", "fb");
        add("application/x-maker", "book");
        add("application/x-maker", "fbdoc");
        add("application/x-mif", "mif");
        add("application/x-ms-wmd", "wmd");
        add("application/x-ms-wmz", "wmz");
        add("application/x-msi", "msi");
        add("application/x-ns-proxy-autoconfig", "pac");
        add("application/x-nwc", "nwc");
        add("application/x-object", "o");
        add("application/x-oz-application", "oza");
        add("application/x-pkcs12", "p12");
        add("application/x-pkcs7-certreqresp", "p7r");
        add("application/x-pkcs7-crl", "crl");
        add("application/x-quicktimeplayer", "qtl");
        add("application/x-shar", "shar");
        add("application/x-shockwave-flash", "swf");
        add("application/x-stuffit", "sit");
        add("application/x-sv4cpio", "sv4cpio");
        add("application/x-sv4crc", "sv4crc");
        add("application/x-tar", "tar");
        add("application/x-texinfo", "texinfo");
        add("application/x-texinfo", "texi");
        add("application/x-troff", "t");
        add("application/x-troff", "roff");
        add("application/x-troff-man", "man");
        add("application/x-ustar", "ustar");
        add("application/x-wais-source", "src");
        add("application/x-wingz", "wz");
        add("application/x-webarchive", "webarchive");
        add("application/x-webarchive-xml", "webarchivexml");
        add("application/x-x509-ca-cert", "crt");
        add("application/x-x509-user-cert", "crt");
        add("application/x-xcf", "xcf");
        add("application/x-xfig", "fig");
        add("application/xhtml+xml", "xhtml");
        add("audio/3gpp", "3gpp");
        add("audio/amr", "amr");
        add("audio/basic", "snd");
        add("audio/midi", "mid");
        add("audio/midi", "midi");
        add("audio/midi", "kar");
        add("audio/midi", "xmf");
        add("audio/mobile-xmf", "mxmf");
        add("audio/mpeg", "mpga");
        add("audio/mpeg", "mpega");
        add("audio/mpeg", "mp2");
        add("audio/mpeg", "mp3");
        add("audio/mpeg", "m4a");
        add("audio/mpegurl", "m3u");
        add("audio/prs.sid", "sid");
        add("audio/x-aiff", "aif");
        add("audio/x-aiff", "aiff");
        add("audio/x-aiff", "aifc");
        add("audio/x-gsm", "gsm");
        add("audio/x-mpegurl", "m3u");
        add("audio/x-ms-wma", "wma");
        add("audio/x-ms-wax", "wax");
        add("audio/x-pn-realaudio", "ra");
        add("audio/x-pn-realaudio", "rm");
        add("audio/x-pn-realaudio", "ram");
        add("audio/x-realaudio", "ra");
        add("audio/x-scpls", "pls");
        add("audio/x-sd2", "sd2");
        add("audio/x-wav", "wav");
        add("image/bmp", "bmp");
        add("audio/x-qcp", "qcp");
        add("image/gif", "gif");
        add("image/ico", "cur");
        add("image/ico", "ico");
        add("image/ief", "ief");
        add("image/jpeg", "jpeg");
        add("image/jpeg", "jpg");
        add("image/jpeg", "jpe");
        add("image/pcx", "pcx");
        add("image/png", "png");
        add("image/svg+xml", "svg");
        add("image/svg+xml", "svgz");
        add("image/tiff", "tiff");
        add("image/tiff", "tif");
        add("image/vnd.djvu", "djvu");
        add("image/vnd.djvu", "djv");
        add("image/vnd.wap.wbmp", "wbmp");
        add("image/x-cmu-raster", "ras");
        add("image/x-coreldraw", "cdr");
        add("image/x-coreldrawpattern", "pat");
        add("image/x-coreldrawtemplate", "cdt");
        add("image/x-corelphotopaint", "cpt");
        add("image/x-icon", "ico");
        add("image/x-jg", "art");
        add("image/x-jng", "jng");
        add("image/x-ms-bmp", "bmp");
        add("image/x-photoshop", "psd");
        add("image/x-portable-anymap", "pnm");
        add("image/x-portable-bitmap", "pbm");
        add("image/x-portable-graymap", "pgm");
        add("image/x-portable-pixmap", "ppm");
        add("image/x-rgb", "rgb");
        add("image/x-xbitmap", "xbm");
        add("image/x-xpixmap", "xpm");
        add("image/x-xwindowdump", "xwd");
        add("model/iges", "igs");
        add("model/iges", "iges");
        add("model/mesh", "msh");
        add("model/mesh", "mesh");
        add("model/mesh", "silo");
        add("text/calendar", "ics");
        add("text/calendar", "icz");
        add("text/comma-separated-values", "csv");
        add("text/css", "css");
        add("text/html", "htm");
        add("text/html", "html");
        add("text/h323", "323");
        add("text/iuls", "uls");
        add("text/mathml", "mml");
        // add ".txt" first so it will be the default for ExtensionFromMimeType
        add("text/plain", "txt");
        add("text/plain", "asc");
        add("text/plain", "text");
        add("text/plain", "diff");
        add("text/plain", "po");     // reserve "pot" for vnd.ms-powerpoint
        add("text/richtext", "rtx");
        add("text/rtf", "rtf");
        add("text/texmacs", "ts");
        add("text/text", "phps");
        add("text/tab-separated-values", "tsv");
        add("text/xml", "xml");
        add("text/x-bibtex", "bib");
        add("text/x-boo", "boo");
        add("text/x-c++hdr", "h++");
        add("text/x-c++hdr", "hpp");
        add("text/x-c++hdr", "hxx");
        add("text/x-c++hdr", "hh");
        add("text/x-c++src", "c++");
        add("text/x-c++src", "cpp");
        add("text/x-c++src", "cxx");
        add("text/x-chdr", "h");
        add("text/x-component", "htc");
        add("text/x-csh", "csh");
        add("text/x-csrc", "c");
        add("text/x-dsrc", "d");
        add("text/x-haskell", "hs");
        add("text/x-java", "java");
        add("text/x-literate-haskell", "lhs");
        add("text/x-moc", "moc");
        add("text/x-pascal", "p");
        add("text/x-pascal", "pas");
        add("text/x-pcs-gcd", "gcd");
        add("text/x-setext", "etx");
        add("text/x-tcl", "tcl");
        add("text/x-tex", "tex");
        add("text/x-tex", "ltx");
        add("text/x-tex", "sty");
        add("text/x-tex", "cls");
        add("text/x-vcalendar", "vcs");
        add("text/x-vcard", "vcf");
        add("video/3gpp", "3gpp");
        add("video/3gpp", "3gp");
        add("video/3gpp", "3g2");
        add("video/dl", "dl");
        add("video/dv", "dif");
        add("video/dv", "dv");
        add("video/fli", "fli");
        add("video/m4v", "m4v");
        add("video/mpeg", "mpeg");
        add("video/mpeg", "mpg");
        add("video/mpeg", "mpe");
        add("video/mp4", "mp4");
        add("video/mpeg", "VOB");
        add("video/quicktime", "qt");
        add("video/quicktime", "mov");
        add("video/vnd.mpegurl", "mxu");
        add("video/webm", "webm");
        add("video/x-la-asf", "lsf");
        add("video/x-la-asf", "lsx");
        add("video/x-mng", "mng");
        add("video/x-ms-asf", "asf");
        add("video/x-ms-asf", "asx");
        add("video/x-ms-wm", "wm");
        add("video/x-ms-wmv", "wmv");
        add("video/x-ms-wmx", "wmx");
        add("video/x-ms-wvx", "wvx");
        add("video/x-msvideo", "avi");
        add("video/x-sgi-movie", "movie");
        add("x-conference/x-cooltalk", "ice");
        add("x-epoc/x-sisx-app", "sisx");
    }

    private static void add(String mimeType, String extension) {
        //
        // if we have an existing x --> y mapping, we do not want to
        // override it with another mapping x --> ?
        // this is mostly because of the way the mime-type map below
        // is constructed (if a mime type maps to several extensions
        // the first extension is considered the most popular and is
        // added first; we do not want to overwrite it later).
        //
        if (!mimeTypeToExtensionMap.containsKey(mimeType)) {
            mimeTypeToExtensionMap.put(mimeType, extension);
        }
        //extensionToMimeTypeMap.put(extension, mimeType);
    }
    /**
     * Returns true if the given MIME type has an entry in the map.
     * @param mimeType A MIME type (i.e. text/plain)
     * @return True iff there is a mimeType entry in the map.
     */
    public static boolean hasMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }
        return mimeTypeToExtensionMap.containsKey(mimeType);
    }

    /**
     * Returns the registered extension for the given MIME type. Note that some
     * MIME types map to multiple extensions. This call will return the most
     * common extension for the given MIME type.
     * @param mimeType A MIME type (i.e. text/plain)
     * @return The extension for the given MIME type or null iff there is none.
     */
    public static String guessExtensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return null;
        }
        return mimeTypeToExtensionMap.get(mimeType);
    }

}
```

### [DownUtil](https://github.com/crazyStrome/MultiplyDowloader/blob/master/src/main/java/MultiplyDowloader/DownUntil.java)

在之前所有的内容都准备好之后，便进行多线程的下载了。在构造器中对文件大小和类型进行判断：

```@Java
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
```

所谓多线程下载就是把一个文件分成多个小块，每次每个线程只下载它的那一小块，之后就相当于多线程下载。使用InputStream的skip方法就可以实现获取不同位置的文件。同时还有断点续传功能，每个线程每下完一部分之后就会把当前指针的位置保存起来，如果此时线程中断，则下次下载时如果存在该文件，则从该文件记录的指针处开始写入数据。

```
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
```

## 结果
***

我下载了十几兆字节的文件，发现下载速度和chrome的下载速度差不多，没有尝试过大文件下载。