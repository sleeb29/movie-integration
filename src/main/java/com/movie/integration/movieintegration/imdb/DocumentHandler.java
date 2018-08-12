package com.movie.integration.movieintegration.imdb;

import com.movie.integration.movieintegration.httpconverter.GZipHttpMessageConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class DocumentHandler {

    private static final Logger logger = Logger.getLogger(DocumentHandler.class.getName());

    HashMap<String, String> fileNameToETags;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    HttpEntity emptyRequestHttpEntity;

    @Autowired
    Set<String> oldRepositoryFiles;

    @Value("${imdb.file_list_uri}")
    String documentHandlerURI;
    @Value("${imdb.file_extension}")
    String fileExtension;

    public void updateLocalDocuments() throws IOException {
        HttpMethod httpMethod = HttpMethod.GET;

        ResponseEntity<String> httpResponse = this.restTemplate.exchange(documentHandlerURI, httpMethod, emptyRequestHttpEntity, String.class, new LinkedMultiValueMap<>());

        String responseString = httpResponse.getBody().toString();
        Document htmlDocument = Jsoup.parse(responseString.toString());
        Elements documentLinks = htmlDocument.select("a[href]");

        Set<String> urls = getAllURLs(documentLinks);
        Set<String> validURLs = getValidURLs(urls, documentHandlerURI);
        updateFileNameToETags(validURLs);

        createNewFiles();
    }

    private Set<String> getAllURLs(Elements documentLinks) {

        Set<String> urls = new HashSet<>();
        for(Element element : documentLinks){
            if(element.attr("href").endsWith("gz")) {
                urls.add(element.attr("href"));
            }
        }

        return urls;


    }

    private Set<String> getValidURLs(Set<String> urls, String uri) throws IOException {

        URL linkURL = new URL(uri);
        Certificate linkCert = getSSLCertificate(linkURL.getHost(), 443);

        Set<String> validURLs = new HashSet<>();
        for(String urlString : urls){
            URL url = new URL(urlString);
            String hostname = url.getHost();
            Certificate urlCert = getSSLCertificate(hostname, 443);
            if(!urlCert.equals(linkCert)){
                logger.log(Level.SEVERE, "cert for: " + url + " does not match cert for: " + uri);
            } else {
                urls.add(urlString);
            }

        }

        return urls;

    }

    private X509Certificate getSSLCertificate(String hostname, int port) throws IOException {

        SocketFactory factory = SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(hostname, port);

        socket.startHandshake();

        Certificate[] certs = socket.getSession().getPeerCertificates();

        for (Certificate cert : certs) {
            if(cert instanceof X509Certificate) {
                try {
                    ( (X509Certificate) cert).checkValidity();
                    return (X509Certificate)cert;
                } catch(CertificateExpiredException cee) {

                } catch (CertificateNotYetValidException e) {
                    e.printStackTrace();
                }
            }

        }

        return null;

    }


    private void updateFileNameToETags(Set<String> urls){

        this.fileNameToETags = new HashMap<>();

        for(String url : urls){

            HttpMethod httpMethod = HttpMethod.HEAD;
            ResponseEntity<HashMap> httpResponse = this.restTemplate.exchange(url, httpMethod, emptyRequestHttpEntity, HashMap.class, new LinkedMultiValueMap<>());
            String ETag = httpResponse.getHeaders().getETag().replace("\"","");
            fileNameToETags.put(url, ETag);

        }

    }

    private void createNewFiles(){

        List<HttpMessageConverter<?>> httpMessageConverters = restTemplate.getMessageConverters();
        httpMessageConverters.add(new GZipHttpMessageConverter());
        this.restTemplate.setMessageConverters(httpMessageConverters);

        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        HttpMethod httpMethod = HttpMethod.GET;

        fileNameToETags.entrySet().parallelStream()
                .filter(entry -> {
                    String fileName = entry.getKey()
                            .split("/")[entry.getKey().split("/").length - 1]
                            .replace("." + fileExtension, "");
                    String ETag = entry.getValue();

                    Boolean fileExists = oldRepositoryFiles.contains(fileName + "_" + ETag);
                    logger.info("file exists? " + fileName + ": " + fileExists);
                    return !fileExists;

                })
                .forEach((Map.Entry<String, String> entry) -> {
                    String fileName = entry.getKey()
                            .split("/")[entry.getKey().split("/").length - 1]
                            .replace("." + fileExtension, "");

                    ResponseEntity<File> entryResponse = restTemplate.exchange(entry.getKey(), httpMethod, emptyRequestHttpEntity, File.class, params);
                    File file = entryResponse.getBody();

                    String currentFileName = file.getName();
                    String newFileName = "imdb/" + fileName + "_" + currentFileName;

                    unzipNewFile(file, newFileName);

                });
    }

    private void unzipNewFile(File file, String fileName){

        byte[] buffer = new byte[1024];

        try{

            GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file));
            FileOutputStream out = new FileOutputStream(fileName.replace(".gz",""));

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();

        }catch(IOException ex){
            ex.printStackTrace();
        }

    }

}
