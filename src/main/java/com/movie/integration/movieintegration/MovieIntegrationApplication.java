package com.movie.integration.movieintegration;

import com.movie.integration.movieintegration.ftp.FileTransferHandler;
import com.movie.integration.movieintegration.imdb.DocumentHandler;

import com.movie.integration.movieintegration.local.LocalFileHandler;
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
        documentHandler.updateLocalDocuments();

        FileTransferHandler fileTransferHandler = (FileTransferHandler)context.getBean("fileTransferHandler");
        fileTransferHandler.updateRemoteDocuments();

        LocalFileHandler localFileHandler = (LocalFileHandler)context.getBean("localFileHandler");
        localFileHandler.cleanUpLocalFiles();

    }

}
