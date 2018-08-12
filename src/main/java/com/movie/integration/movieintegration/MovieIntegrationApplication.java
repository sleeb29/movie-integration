package com.movie.integration.movieintegration;

import com.movie.integration.movieintegration.imdb.DocumentHandler;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Component;

import java.io.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
@ComponentScan({
"com.movie.integration.movieintegration",
"com.movie.integration.movieintegration.config"})
@Component
public class MovieIntegrationApplication {

    private static final Logger logger = Logger.getLogger(MovieIntegrationApplication.class.getName());

    public static void main(String[] args) throws IOException {
        ApplicationContext context
                = new AnnotationConfigApplicationContext(MovieIntegrationApplication.class);

        DocumentHandler documentHandler = (DocumentHandler)context.getBean("documentHandler");
        documentHandler.updateDocumentsLocally();

        DefaultFtpSessionFactory ftpSessionFactory = (DefaultFtpSessionFactory)context.getBean("ftpSessionFactory");
        FtpSession ftpSession = ftpSessionFactory.getSession();

        String fileRegex = "^(title[.][a-zA-Z]+[.]tsv_).+$";
        String filePathRegex = "(title[.][a-zA-Z]+[.]tsv_)";
        Pattern pattern = Pattern.compile(fileRegex);
        Pattern pattern2 = Pattern.compile(filePathRegex);

        File dir = new File(".");
        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });

        HashSet<String> ftpFiles = new HashSet<>(Arrays.asList(ftpSession.listNames(".")));
        List<String> addedFiles = new ArrayList<>();

        for(int i = 0; i < files.length; i++){
            File file = files[i];

            if(ftpFiles.contains(file.getName())){
                continue;
            }

            Matcher m = pattern2.matcher(file.getName());
            Boolean found = m.find();
            if(found){
                String matchingString = m.group(1);
                addedFiles.add(matchingString);
            }
            ftpSession.write(new FileInputStream(file), file.getPath());
        }

        for(String ftpFile : ftpFiles){
            for(String addedFile : addedFiles){
                Pattern ftpPattern = Pattern.compile(".*" + addedFile + ".*");
                Boolean matches = ftpPattern.matcher(ftpFile).matches();
                logger.info("addedFile: " + addedFile + "-" + "ftpFile: " + ftpFile + " matches=" + matches);
                if(matches){
                    ftpSession.getClientInstance().deleteFile(ftpFile);
                    break;
                }
            }
        }

    }

}
