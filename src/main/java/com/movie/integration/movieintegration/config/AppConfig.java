package com.movie.integration.movieintegration.config;

import com.movie.integration.movieintegration.ftp.FileTransferHandler;
import com.movie.integration.movieintegration.imdb.DocumentHandler;
import com.movie.integration.movieintegration.local.LocalFileHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class AppConfig {

    @Value("${ftp_host_name}")
    String hostName;
    @Value("${ftp_port}")
    int portNumber;
    @Value("${ftp_account_name}")
    String ftpAccountName;
    @Value("${ftp_password}")
    String ftpPassword;

    @Bean
    public RestTemplate restTemplate(){
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(createHttpClient());
        RestTemplate restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);
        return restTemplate;
    }

    private HttpClient createHttpClient() {
        return HttpClientBuilder.create().build();
    }

    @Bean
    public HttpEntity emptyRequestHttpEntity(){
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Object> request = new HttpEntity<>(params, headers);
        return request;
    }

    @Bean
    public DocumentHandler documentHandler(){
        return new DocumentHandler();
    }

    @Bean
    FileTransferHandler fileTransferHandler(){
        return new FileTransferHandler();
    }

    @Bean
    LocalFileHandler localFileHandler() { return new LocalFileHandler(); }

    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory(){
        DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
        ftpSessionFactory.setHost(hostName);
        ftpSessionFactory.setPort(portNumber);
        ftpSessionFactory.setUsername(ftpAccountName);
        ftpSessionFactory.setPassword(ftpPassword);
        return ftpSessionFactory;
    }

    @Bean
    public Set<String> oldRepositoryFiles(){

        String repositoryFile = "tag_repository/tag_file";
        File oldRepositoryFile = new File(repositoryFile);
        BufferedReader br = null;

        Set<String> oldFiles = new HashSet<>();

        try {
            br = new BufferedReader(new FileReader(oldRepositoryFile));
            String oldFile;
            while ((oldFile = br.readLine()) != null) {
                oldFiles.add(oldFile);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return oldFiles;

    }

}
