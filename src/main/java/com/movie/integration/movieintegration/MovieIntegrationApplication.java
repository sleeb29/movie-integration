package com.movie.integration.movieintegration;

import com.movie.integration.movieintegration.ftp.FileTransferHandler;
import com.movie.integration.movieintegration.imdb.DocumentHandler;

import com.movie.integration.movieintegration.local.LocalFileHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.*;

import java.util.*;
import java.util.logging.Logger;

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

        HashMap<String, String> commandLineSwitches = (HashMap<String, String>)context.getBean("commandLineSwitches");
        Map<String, Boolean> routineToRunRoutineMap = parseArgumentsToMap(commandLineSwitches.values(), args);

        String documentHandlerSwitch = commandLineSwitches.get("documentHandler");
        if(!routineToRunRoutineMap.containsKey(documentHandlerSwitch) || routineToRunRoutineMap.get(documentHandlerSwitch)){
            DocumentHandler documentHandler = (DocumentHandler)context.getBean("documentHandler");
            documentHandler.updateLocalDocuments();
        }

        String fileTransferHandlerSwitch = commandLineSwitches.get("fileTransferHandler");
        if(!routineToRunRoutineMap.containsKey(fileTransferHandlerSwitch) || routineToRunRoutineMap.get(fileTransferHandlerSwitch)){
            FileTransferHandler fileTransferHandler = (FileTransferHandler)context.getBean("fileTransferHandler");
            fileTransferHandler.updateRemoteDocuments();
        }

        String localFileHandlerSwitch = commandLineSwitches.get("localFileHandler");
        if(!routineToRunRoutineMap.containsKey(localFileHandlerSwitch) || routineToRunRoutineMap.get(localFileHandlerSwitch)){
            LocalFileHandler localFileHandler = (LocalFileHandler)context.getBean("localFileHandler");
            localFileHandler.cleanUpLocalFiles();
        }

    }

    private static Map<String, Boolean> parseArgumentsToMap(Collection<String> commandLineSwitches, String[] args){
        Map<String, Boolean> routineToRunRoutineMap = new HashMap<>();

        for(int i = 0; i < args.length; i++){

            String arg = args[i];

            if(commandLineSwitches.contains(arg)){
                Boolean argValue = Boolean.parseBoolean(args[i + 1]);
                routineToRunRoutineMap.put(arg, argValue);
            }

        }

        return routineToRunRoutineMap;

    }

}
